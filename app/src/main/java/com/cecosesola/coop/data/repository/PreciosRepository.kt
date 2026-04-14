package com.cecosesola.coop.data.repository

import android.util.Log
import com.cecosesola.coop.data.local.*
import com.cecosesola.coop.data.remote.GitHubApiService
import com.cecosesola.coop.domain.model.Producto
import com.cecosesola.coop.domain.model.toDomain
import com.cecosesola.coop.domain.model.toEntity
import com.cecosesola.coop.presentation.utils.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

class PreciosRepository(
    private val preciosDao: PreciosDao,
    private val metadataDao: MetadataDao,
    private val favoritosDao: FavoritosDao,
    private val busquedasDao: BusquedasDao,
    private val apiService: GitHubApiService,
    private val networkMonitor: NetworkMonitor
) {
    fun getProductosFlow(): Flow<List<Producto>> = 
        preciosDao.getAllProductosFlow().map { entities -> entities.map { it.toDomain() } }
    
    suspend fun getUltimaSincronizacion(): Long? = metadataDao.getUltimaSync()
    
    fun getFavoritosFlow(): Flow<List<String>> = favoritosDao.getAllFlow()
        .map { entities -> entities.map { it.productoId } }
    
    suspend fun toggleFavorito(productoId: String) {
        if (favoritosDao.isFavorito(productoId)) {
            favoritosDao.deleteById(productoId)
        } else {
            favoritosDao.insert(FavoritoEntity(productoId))
        }
    }
    
    suspend fun isFavorito(productoId: String): Boolean = favoritosDao.isFavorito(productoId)
    
    fun getBusquedasRecientesFlow(): Flow<List<String>> = busquedasDao.getRecientesFlow()
        .map { entities -> entities.map { it.query }.distinct() }
    
    suspend fun guardarBusqueda(query: String) {
        if (query.isNotBlank()) {
            busquedasDao.insert(BusquedaEntity(query = query))
        }
    }
    
    suspend fun eliminarBusqueda(query: String) {
        busquedasDao.deleteByQuery(query)
    }
    
    suspend fun refrescarSiEsNecesario(): Result<Unit> {
        Log.d("Cecosesola", "🔵 REPO: refrescar llamado")
        
        val hayInternet = networkMonitor.isConnected
        Log.d("Cecosesola", "🔵 hayInternet=$hayInternet")
        
        if (!hayInternet) {
            Log.d("Cecosesola", "🔴 Sin conexión")
            return Result.failure(IOException("Sin conexión"))
        }

        return try {
            Log.d("Cecosesola", "🔵 Llamando API...")
            val response = apiService.getPrecios()
            Log.d("Cecosesola", "🔵 API: ${response.productos.size} productos")
            preciosDao.replaceAll(response.productos.map { it.toDomain().toEntity() })
            metadataDao.setUltimaSync(MetadataEntity(ultimaSincronizacion = System.currentTimeMillis()))
            Log.d("Cecosesola", "🔵 Guardado en Room")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Cecosesola", "🔴 ERROR: ${e.message}", e)
            Result.failure(e)
        }
    }
}
