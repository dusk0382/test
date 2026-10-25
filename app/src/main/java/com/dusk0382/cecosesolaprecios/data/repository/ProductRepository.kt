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
import com.dusk0382.cecosesolaprecios.data.remote.dto.PreciosRepoDto
import com.dusk0382.cecosesolaprecios.data.sync.EnrichSyncWorker
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
        productDao.searchFlow(
            escaparLike(consultaDeBusqueda(query)),
            barcodeDeConsulta(query),
            clases.joinToString(","),
            orden,
        )

    /**
     * Conteo por clase para la consulta actual — el número que se muestra junto a
     * cada opción del filtro tiene que ser el de los resultados que el usuario
     * está viendo, no el del catálogo entero.
     */
    fun conteoPorClaseFlow(query: String): Flow<List<ClaseConteo>> =
        productDao.conteoPorClaseFlow(
            escaparLike(consultaDeBusqueda(query)),
            barcodeDeConsulta(query),
        )

    fun byIdFlow(id: Long): Flow<ProductEntity?> = productDao.byIdFlow(id)
    fun countFlow(): Flow<Int> = productDao.countFlow()
    suspend fun byBarcode(barcode: String): ProductEntity? =
        withContext(Dispatchers.IO) { productDao.byBarcode(barcode) }

    suspend fun ultimaFechaRepo(): String? = metaDao.get(MetaKeys.REPO_DATE)
    suspend fun ultimaFechaApi(): String? = metaDao.get(MetaKeys.API_SYNC_AT)

    /** Instante (epoch ms) del último enrich exitoso, reactivo a los cambios de meta. */
    fun apiSyncAtFlow(): Flow<Long?> = metaDao.valueFlow(MetaKeys.API_SYNC_AT)
        .map { it?.toLongOrNull() }
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

    /** Unidades totales del carrito (suma de `quantity`, no de líneas). */
    fun cartUnitsCountFlow(): Flow<Int> = db.cartDao().unitsCountFlow()

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
     * Borra el enriquecimiento de todas las filas y vuelve a encolarlo. Sirve
     * para reparar una base instalada vieja con `apiId`/CEC mal asignados sin
     * tocar filas, favoritos ni carrito (los productos base y sus precios en Bs
     * quedan intactos).
     *
     * Se invalida también `API_VERSION`: `syncEnrich` salta la reescritura de
     * productos cuando la versión no cambió, así que sin esto el enrich que se
     * encola acá volvería en segundos sin reconstruir nada.
     */
    suspend fun resetEnrichment() = withContext(Dispatchers.IO) {
        // clearEnrichment y la invalidación de la versión van juntas: si el
        // proceso muere entre una y otra, la base queda sin CEC y con la versión
        // vieja, y el próximo enrich se salta la reconstrucción.
        db.withTransaction {
            db.productDao().clearEnrichment()
            metaDao.put(MetaEntity(MetaKeys.API_VERSION, ""))
            // La fecha mostrada en Ajustes también deja de mentir: hasta que
            // termine el worker no hay precio solidario válido.
            metaDao.put(MetaEntity(MetaKeys.API_SYNC_AT, ""))
        }
        requestEnrich()
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
        val esquemaGuardado = metaDao.get(MetaKeys.MIRROR_ID_SCHEME).orEmpty()
        val esquemaActual = dto.idScheme.orEmpty()
        // Un cambio de esquema (ids posicionales -> _id de la API) o el primer
        // arranque con esta versión exige reconstruir el mirror aunque la fecha
        // no haya cambiado. Es la reparación automática de bases con filas
        // quimera; el usuario no tiene que borrar datos.
        val necesitaReconstruir = metaDao.get(MetaKeys.CATALOG_REBUILD) != REBUILD_VERSION ||
            esquemaActual != esquemaGuardado
        if (fechaGuardada == dto.fechaActualizacion && !necesitaReconstruir) return@withContext false

        val filas = db.withTransaction {
            val existentes = productDao.all()
            val nuevas = if (necesitaReconstruir) {
                reconstruirCatalogo(dto, existentes)
            } else {
                val incremental = MergeEngine.filasDesdeRepo(dto, existentes)
                productDao.upsertAll(incremental)
                incremental
            }
            metaDao.put(MetaEntity(MetaKeys.REPO_DATE, dto.fechaActualizacion))
            metaDao.put(MetaEntity(MetaKeys.REPO_SYNC_AT, System.currentTimeMillis().toString()))
            metaDao.put(MetaEntity(MetaKeys.MIRROR_ID_SCHEME, esquemaActual))
            metaDao.put(MetaEntity(MetaKeys.CATALOG_REBUILD, REBUILD_VERSION))
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
        Log.i(
            TAG,
            "syncBase: ${filas.size} filas desde el mirror (fecha ${dto.fechaActualizacion})" +
                if (necesitaReconstruir) " [reconstrucción]" else "",
        )

        // El mirror cambia ~3 veces/día pero el enrich programado es diario: sin
        // esto el CEC podía quedar 16 h atrás de la lista base (capturas: base
        // 19:45 con enriquecido de las 03:05). El umbral de 6 h evita encolar en
        // cada refresh manual cuando hace poco que se enriqueció. Encolar no
        // puede tumbar el sync base: si WorkManager falla, la base ya está bien.
        // Si reconstruimos, el enrich es obligatorio: las filas nuevas no tienen CEC.
        if (necesitaReconstruir || enrichViejo(ENRICH_MAX_AGE_MS)) {
            runCatching { requestEnrich() }
                .onFailure { Log.w(TAG, "syncBase: no se pudo encolar el enrich: ${it.message}", it) }
        }
        true
    }

    /**
     * Reconstruye solo las filas del mirror. Las filas solo-API se conservan y
     * pueden reutilizarse por nombre, de modo que sus localId no cambian. Los
     * favoritos y el carrito de las filas viejas se reasignan a la fila nueva
     * con el mismo nombre normalizado (y precio más cercano si el nombre se
     * repite). Es la reparación única de bases que mezclaron productos porque
     * el mirror reutilizaba ids posicionales.
     */
    private suspend fun reconstruirCatalogo(
        dto: PreciosRepoDto,
        existentes: List<ProductEntity>,
    ): List<ProductEntity> {
        val viejosMirror = existentes.filter { it.repoId != null }
        val apiOnly = existentes.filter { it.repoId == null }
        val idsViejos = viejosMirror.map { it.localId }.toSet()
        val porIdViejo = viejosMirror.associateBy { it.localId }
        val favoritos = if (idsViejos.isEmpty()) {
            emptyList()
        } else {
            db.favoriteDao().all().filter { it.productId in idsViejos }
        }
        val carrito = if (idsViejos.isEmpty()) {
            emptyList()
        } else {
            db.cartDao().all().filter { it.productId in idsViejos }
        }

        if (idsViejos.isNotEmpty()) productDao.deleteByIds(idsViejos.toList())

        // Las filas solo-API se conservan; si el mirror trae el mismo nombre,
        // filasDesdeRepo las reutiliza y no se pierden localId ni favoritos.
        val filas = MergeEngine.filasDesdeRepo(dto, apiOnly)
        productDao.upsertAll(filas)

        val despues = productDao.all()
        val porNombre = despues.groupBy { it.nombreNormalizado }
        fun destino(viejo: ProductEntity): ProductEntity? {
            val candidatos = porNombre[viejo.nombreNormalizado].orEmpty()
            return candidatos.minByOrNull { kotlin.math.abs(it.precioBs - viejo.precioBs) }
        }

        val carritoDestinos = db.cartDao().all().map { it.productId }.toMutableSet()
        favoritos.forEach { favorito ->
            val viejo = porIdViejo[favorito.productId] ?: return@forEach
            val nuevo = destino(viejo)
            if (nuevo == null) {
                db.favoriteDao().remove(favorito.productId)
                return@forEach
            }
            if (nuevo.localId != favorito.productId) {
                db.favoriteDao().add(FavoriteEntity(nuevo.localId))
                db.favoriteDao().remove(favorito.productId)
            }
        }
        carrito.forEach { item ->
            val viejo = porIdViejo[item.productId] ?: return@forEach
            val nuevo = destino(viejo)
            if (nuevo == null) {
                db.cartDao().remove(item.productId)
                return@forEach
            }
            if (nuevo.localId != item.productId) {
                if (nuevo.localId !in carritoDestinos) {
                    db.cartDao().upsert(CartItemEntity(nuevo.localId, item.quantity, item.addedAt))
                    carritoDestinos += nuevo.localId
                }
                db.cartDao().remove(item.productId)
            }
        }
        return filas
    }

    /** true si nunca hubo un enrich exitoso o si pasó más de [maxAgeMillis]. */
    private suspend fun enrichViejo(maxAgeMillis: Long): Boolean {
        val ultimo = metaDao.get(MetaKeys.API_SYNC_AT)?.toLongOrNull() ?: return true
        return System.currentTimeMillis() - ultimo > maxAgeMillis
    }

    /**
     * Enriquece con la API oficial (lento; fallar es normal). True si hubo
     * cambios de productos o de meta (tasa, ferias, versión).
     *
     * El payload se parsea SIEMPRE para sacar versión, tasa y ferias: el guard de
     * versión solo evita reescribir las ~527 filas cuando el catálogo no cambió.
     * Antes retornaba antes de tocar meta, así que un cambio de tasa sin cambio
     * de versión no se aplicaba nunca y el toggle USD quedaba con precios
     * solidarios viejos. `API_SYNC_AT` se sella en toda descarga exitosa (última
     * verificación real) y no cuenta como "cambio" para el valor de retorno.
     *
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
        val tasa = MergeEngine.tasaVed(payload)
        val tasaGuardada = metaDao.get(MetaKeys.API_RATE_VED)?.toDoubleOrNull()
        // Si el payload no trae la tasa, la última conocida sigue siendo mejor
        // que 0: las filas solo-API derivan su precio en Bs de ella.
        val tasaAplicar = tasa ?: tasaGuardada
        val ferias = MergeEngine.nombresBranches(payload, json)
        val feriasJson = json.encodeToString(ferias)
        val hayBranches = payload.branches != null

        // Valores previos (antes de escribir): el retorno distingue "actualicé
        // algo" de "la misma foto otra vez".
        val versionPrevia = metaDao.get(MetaKeys.API_VERSION)
        val tasaPrevia = metaDao.get(MetaKeys.API_RATE_VED)
        val feriasPrevias = metaDao.get(MetaKeys.BRANCHES_JSON)

        // Misma versión: el catálogo no cambió y no se reescriben las filas, pero
        // tasa, ferias y sello de contacto se guardan igual (ver KDoc).
        val mismaVersion = version != null && version == versionGuardada
        val aAplicar = if (mismaVersion) emptyList() else MergeEngine.enriquecidosDesdePayload(payload)

        db.withTransaction {
            if (aAplicar.isNotEmpty()) {
                val filas = MergeEngine.filasConEnriquecimiento(aAplicar, productDao.all(), tasaAplicar)
                productDao.upsertAll(filas)
            }
            if (version != null) metaDao.put(MetaEntity(MetaKeys.API_VERSION, version.toString()))
            if (tasa != null) {
                metaDao.put(MetaEntity(MetaKeys.API_RATE_VED, tasa.toString()))
                // Una tasa nueva obliga a recalcular el Bs de las filas solo-API
                // aunque la versión del catálogo no haya cambiado.
                if (tasa.toString() != tasaPrevia) productDao.updateApiOnlyBs(tasa)
            }
            // Si el payload no trae branches, no se pisan las ferias conocidas.
            if (hayBranches) metaDao.put(MetaEntity(MetaKeys.BRANCHES_JSON, feriasJson))
            metaDao.put(MetaEntity(MetaKeys.API_SYNC_AT, System.currentTimeMillis().toString()))
        }

        val huboCambios = aAplicar.isNotEmpty() ||
            (version != null && version.toString() != versionPrevia) ||
            (tasa != null && tasa.toString() != tasaPrevia) ||
            (hayBranches && feriasJson != feriasPrevias)

        Log.i(
            TAG,
            if (aAplicar.isEmpty()) {
                "syncEnrich: sin cambios de catálogo (version $version); " +
                    "tasa ${tasaAplicar ?: "sin dato"}, ${ferias.size} ferias"
            } else {
                "syncEnrich: ${aAplicar.size} productos enriquecidos, version $version, " +
                    "Bs por USD ${tasaAplicar ?: "sin tasa (los precios en Bs de filas solo-API quedan en 0)"}"
            },
        )
        huboCambios
    }

    private companion object {
        const val TAG = "CecoSync"

        /** Versión del algoritmo de reconstrucción de la base instalada. */
        const val REBUILD_VERSION = "api-ids-v1"

        /** Mismo ritmo que el mirror: 6 h. Evita encolar en cada refresh manual
         *  cuando hace poco que se enriqueció. */
        const val ENRICH_MAX_AGE_MS = 6L * 60 * 60 * 1000
    }
}
