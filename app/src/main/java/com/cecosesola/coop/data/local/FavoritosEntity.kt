package com.cecosesola.coop.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "favoritos")
data class FavoritoEntity(
    @PrimaryKey val productoId: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * CORREGIDO: Index único en 'query' para que el upsert del DAO funcione
 * correctamente. Sin este índice, IGNORE no detecta el conflicto y se
 * insertan duplicados igualmente.
 */
@Entity(
    tableName = "busquedas",
    indices = [Index(value = ["query"], unique = true)]
)
data class BusquedaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
