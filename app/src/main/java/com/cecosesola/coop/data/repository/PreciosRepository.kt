package com.cecosesola.coop.data.repository

import android.util.Log
import com.cecosesola.coop.data.local.*
import com.cecosesola.coop.data.remote.GitHubApiService
import com.cecosesola.coop.domain.model.Producto
import com.cecosesola.coop.domain.model.toDomain
import com.cecosesola.coop.domain.model.toEntity
import com.cecosesola.coop.presentation.utils.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

class PreciosRepository(
    private val preciosDao: PreciosDao,
    private val metadataDao: MetadataDao,
    private val favoritosDao: FavoritosDao,
    private val busquedasDao: BusquedasDao,
    private val apiService: GitHubApiService,
    private val networkMonitor: NetworkMonitor
) {
    /**
     * Flow de productos: el mapeo toDomain() ocurre en IO para no bloquear el hilo
     * principal. conflate() descarta emisiones intermedias si el colector va lento
     * (ej. durante un replaceAll rápido), evitando el flash de lista vacía.
     */
    fun getProductosFlow(): Flow<List<Producto>> =
        preciosDao.getAllProductosFlow()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
            .conflate()

    suspend fun getUltimaSincronizacion(): Long? =
        withContext(Dispatchers.IO) { metadataDao.getUltimaSync() }

    /**
     * Flow de IDs de favoritos mapeado a Set para O(1) en las búsquedas
     * de isFavorito(). El ViewModel lo convierte a StateFlow<Set<String>>.
     */
    fun getFavoritosFlow(): Flow<Set<String>> =
        favoritosDao.getAllFlow()
            .map { entities -> entities.mapTo(HashSet()) { it.productoId } }
            .flowOn(Dispatchers.IO)

    suspend fun toggleFavorito(productoId: String) = withContext(Dispatchers.IO) {
        if (favoritosDao.isFavorito(productoId)) {
            favoritosDao.deleteById(productoId)
        } else {
            favoritosDao.insert(FavoritoEntity(productoId))
        }
    }

    fun getBusquedasRecientesFlow(): Flow<List<String>> =
        busquedasDao.getRecientesFlow()
            .map { entities -> entities.map { it.query }.distinct() }
            .flowOn(Dispatchers.IO)

    suspend fun guardarBusqueda(query: String) = withContext(Dispatchers.IO) {
        if (query.isNotBlank()) busquedasDao.insert(BusquedaEntity(query = query))
    }

    suspend fun eliminarBusqueda(query: String) = withContext(Dispatchers.IO) {
        busquedasDao.deleteByQuery(query)
    }

    suspend fun refrescarSiEsNecesario(): Result<Unit> = withContext(Dispatchers.IO) {
        Log.d("Cecosesola", "🔵 REPO: refrescar llamado")

        if (!networkMonitor.isConnected) {
            Log.d("Cecosesola", "🔴 Sin conexión")
            return@withContext Result.failure(IOException("Sin conexión"))
        }

        try {
            Log.d("Cecosesola", "🔵 Llamando API...")
            val response = apiService.getPrecios()
            Log.d("Cecosesola", "🔵 API: ${response.productos.size} productos")

            // replaceAll DEBE estar anotado con @Transaction en el DAO.
            // Así Room no emite el Flow con lista vacía entre DELETE e INSERT.
            val entities = response.productos.map { it.toDomain().toEntity() }
            preciosDao.replaceAll(entities)
            metadataDao.setUltimaSync(
                MetadataEntity(ultimaSincronizacion = System.currentTimeMillis())
            )
            Log.d("Cecosesola", "🔵 Guardado en Room")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Cecosesola", "🔴 ERROR: ${e.message}", e)
            Result.failure(e)
        }
    }
}
