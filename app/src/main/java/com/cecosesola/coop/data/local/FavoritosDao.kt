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

    /**
     * CORREGIDO: antes era OnConflictStrategy.REPLACE con autoGenerate = true.
     * REPLACE en SQLite hace DELETE + INSERT cuando hay conflicto de clave única,
     * lo que con autoGenerate generaba un nuevo ID en cada inserción — nunca
     * actualizaba la fila existente. Resultado: duplicados infinitos de la misma
     * query y el Flow se disparaba innecesariamente en cada búsqueda repetida.
     *
     * Solución: IGNORE + upsert manual. Si la query ya existe, actualiza solo
     * el timestamp para que suba al top de recientes.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInternal(busqueda: BusquedaEntity): Long

    @Query("UPDATE busquedas SET timestamp = :ts WHERE query = :query")
    suspend fun updateTimestamp(query: String, ts: Long)

    @Transaction
    suspend fun upsert(busqueda: BusquedaEntity) {
        val id = insertInternal(busqueda)
        if (id == -1L) {
            // Ya existía — solo actualiza el timestamp para que suba al top
            updateTimestamp(busqueda.query, busqueda.timestamp)
        }
    }

    @Query("DELETE FROM busquedas WHERE query = :query")
    suspend fun deleteByQuery(query: String)

    @Query("DELETE FROM busquedas")
    suspend fun deleteAll()
}
