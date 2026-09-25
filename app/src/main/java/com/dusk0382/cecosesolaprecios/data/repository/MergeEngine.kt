package com.dusk0382.cecosesolaprecios.data.repository

import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.data.remote.dto.PayloadOficial
import com.dusk0382.cecosesolaprecios.data.remote.dto.PreciosRepoDto
import com.dusk0382.cecosesolaprecios.data.remote.dto.activo
import com.dusk0382.cecosesolaprecios.domain.normalizarNombre
import com.dusk0382.cecosesolaprecios.domain.rubroDe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Lógica pura de fusión de las dos fuentes (sin Android, testeable en JVM):
 * merge de filas + extracción de valores del payload oficial.
 */
object MergeEngine {

    /** Precios.json → filas base. No toca filas enriquecidas de la API si el
     *  producto ya existe (por repoId o por nombre normalizado). */
    fun filasDesdeRepo(
        dto: PreciosRepoDto,
        existentesPorRepoId: Map<String, ProductEntity>,
        existentesPorNombre: Map<String, ProductEntity>,
    ): List<ProductEntity> = dto.productos.mapNotNull { p ->
        val normal = normalizarNombre(p.nombre)
        val vieja = existentesPorRepoId[p.id] ?: existentesPorNombre[normal]
        // La clase (rubro) se recalcula siempre: el nombre manda sobre el tag
        // heredado de la API, que es mas especifico pero menos confiable.
        val clase = rubroDe(p.nombre).name
        vieja?.copy(
            nombre = p.nombre,
            nombreNormalizado = normal,
            clase = clase,
            precioBs = p.precio,
            imagenUrl = p.imagen ?: vieja.imagenUrl,
            repoId = p.id,
            fuente = if (vieja.apiId != null) "ambas" else "repo",
        ) ?: ProductEntity(
            repoId = p.id,
            nombre = p.nombre,
            nombreNormalizado = normal,
            clase = clase,
            precioBs = p.precio,
            imagenUrl = p.imagen,
            fuente = "repo",
        )
    }

    /** GraphQL oficial → datos de enriquecimiento por producto. */
    fun enriquecidosDesdePayload(payload: PayloadOficial): List<ApiEnriquecido> =
        payload.products?.asSequence()
            ?.map { it.model }
            ?.filter { it.activo() && !it.name.isNullOrBlank() }
            ?.mapNotNull { p ->
                val base = p.pricePublished?.priceBase?.amount?.numberDecimal?.toDoubleOrNull() ?: return@mapNotNull null
                val anterior = p.pricePublished?.oldPrice?.amount?.numberDecimal?.toDoubleOrNull()
                ApiEnriquecido(
                    apiId = p.id ?: return@mapNotNull null,
                    nombre = p.name!!,
                    precioCec = base,
                    precioAnteriorCec = anterior,
                    categoria = p.tags?.firstOrNull()?.lowercase(),
                    marca = p.brand?.takeIf { it.isNotBlank() },
                    presentacion = p.presentation?.trim()?.takeIf { it.isNotEmpty() },
                    barcode = p.barcode?.takeIf { it.isNotBlank() },
                    imagen = p.images?.firstOrNull(),
                    updatedAt = p.updatedAt,
                )
            }?.toList() ?: emptyList()

    /** Enriquecimiento → upsert sobre filas existentes (match: apiId → barcode →
     *  nombre normalizado) o insert nuevo con fuente "api". El delta de precio se
     *  guarda en CEC (misma moneda en ambos puntos); Bs es solo para mostrar. */
    fun filasConEnriquecimiento(
        enriquecidos: List<ApiEnriquecido>,
        existentes: List<ProductEntity>,
        tasaVedPorCec: Double?,
    ): List<ProductEntity> {
        val porApiId = existentes.filter { it.apiId != null }.associateBy { it.apiId!! }
        val porBarcode = existentes.filter { it.barcode != null }.associateBy { it.barcode!! }
        val porNombre = existentes.associateBy { it.nombreNormalizado }
        return enriquecidos.map { e ->
            val vieja = porApiId[e.apiId] ?: e.barcode?.let { porBarcode[it] } ?: porNombre[normalizarNombre(e.nombre)]
            vieja?.copy(
                nombre = e.nombre,
                nombreNormalizado = normalizarNombre(e.nombre),
                clase = rubroDe(e.nombre, listOfNotNull(e.categoria)).name,
                precioBs = vieja.precioBs, // el precio visual sigue siendo la fuente canónica del repo
                apiId = e.apiId,
                categoria = e.categoria,
                marca = e.marca,
                presentacion = e.presentacion,
                barcode = e.barcode,
                imagenGrandeUrl = e.imagen,
                precioCec = e.precioCec,
                precioAnteriorCec = e.precioAnteriorCec,
                updatedAt = e.updatedAt,
                fuente = if (vieja.repoId != null) "ambas" else "api",
            ) ?: ProductEntity(
                apiId = e.apiId,
                nombre = e.nombre,
                nombreNormalizado = normalizarNombre(e.nombre),
                clase = rubroDe(e.nombre, listOfNotNull(e.categoria)).name,
                precioBs = e.precioCec * (tasaVedPorCec ?: 1.0),
                imagenUrl = e.imagen,
                imagenGrandeUrl = e.imagen,
                categoria = e.categoria,
                marca = e.marca,
                presentacion = e.presentacion,
                barcode = e.barcode,
                precioCec = e.precioCec,
                precioAnteriorCec = e.precioAnteriorCec,
                updatedAt = e.updatedAt,
                fuente = "api",
            )
        }
    }

    /** "forSales: [{USD,1},{VED,832.49}]" → tasa VED/CEC (Bs por unidad solidaria). */
    fun tasaVed(payload: PayloadOficial): Double? =
        payload.officialRate?.model?.forSales
            ?.firstOrNull { it.destination == "VED" }
            ?.value?.numberDecimal?.toDoubleOrNull()

    /** Nombres de ferias desde `branches` (JsonElement crudos: _id puede ser int). */
    fun nombresBranches(payload: PayloadOficial, json: Json): List<String> =
        payload.branches?.mapNotNull { el ->
            runCatching {
                val obj = el as? kotlinx.serialization.json.JsonObject ?: return@runCatching null
                obj["name"]?.let { (it as? JsonPrimitive)?.contentOrNull }
            }.getOrNull()
        } ?: emptyList()
}

data class ApiEnriquecido(
    val apiId: String,
    val nombre: String,
    val precioCec: Double,
    val precioAnteriorCec: Double?,
    val categoria: String?,
    val marca: String?,
    val presentacion: String?,
    val barcode: String?,
    val imagen: String?,
    val updatedAt: String?,
)
