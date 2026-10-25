package com.dusk0382.cecosesolaprecios

import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.data.repository.ApiEnriquecido
import com.dusk0382.cecosesolaprecios.data.repository.MergeEngine
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Casos borde del matching del enriquecimiento: el barcode como identidad y la
 * prioridad del apiId.
 *
 * El orden de matching es apiId → barcode → nombre normalizado. El apiId manda:
 * si una base instalada quedó con un apiId mal asignado, ningún enrich posterior
 * lo corrige porque siempre vuelve a encontrar la misma fila. Por eso existe
 * "Reconstruir enriquecimiento", que borra apiId/barcode/CEC de todas las filas
 * y fuerza un match limpio por nombre o barcode.
 *
 * `enq` está duplicado de MergeTest a propósito: ese archivo no se toca.
 */
class MergeEdgeTest {

    private fun enq(
        id: String,
        nombre: String,
        barcode: String? = null,
        cec: Double = 1.0,
    ) = ApiEnriquecido(
        apiId = id,
        nombre = nombre,
        precioCec = cec,
        precioAnteriorCec = cec * 0.9,
        categoria = "confiteria",
        marca = "Puig",
        presentacion = "250gr",
        barcode = barcode,
        imagen = "https://x/$id.png",
        updatedAt = "2026-09-12",
    )

    @Test
    fun `un enriquecido con otro nombre matchea por barcode y recalcula el precio`() {
        val soloApi = ProductEntity(
            localId = 21,
            repoId = null,
            apiId = null,
            nombre = "Nombre que trajo el escaneo",
            nombreNormalizado = "nombre que trajo el escaneo",
            precioBs = 10.0,
            barcode = "12345670",
            fuente = "api",
        )
        val filas = MergeEngine.filasConEnriquecimiento(
            listOf(enq("501", "Nombre oficial distinto", barcode = "12345670", cec = 2.0)),
            listOf(soloApi),
            tasaVedPorCec = 800.0,
        )

        assertEquals(1, filas.size)
        val f = filas[0]
        // El nombre cambió, así que solo el barcode pudo identificar la fila.
        assertEquals(21L, f.localId)
        assertEquals("501", f.apiId)
        assertEquals("12345670", f.barcode)
        assertEquals("Nombre oficial distinto", f.nombre)
        // Fila sin repoId: el precio en Bs se deriva de CEC * tasa, no se conserva
        // el 10.0 viejo (que venía de otra tasa o de un escaneo).
        assertEquals(2.0 * 800.0, f.precioBs, 0.001)
        assertEquals("api", f.fuente)
    }

    @Test
    fun `el matching por apiId tiene prioridad y conserva el localId`() {
        val existente = ProductEntity(
            localId = 34,
            repoId = null,
            apiId = "77",
            nombre = "Nombre anterior",
            nombreNormalizado = "nombre anterior",
            precioBs = 0.0,
            fuente = "api",
        )
        // Sin barcode y con otro nombre: el único match posible es el apiId.
        val filas = MergeEngine.filasConEnriquecimiento(
            listOf(enq("77", "Nombre nuevo", cec = 1.0)),
            listOf(existente),
            tasaVedPorCec = 800.0,
        )

        assertEquals(1, filas.size)
        assertEquals(34L, filas[0].localId)
        assertEquals("77", filas[0].apiId)
        assertEquals("Nombre nuevo", filas[0].nombre)
    }
}
