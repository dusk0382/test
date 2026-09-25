package com.dusk0382.cecosesolaprecios.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fila única por producto, fusionada entre las dos fuentes.
 *  - `fuente`: "repo" (precios.json), "api" (GraphQL oficial), "ambas".
 *  - Campos de enriquecimiento son nullables: solo la API oficial los aporta.
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    /** id del scraper (ej. "51"); null si el producto solo existe en la API */
    val repoId: String? = null,
    /** id _id de la API oficial; null si solo viene del repo */
    val apiId: String? = null,

    val nombre: String,
    val nombreNormalizado: String,
    /** Rubro derivado del nombre (dominio/Rubros.kt), no el tag de la API.
     *  Se calcula al insertar, nunca en la UI: así la grilla filtra por columna
     *  indexable en vez de clasificar 527 filas por recomposición. */
    val clase: String = "Otros",
    val precioBs: Double,
    val imagenUrl: String? = null,

    // — enriquecimiento (solo API oficial) —
    val categoria: String? = null,
    val marca: String? = null,
    val presentacion: String? = null,
    val barcode: String? = null,
    val imagenGrandeUrl: String? = null,
    /** Precio CEC ("precio solidario") actual — ≈ USD 1:1. */
    val precioCec: Double? = null,
    /** Precio CEC de la actualización anterior (solo API). El % de variación se
     *  calcula CEC↔CEC (misma moneda); Bs solo para mostrar. */
    val precioAnteriorCec: Double? = null,
    val updatedAt: String? = null,

    val fuente: String = "repo",
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val productId: Long,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val productId: Long,
    val quantity: Int = 1,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

object MetaKeys {
    const val REPO_DATE = "repo_fecha_actualizacion"
    const val REPO_SYNC_AT = "repo_sync_at"
    const val API_VERSION = "api_priceListVersion"
    const val API_SYNC_AT = "api_sync_at"
    const val API_RATE_VED = "api_rate_ved_per_cec"
    const val BRANCHES_JSON = "api_branches_json"
}
