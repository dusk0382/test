package com.dusk0382.cecosesolaprecios

import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.data.remote.dto.PreciosRepoDto
import com.dusk0382.cecosesolaprecios.data.remote.dto.ProductoRepoDto
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
        // Sin tasa oficial no se inventa un precio en Bs. Antes era `precioCec * 1.0`,
        // que pintaba "Bs 1,68" para 1,68 CEC: 500 veces menor al real.
        assertEquals(0.0, filas[0].precioBs, 0.001)
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
        val dto = PreciosRepoDto(
            fechaActualizacion = "2026-09-13 00:00:00",
            totalProductos = 1,
            productos = listOf(
                ProductoRepoDto(id = "2", nombre = "Mayonesa Mavesa 910gr", precio = 6510.0, imagen = null),
            ),
        )
        val filas = MergeEngine.filasDesdeRepo(dto, listOf(enriquecida))
        assertEquals(1, filas.size)
        // el bug del proyecto viejo (replaceAll) se pagaba con favoritos huérfanos:
        assertEquals(3L, filas[0].localId)
        assertEquals("B123", filas[0].barcode)
        assertEquals(6510.0, filas[0].precioBs, 0.001)
        assertEquals("55", filas[0].apiId)
    }

    /**
     * El medio hermano del bug de `replaceAll`: el mirror real tiene 18 grupos de
     * nombres duplicados (42 productos), uno con el mismo nombre a distinto precio.
     * Cuando el scraper agrega un id nuevo para un nombre que ya existe, el
     * fallback por nombre reescribía la fila existente: el `repoId` viejo quedaba
     * huérfano y el producto desaparecía para siempre (el sync siguiente ni lo
     * intentaba, porque `fecha_actualizacion` ya estaba guardada).
     */
    @Test
    fun `un nombre repetido no roba la fila del producto que ya tiene repoId`() {
        val existente = ProductEntity(
            localId = 1,
            repoId = "138",
            nombre = "Prestobarba Razormax",
            nombreNormalizado = "prestobarba razormax",
            precioBs = 100.0,
            fuente = "repo",
        )
        val dto = PreciosRepoDto(
            fechaActualizacion = "2026-09-13 00:00:00",
            totalProductos = 2,
            productos = listOf(
                ProductoRepoDto(id = "138", nombre = "Prestobarba Razormax", precio = 105.0, imagen = null),
                ProductoRepoDto(id = "392", nombre = "Prestobarba Razormax", precio = 110.0, imagen = null),
            ),
        )
        val filas = MergeEngine.filasDesdeRepo(dto, listOf(existente))

        // El repoId conocido se actualiza sobre la fila existente...
        val actualizada = filas.first { it.repoId == "138" }
        assertEquals(1L, actualizada.localId)
        assertEquals(105.0, actualizada.precioBs, 0.001)
        // ...y el nuevo entra como fila nueva, sin pisar la anterior.
        val nueva = filas.first { it.repoId == "392" }
        assertEquals(0L, nueva.localId) // 0 = sin asignar todavía, la asigna Room
        assertEquals(110.0, nueva.precioBs, 0.001)
    }

    /** El caso que el fallback por nombre sí debe resolver: un renglón que la API
     *  trajo primero (sin repoId) y que después aparece en el mirror. Se le pega
     *  el precio al renglón existente en vez de duplicarlo. */
    @Test
    fun `el fallback por nombre si pega una fila que solo vino de la API`() {
        val soloApi = ProductEntity(
            localId = 9,
            repoId = null,
            apiId = "77",
            nombre = "Gelatina Sonrisa Cereza",
            nombreNormalizado = "gelatina sonrisa cereza",
            precioBs = 0.0,
            fuente = "api",
        )
        val dto = PreciosRepoDto(
            fechaActualizacion = "2026-09-13 00:00:00",
            totalProductos = 1,
            productos = listOf(
                ProductoRepoDto(id = "5", nombre = "Gelatina Sonrisa Cereza", precio = 33.0, imagen = null),
            ),
        )
        val filas = MergeEngine.filasDesdeRepo(dto, listOf(soloApi))
        assertEquals(1, filas.size)
        assertEquals(9L, filas[0].localId)
        assertEquals("5", filas[0].repoId)
        assertEquals("ambas", filas[0].fuente)
        assertEquals(33.0, filas[0].precioBs, 0.001)
    }
}
