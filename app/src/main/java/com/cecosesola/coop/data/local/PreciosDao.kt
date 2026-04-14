package com.cecosesola.coop.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PreciosDao {
    @Query("SELECT * FROM productos ORDER BY nombre ASC")
    fun getAllProductosFlow(): Flow<List<ProductoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(productos: List<ProductoEntity>)

    @Query("DELETE FROM productos")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(productos: List<ProductoEntity>) {
        deleteAll()
        insertAll(productos)
    }
}

@Entity(tableName = "metadata")
data class MetadataEntity(
    @PrimaryKey val id: Int = 1, 
    val ultimaSincronizacion: Long
)

@Dao
interface MetadataDao {
    @Query("SELECT ultimaSincronizacion FROM metadata WHERE id = 1")
    suspend fun getUltimaSync(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setUltimaSync(metadata: MetadataEntity)
    
    @Query("UPDATE metadata SET ultimaSincronizacion = :timestamp WHERE id = 1")
    suspend fun updateUltimaSync(timestamp: Long)
}
