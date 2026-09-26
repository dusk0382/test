package com.dusk0382.cecosesolaprecios.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dusk0382.cecosesolaprecios.data.local.AppDatabase
import com.dusk0382.cecosesolaprecios.data.local.CartLine
import com.dusk0382.cecosesolaprecios.data.local.ClaseConteo
import com.dusk0382.cecosesolaprecios.data.local.CartItemEntity
import com.dusk0382.cecosesolaprecios.data.local.MetaEntity
import com.dusk0382.cecosesolaprecios.data.local.FavoriteEntity
import com.dusk0382.cecosesolaprecios.data.local.MetaKeys
import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.data.remote.HttpClients
import com.dusk0382.cecosesolaprecios.data.remote.OfficialApi
import com.dusk0382.cecosesolaprecios.data.remote.RepoApi
import com.dusk0382.cecosesolaprecios.data.remote.dto.PayloadOficial
import com.dusk0382.cecosesolaprecios.data.sync.EnrichSyncWorker
import com.dusk0382.cecosesolaprecios.domain.normalizarNombre
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class ProductRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val http: HttpClients,
    private val repoApi: RepoApi,
    private val officialApi: OfficialApi,
) {
    private val json: Json get() = http.jsonFormat
    private val productDao get() = db.productDao()
    private val metaDao get() = db.metaDao()

    // — lecturas (Room-first; la UI nunca toca la red) —

    /**
     * Búsqueda + filtro por clases (multi-selección OR entre clases, AND con la
     * búsqueda). Sin tope de clases: la UI deja elegir varias y el filtro tiene
     * que aplicar todas las que haya.
     */
    fun searchFlow(query: String, clases: List<String>, orden: String): Flow<List<ProductEntity>> =
        productDao.searchFlow(escaparLike(consultaDeBusqueda(query)), clases.joinToString(","), orden)

    /**
     * Conteo por clase para la consulta actual — el número que se muestra junto a
     * cada opción del filtro tiene que ser el de los resultados que el usuario
     * está viendo, no el del catálogo entero.
     */
    fun conteoPorClaseFlow(query: String): Flow<List<ClaseConteo>> =
        productDao.conteoPorClaseFlow(escaparLike(consultaDeBusqueda(query)))

    fun byIdFlow(id: Long): Flow<ProductEntity?> = productDao.byIdFlow(id)
    fun countFlow(): Flow<Int> = productDao.countFlow()
    suspend fun byBarcode(barcode: String): ProductEntity? =
        withContext(Dispatchers.IO) { productDao.byBarcode(barcode) }

    suspend fun ultimaFechaRepo(): String? = metaDao.get(MetaKeys.REPO_DATE)
    suspend fun ultimaFechaApi(): String? = metaDao.get(MetaKeys.API_SYNC_AT)
    suspend fun tasaOficial(): Double? = metaDao.get(MetaKeys.API_RATE_VED)?.toDoubleOrNull()
    suspend fun ferias(): List<String> = metaDao.get(MetaKeys.BRANCHES_JSON)
        ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() } ?: emptyList()

    // — favoritos —

    fun favoriteIdsFlow(): Flow<List<Long>> = db.favoriteDao().idsFlow()
    fun favoritosFlow(): Flow<List<ProductEntity>> = db.favoriteDao().favoritosConProductosFlow()
    fun isFavoriteFlow(id: Long): Flow<Boolean> = db.favoriteDao().isFavoriteFlow(id)

    suspend fun toggleFavorite(id: Long) = withContext(Dispatchers.IO) {
        val dao = db.favoriteDao()
        if (dao.isFavorite(id)) dao.remove(id) else dao.add(FavoriteEntity(id))
    }

    // — carrito —

    fun cartFlow(): Flow<List<CartLine>> = db.cartDao().itemsWithProductFlow()

    fun cartQuantityFlow(id: Long): Flow<Int?> = db.cartDao().quantityOfFlow(id)

    fun cartCountFlow(): Flow<Int> = db.cartDao().countFlow()

    /** Mapa productId → cantidad para pintar steppers en tarjetas sin query por ítem. */
    fun cartQuantitiesFlow(): Flow<Map<Long, Int>> = db.cartDao().quantitiesFlow()
        .map { filas -> filas.associate { it.productId to it.quantity } }

    suspend fun setQuantity(productId: Long, quantity: Int) = withContext(Dispatchers.IO) {
        if (quantity <= 0) db.cartDao().remove(productId)
        else db.cartDao().upsert(CartItemEntity(productId, quantity))
    }

    suspend fun clearCart() = withContext(Dispatchers.IO) { db.cartDao().clear() }

    // — sync —

    /** Encola el enriquecimiento como worker one-time: la API oficial tarda
     *  7–40s y no debe atarse al ciclo de vida del ViewModel.
     *
     *  `REPLACE` y no `KEEP`: esto lo pide una persona tocando un botón, y con
     *  `KEEP` la pulsación se descartaba en silencio si ya había un enrich
     *  encolado, mientras la UI le decía al usuario "enriqueciendo en segundo
     *  plano…". El nombre es único, así que no se apilan enrichimientos. */
    fun requestEnrich() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_enrich_manual",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<EnrichSyncWorker>().build(),
        )
    }

    /**
     * Sincroniza precios.json (rápido). True si hubo cambios.
     *
     * El read-modify-write va entero en una transacción: sin ella, `sync_base` y
     * `sync_enrich` (que corren con nombres de work distintos, así que `KEEP` no
     * los serializa) se pisan. El peor caso medido: un `sync_base` que leyó el
     * snapshot antes de que el enrich pusiera un `apiId` devuelve esa fila con
     * `apiId = null` y `fuente = "repo"`, revirtiendo el enriquecimiento ya
     * aplicado.
     */
    suspend fun syncBase(): Boolean = withContext(Dispatchers.IO) {
        val dto = repoApi.getPrecios()
        val fechaGuardada = metaDao.get(MetaKeys.REPO_DATE)
        if (fechaGuardada == dto.fechaActualizacion) return@withContext false

        val filas = db.withTransaction {
            val existentes = productDao.all()
            val nuevas = MergeEngine.filasDesdeRepo(dto, existentes)
            productDao.upsertAll(nuevas)
            metaDao.put(MetaEntity(MetaKeys.REPO_DATE, dto.fechaActualizacion))
            metaDao.put(MetaEntity(MetaKeys.REPO_SYNC_AT, System.currentTimeMillis().toString()))
            // El mirror declara cuántos productos trae. Si no coincide con lo que
            // queda en la base, algo se perdió en el camino: antes se parseaba este
            // campo y nunca se cruzaba, así que la pérdida pasaba inadvertida.
            if (dto.totalProductos > 0) {
                val enBase = productDao.count()
                if (enBase < dto.totalProductos) {
                    Log.w(
                        TAG,
                        "syncBase: el mirror trae ${dto.totalProductos} productos pero " +
                            "quedan $enBase filas. Puede faltar un producto nuevo " +
                            "(id que colisiona por nombre) o haber enriching incompleto.",
                    )
                }
            }
            nuevas
        }
        Log.i(TAG, "syncBase: ${filas.size} filas desde el mirror (fecha ${dto.fechaActualizacion})")
        true
    }

    /**
     * Enriquece con la API oficial (lento; fallar es normal). True si hubo cambios.
     *
     * `false` significa "no había nada que hacer" (misma versión, payload vacío).
     * Un fallo de red o de parseo lanza y lo distingue: el worker reintenta solo
     * eso, en vez de reportar éxito ante un error de schema.
     */
    suspend fun syncEnrich(): Boolean = withContext(Dispatchers.IO) {
        val raw = officialApi.downloadFile()
        val payload = runCatching {
            json.decodeFromString(PayloadOficial.serializer(), raw)
        }.getOrElse {
            // Se distingue del "no había cambios": un cambio de schema no se
            // arregla reintentando, pero sí debe verse en el log.
            error("el payload oficial no parsea: ${it.message}")
        }

        val version = payload.priceList?.model?.version
        val versionGuardada = metaDao.get(MetaKeys.API_VERSION)?.toIntOrNull()
        if (version != null && version == versionGuardada) return@withContext false

        val tasa = MergeEngine.tasaVed(payload)
        val enriquecidos = MergeEngine.enriquecidosDesdePayload(payload)
        if (enriquecidos.isEmpty()) return@withContext false

        db.withTransaction {
            val filas = MergeEngine.filasConEnriquecimiento(enriquecidos, productDao.all(), tasa)
            productDao.upsertAll(filas)
            if (version != null) metaDao.put(MetaEntity(MetaKeys.API_VERSION, version.toString()))
            metaDao.put(MetaEntity(MetaKeys.API_SYNC_AT, System.currentTimeMillis().toString()))
            if (tasa != null) metaDao.put(MetaEntity(MetaKeys.API_RATE_VED, tasa.toString()))
            metaDao.put(
                MetaEntity(MetaKeys.BRANCHES_JSON, json.encodeToString(MergeEngine.nombresBranches(payload, json)))
            )
            filas
        }
        Log.i(
            TAG,
            "syncEnrich: ${enriquecidos.size} productos enriquecidos, version $version, " +
                "tasa VED/CEC ${tasa ?: "sin tasa (los precios en Bs de filas solo-API quedan en 0)"}",
        )
        true
    }

    private companion object {
        const val TAG = "CecoSync"

        /**
         * Consulta de búsqueda ya normalizada (minúsculas, sin acentos), que es
         * como está la columna `nombreNormalizado`.
         */
        fun consultaDeBusqueda(query: String): String = normalizarNombre(query)

        /**
         * Escapa los comodines de `LIKE` antes de mandarlos a la consulta. Sin
         * esto, escribir `%` en el buscador traía los 518 productos y `_`
         * emparejaba cualquier carácter: un `%` es un carácter perfectly normal
         * en un nombre de producto ("QUESO 100%") y el usuario lo escribe sin
         * saber que está disparando un comodín.
         */
        fun escaparLike(texto: String): String = texto
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }
}
