package com.cecosesola.coop.data.local

import androidx.room.*

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
}
