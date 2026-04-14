package com.cecosesola.coop.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos")
data class ProductoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val precio: Double,
    val categoria: String?,
    val imagenUrl: String?,
    val presentacion: String = ""
)
