package com.dusk0382.cecosesolaprecios.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** DTOs del mirror del usuario: dusk0382/cecosesola-data → precios.json */

@Serializable
data class PreciosRepoDto(
    @SerialName("fecha_actualizacion") val fechaActualizacion: String,
    @SerialName("total_productos") val totalProductos: Int = 0,
    @SerialName("productos") val productos: List<ProductoRepoDto>,
)

@Serializable
data class ProductoRepoDto(
    @SerialName("id") val id: String,
    @SerialName("nombre") val nombre: String,
    @SerialName("precio") val precio: Double,
    @SerialName("imagen") val imagen: String? = null,
)
