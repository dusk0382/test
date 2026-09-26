package com.dusk0382.cecosesolaprecios.data.repository

import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.data.remote.dto.PayloadOficial
import com.dusk0382.cecosesolaprecios.data.remote.dto.PreciosRepoDto
import com.dusk0382.cecosesolaprecios.data.remote.dto.activo
import com.dusk0382.cecosesolaprecios.data.remote.dto.toDoubleOrNull
import com.dusk0382.cecosesolaprecios.domain.aHttps
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

    /**
     * Precios.json → filas base.
     *
     * Matcheo: primero por `repoId` (la clave estable del scraper) y solo si no
     * aparece, por nombre normalizado.
     *
     * El fallback por nombre tiene una restricción que no es negociable: **solo
     * puede tocar filas que no tengan `repoId`**. Una fila con `repoId` ya fue
     * claimed por el mirror con ese id, y si un producto nuevo del mirror trae
     * un nombre que se normaliza igual al de un producto existente pero es en
     * realidad otro producto, reescribir la fila la roba: el `repoId` viejo queda
     * huérfano y el producto desaparece. Y como después se guarda
     * `fecha_actualizacion`, el siguiente sync ni lo intenta recuperar.
     *
     * El caso que el fallback sí resuelve, y vale la pena conservar: un producto
     * que la app encontró primero por la API oficial (sin `repoId`) y que después
     * aparece en el mirror. Ahí hay que pegarle el precio al renglón existente y
     * no duplicarlo.
     */
    fun filasDesdeRepo(
        dto: PreciosRepoDto,
        existentes: List<ProductEntity>,
    ): List<ProductEntity> {
        val porRepoId = existentes.filter { it.repoId != null }.associateBy { it.repoId!! }
        // Solo filas sin repoId son candidatas al fallback por nombre.
        val porNombre = existentes.filter { it.repoId == null }
            .associateBy { it.nombreNormalizado }
        // Un renglón no puede ser claimado dos veces en la misma pasada.
        val claimedPorNombre = mutableSetOf<String>()

        return dto.productos.mapNotNull { p ->
            val normal = normalizarNombre(p.nombre)
            val porId = porRepoId[p.id]
            val porNombreNorm = porNombre[normal]?.takeIf { normal !in claimedPorNombre }
            val vieja = porId ?: porNombreNorm
            if (porNombreNorm != null) claimedPorNombre += normal
            // La clase (rubro) se recalcula siempre: el nombre manda sobre el tag
            // heredado de la API, que es mas especifico pero menos confiable.
            val clase = rubroDe(p.nombre).name
            vieja?.copy(
                nombre = p.nombre,
                nombreNormalizado = normal,
                clase = clase,
                precioBs = p.precio,
                imagenUrl = p.imagen?.let(::aHttps) ?: vieja.imagenUrl,
                repoId = p.id,
                fuente = if (vieja.apiId != null) "ambas" else "repo",
            ) ?: ProductEntity(
                repoId = p.id,
                nombre = p.nombre,
                nombreNormalizado = normal,
                clase = clase,
                precioBs = p.precio,
                imagenUrl = p.imagen?.let(::aHttps),
                fuente = "repo",
            )
        }
    }

    /** GraphQL oficial → datos de enriquecimiento por producto. */
    fun enriquecidosDesdePayload(payload: PayloadOficial): List<ApiEnriquecido> =
        payload.products?.asSequence()
            ?.map { it.model }
            ?.filter { it.activo() && !it.name.isNullOrBlank() }
            ?.mapNotNull { p ->
                val base = p.pricePublished?.priceBase?.amount?.toDoubleOrNull() ?: return@mapNotNull null
                // El precio anterior viene anidado un nivel más: oldPrice tiene la
                // misma forma que pricePublished, el monto está en oldPrice.priceBase.
                val anterior = p.pricePublished?.oldPrice?.priceBase?.amount?.toDoubleOrNull()
                ApiEnriquecido(
                    apiId = p.id ?: return@mapNotNull null,
                    nombre = p.name!!,
                    precioCec = base,
                    precioAnteriorCec = anterior,
                    categoria = p.tags?.firstOrNull()?.lowercase(),
                    marca = p.brand?.takeIf { it.isNotBlank() },
                    presentacion = p.presentation?.trim()?.takeIf { it.isNotEmpty() },
                    barcode = p.barcode?.takeIf { it.isNotBlank() },
                    // La API oficial sirve las imágenes en http. Se sube el esquema al
                    // guardar: los dos hosts de Cecosesola responden por https, y así la app
                    // no necesita cleartext (ver domain/Urls.kt).
                    imagen = p.images?.firstOrNull()?.let(::aHttps),
                    updatedAt = p.updatedAt,
                )
            }?.toList() ?: emptyList()

    /** Enriquecimiento → upsert sobre filas existentes (match: apiId → barcode →
     *  nombre normalizado) o insert nuevo con fuente "api". El delta de precio se
     *  guarda en CEC (misma moneda en ambos puntos); Bs es solo para mostrar.
     *
     *  Misma restricción que en [filasDesdeRepo]: el fallback por nombre solo
     *  puede tocar filas que todavía no tienen `apiId` ni `barcode` identificables,
     *  y una fila no se claima dos veces en la misma pasada. Sin eso, un nombre
     *  repetido del catálogo real (18 grupos, 42 productos) hace que un producto
     *  escriba sobre otro. */
    fun filasConEnriquecimiento(
        enriquecidos: List<ApiEnriquecido>,
        existentes: List<ProductEntity>,
        tasaVedPorCec: Double?,
    ): List<ProductEntity> {
        val porApiId = existentes.filter { it.apiId != null }.associateBy { it.apiId!! }
        val porBarcode = existentes.filter { it.barcode != null }.associateBy { it.barcode!! }
        // Solo filas sin apiId son candidatas al fallback por nombre: una fila con
        // apiId pertenece a otro producto de la API aunque se llame igual.
        val porNombre = existentes.filter { it.apiId == null }
            .groupBy { it.nombreNormalizado }
            .mapValues { (_, filas) -> filas.first() }
        val claimed = mutableSetOf<Long>()

        return enriquecidos.map { e ->
            val normal = normalizarNombre(e.nombre)
            val vieja = porApiId[e.apiId]
                ?: e.barcode?.let { porBarcode[it] }
                ?: porNombre[normal]?.takeIf { it.localId !in claimed }
            vieja?.let { claimed += it.localId }
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
                // Sin tasa oficial no hay forma honesta de expresar un precio
                // solidario en Bs. Antes se usaba `?: 1.0`, que pintaba "Bs 1,68"
                // para un producto de 1,68 CEC: un precio 500 veces menor al real
                // que el usuario leía como si fuera el del producto. Con 0.0 la
                // tarjeta muestra Bs 0,00 y no inventa un número.
                precioBs = e.precioCec * (tasaVedPorCec ?: 0.0),
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
            ?.value?.toDoubleOrNull()

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
