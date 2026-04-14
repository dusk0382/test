package com.cecosesola.coop.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Índices en nombre y categoria:
 * - Aceleran los ORDER BY y los WHERE que hace Room internamente.
 * - El filtrado de búsqueda en el ViewModel recorre menos datos.
 *
 * IMPORTANTE: añadir índices requiere incrementar la versión de la base de datos
 * en PreciosDatabase y proveer una Migration (o allowDestructiveMigration en dev).
 */
@Entity(
    tableName = "productos",
    indices = [
        Index(value = ["nombre"]),
        Index(value = ["categoria"])
    ]
)
data class ProductoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val precio: Double,
    val categoria: String?,
    val imagenUrl: String?,
    val presentacion: String = ""
)
