package com.cecosesola.coop.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favoritos")
data class FavoritoEntity(
    @PrimaryKey val productoId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "busquedas")
data class BusquedaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
