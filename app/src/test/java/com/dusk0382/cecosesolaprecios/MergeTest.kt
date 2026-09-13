package com.dusk0382.cecosesolaprecios

import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.data.repository.ApiEnriquecido
import com.dusk0382.cecosesolaprecios.data.repository.MergeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MergeTest {

    private fun enq(id: String, nombre: String, barcode: String? = null, cec: Double = 1.0) =
        ApiEnriquecido(
            apiId = id, nombre = nombre, precioCec = cec, precioAnteriorCec = cec * 0.9,
            categoria = "confiteria", marca = "Puig", presentacion = "250gr",
            barcode = barcode, imagen = "https://x/$id.png", updatedAt = "2026-09-12",
        )

    @Test
    fun `enriquecimiento matchea por nombre y conserva precio del repo`() {
        val base = ProductEntity(
            localId = 7,
            repoId = "1",
            nombre = "Galleta Maria Puig",
            nombreNormalizado = "galleta maria puig",
            precioBs = 1365.0,
            fuente = "repo",
        )
        val filas = MergeEngine.filasConEnriquecimiento(
            listOf(enq("51", "Galleta Maria Puig", barcode = "7591082000307")),
            listOf(base),
            tasaVedPorCec = 832.49,
        )
        assertEquals(1, filas.size)
        val f = filas[0]
        assertEquals(7L, f.localId)
        assertEquals("ambas", f.fuente)
        assertEquals("7591082000307", f.barcode)
        assertEquals("confiteria", f.categoria)
        assertEquals(1365.0, f.precioBs, 0.001) // canónica sigue siendo el mirror
        assertEquals(0.9, f.precioAnteriorCec!!, 0.001) // el % se calcula CEC↔CEC
    }

    @Test
    fun `producto solo de la API entra con fuente api`() {
        val filas = MergeEngine.filasConEnriquecimiento(
            listOf(enq("999", "Producto Nuevo Sin En Repo")),
            emptyList(),
            tasaVedPorCec = null,
        )
        assertEquals(1, filas.size)
        assertEquals("api", filas[0].fuente)
        assertEquals(1.0, filas[0].precioBs, 0.001) // sin tasa: CEC ≈ Bs
        assertNull(filas[0].repoId)
    }

    @Test
    fun `merge del mirror no destruye filas enriquecidas`() {
        val enriquecida = ProductEntity(
            localId = 3,
            repoId = "2",
            apiId = "55",
            nombre = "Mayonesa Mavesa 910gr",
            nombreNormalizado = "mayonesa mavesa 910gr",
            precioBs = 6500.0,
            barcode = "B123",
            fuente = "ambas",
        )
        val dto = com.dusk0382.cecosesolaprecios.data.remote.dto.PreciosRepoDto(
            fechaActualizacion = "2026-09-13 00:00:00",
            totalProductos = 1,
            productos = listOf(
                com.dusk0382.cecosesolaprecios.data.remote.dto.ProductoRepoDto(
                    id = "2", nombre = "Mayonesa Mavesa 910gr", precio = 6510.0, imagen = null,
                ),
            ),
        )
        val filas = MergeEngine.filasDesdeRepo(dto, mapOf("2" to enriquecida), emptyMap())
        assertEquals(1, filas.size)
        // el bug del proyecto viejo (replaceAll) se pagaba con favoritos huérfanos:
        assertEquals(3L, filas[0].localId)
        assertEquals("B123", filas[0].barcode)
        assertEquals(6510.0, filas[0].precioBs, 0.001)
        assertEquals("55", filas[0].apiId)
    }
}
