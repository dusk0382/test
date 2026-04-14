package com.cecosesola.coop.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritosDao {
    @Query("SELECT * FROM favoritos ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<FavoritoEntity>>
    
    @Query("SELECT EXISTS(SELECT 1 FROM favoritos WHERE productoId = :id)")
    suspend fun isFavorito(id: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorito: FavoritoEntity)
    
    @Query("DELETE FROM favoritos WHERE productoId = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM favoritos")
    suspend fun deleteAll()
}

@Dao
interface BusquedasDao {
    @Query("SELECT * FROM busquedas ORDER BY timestamp DESC LIMIT 10")
    fun getRecientesFlow(): Flow<List<BusquedaEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(busqueda: BusquedaEntity)
    
    @Query("DELETE FROM busquedas WHERE query = :query")
    suspend fun deleteByQuery(query: String)
    
    @Query("DELETE FROM busquedas")
    suspend fun deleteAll()
}
