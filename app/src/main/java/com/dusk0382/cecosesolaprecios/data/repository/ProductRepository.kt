package com.dusk0382.cecosesolaprecios.data.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dusk0382.cecosesolaprecios.data.local.AppDatabase
import com.dusk0382.cecosesolaprecios.data.local.CartLine
import com.dusk0382.cecosesolaprecios.data.local.CartItemEntity
import com.dusk0382.cecosesolaprecios.data.local.MetaEntity
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

    fun searchFlow(query: String, categoria: String?, orden: String): Flow<List<ProductEntity>> =
        productDao.searchFlow(normalizarNombre(query), categoria, orden)

    fun byIdFlow(id: Long): Flow<ProductEntity?> = productDao.byIdFlow(id)
    fun categoriasFlow(): Flow<List<String>> = productDao.categoriasFlow()
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
        if (dao.isFavorite(id)) dao.remove(id) else dao.add(id)
    }

    // — carrito —

    fun cartFlow(): Flow<List<CartLine>> = db.cartDao().itemsWithProductFlow()

    fun cartQuantityFlow(id: Long): Flow<Int?> = db.cartDao().quantityOfFlow(id)

    fun cartCountFlow(): Flow<Int> = db.cartDao().countFlow()

    suspend fun setQuantity(productId: Long, quantity: Int) = withContext(Dispatchers.IO) {
        if (quantity <= 0) db.cartDao().remove(productId)
        else db.cartDao().upsert(CartItemEntity(productId, quantity))
    }

    suspend fun clearCart() = withContext(Dispatchers.IO) { db.cartDao().clear() }

    // — sync —

    /** Encola el enriquecimiento como worker one-time: la API oficial tarda
     *  7–40s y no debe atarse al ciclo de vida del ViewModel. */
    fun requestEnrich() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_enrich_manual",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<EnrichSyncWorker>().build(),
        )
    }

    /** Sincroniza precios.json (rápido). True si hubo cambios. */
    suspend fun syncBase(): Boolean = withContext(Dispatchers.IO) {
        val dto = repoApi.getPrecios()
        val fechaGuardada = metaDao.get(MetaKeys.REPO_DATE)
        if (fechaGuardada == dto.fechaActualizacion) return@withContext false

        val existentes = productDao.all()
        val porRepoId = existentes.filter { it.repoId != null }.associateBy { it.repoId!! }
        val porNombre = existentes.associateBy { it.nombreNormalizado }
        val filas = MergeEngine.filasDesdeRepo(dto, porRepoId, porNombre)
        productDao.upsertAll(filas)
        metaDao.put(MetaEntity(MetaKeys.REPO_DATE, dto.fechaActualizacion))
        metaDao.put(MetaEntity(MetaKeys.REPO_SYNC_AT, System.currentTimeMillis().toString()))
        true
    }

    /** Enriquece con la API oficial (lento; fallar es normal). True si hubo cambios. */
    suspend fun syncEnrich(): Boolean = withContext(Dispatchers.IO) {
        val raw = officialApi.downloadFile() ?: return@withContext false
        val payload = runCatching {
            json.decodeFromString(PayloadOficial.serializer(), raw)
        }.getOrNull() ?: return@withContext false

        val version = payload.priceList?.model?.version
        val versionGuardada = metaDao.get(MetaKeys.API_VERSION)?.toIntOrNull()
        if (version != null && version == versionGuardada) return@withContext false

        val tasa = MergeEngine.tasaVed(payload)
        val enriquecidos = MergeEngine.enriquecidosDesdePayload(payload)
        if (enriquecidos.isEmpty()) return@withContext false

        val filas = MergeEngine.filasConEnriquecimiento(enriquecidos, productDao.all(), tasa)
        productDao.upsertAll(filas)

        if (version != null) metaDao.put(MetaEntity(MetaKeys.API_VERSION, version.toString()))
        metaDao.put(MetaEntity(MetaKeys.API_SYNC_AT, System.currentTimeMillis().toString()))
        if (tasa != null) metaDao.put(MetaEntity(MetaKeys.API_RATE_VED, tasa.toString()))
        metaDao.put(MetaEntity(MetaKeys.BRANCHES_JSON, json.encodeToString(MergeEngine.nombresBranches(payload, json))))
        true
    }
}
