package com.cecosesola.coop.domain.model

import com.cecosesola.coop.data.local.ProductoEntity
import com.cecosesola.coop.data.remote.ProductoRemoto

data class Producto(
    val id: String,
    val nombre: String,
    val precio: Double,
    val categoria: String? = null,
    val imagenUrl: String?,
    val presentacion: String = ""
)

fun ProductoRemoto.toDomain(): Producto = Producto(
    id = id,
    nombre = nombre,
    precio = precio,
    categoria = categoria,
    imagenUrl = imagen,
    presentacion = presentacion ?: ""
)

fun Producto.toEntity(): ProductoEntity = ProductoEntity(
    id = id,
    nombre = nombre,
    precio = precio,
    categoria = categoria,
    imagenUrl = imagenUrl,
    presentacion = presentacion
)

fun ProductoEntity.toDomain(): Producto = Producto(
    id = id,
    nombre = nombre,
    precio = precio,
    categoria = categoria,
    imagenUrl = imagenUrl,
    presentacion = presentacion
)
