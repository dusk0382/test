package com.dusk0382.cecosesolaprecios

import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.data.repository.MergeEngine
import com.dusk0382.cecosesolaprecios.data.remote.dto.GraphqlEnvelope
import com.dusk0382.cecosesolaprecios.data.remote.dto.PayloadOficial
import com.dusk0382.cecosesolaprecios.data.remote.dto.PreciosRepoDto
import com.dusk0382.cecosesolaprecios.domain.normalizarNombre
import java.io.BufferedReader
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Anti-drift: parsea las RESPUESTAS REALES capturadas el 2026-09-13 de ambas
 * fuentes. Si Cecosesola o el scraper cambian el schema, CI se pone rojo antes
 * de que se vea en el teléfono.
 */
class ParserTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun fixture(name: String): String =
        javaClass.classLoader!!.getResourceAsStream("fixtures/$name")!!
            .bufferedReader().use(BufferedReader::readText)

    @Test
    fun `precios json del mirror parsea completo`() {
        val dto = json.decodeFromString(PreciosRepoDto.serializer(), fixture("precios.json"))
        assertTrue("esperaba >500 productos, ${dto.productos.size}", dto.productos.size > 500)
        assertEquals(dto.totalProductos, dto.productos.size)
        assertTrue(dto.fechaActualizacion.isNotBlank())
        val maria = dto.productos.first { it.id == "1" }
        assertEquals("Galleta Maria Puig", maria.nombre)
        assertEquals(1365.0, maria.precio, 0.001)
        assertTrue(maria.imagen!!.startsWith("https://raw.githubusercontent.com"))
    }

    @Test
    fun `graphql envelope y payload oficial parsean`() {
        val raw = fixture("downloadfile_graphql.json")
        val envelope = json.decodeFromString(GraphqlEnvelope.serializer(), raw)
        val inner = envelope.data?.downloadFile
        assertNotNull("downloadFile faltante", inner)
        val payload = json.decodeFromString(PayloadOficial.serializer(), inner!!)

        assertTrue(payload.products!!.size > 500)
        assertNotNull(payload.priceList?.model?.version)
        assertNotNull(payload.officialRate)

        val enriquecidos = MergeEngine.enriquecidosDesdePayload(payload)
        assertTrue("esperaba >500 enriquecidos, ${enriquecidos.size}", enriquecidos.size > 500)
        val galleta = enriquecidos.first { it.nombre == "Galleta Maria Puig" }
        assertEquals("confiteria", galleta.categoria)
        assertEquals("7591082000307", galleta.barcode)
        assertEquals("Puig", galleta.marca)
        assertEquals(1.6815801, galleta.precioCec, 0.01)
        // Precio anterior: vive en oldPrice.priceBase.amount (un nivel más
        // adentro). Valor tomado del fixture, no inventado.
        assertEquals(1.639658870012853, galleta.precioAnteriorCec!!, 0.001)

        val tasa = MergeEngine.tasaVed(payload)
        assertEquals(832.49, tasa!!, 0.01)
        assertEquals(4, MergeEngine.nombresBranches(payload, json).size)
    }

    /**
     * El test que faltaba y por el que la variación de precio estuvo muerta
     * con el CI en verde: MergeTest armaba el DTO a mano, así que el parser de
     * `oldPrice` nunca se probó y `precioAnteriorCec` salía siempre null.
     * `DeltaBadge` sale temprano con null, así que el fallo no se veía en
     * ninguna parte salvo acá.
     *
     * Assert contra el fixture real: hoy los 527 productos traen precio
     * anterior, así que la cobertura esperada es 100%.
     */
    @Test
    fun `el precio anterior se lee del fixture y llega al merge`() {
        val payload = payloadOficial()
        val enriquecidos = MergeEngine.enriquecidosDesdePayload(payload)
        val conAnterior = enriquecidos.count { it.precioAnteriorCec != null }
        assertEquals(
            "el precio anterior debe llegar para todos los productos del fixture",
            enriquecidos.size,
            conAnterior,
        )

        // Y que sobreviva el merge: esta es la mitad que MergeTest no cubría
        // porque construía el DTO a mano en vez de parsear el fixture.
        val base = ProductEntity(
            localId = 1,
            repoId = "1",
            nombre = "Galleta Maria Puig",
            nombreNormalizado = "galleta maria puig",
            precioBs = 1365.0,
            fuente = "repo",
        )
        val fila = MergeEngine.filasConEnriquecimiento(
            listOf(enriquecidos.first { it.nombre == "Galleta Maria Puig" }),
            listOf(base),
            tasaVedPorCec = 832.49,
        ).single()
        assertEquals("elBs del mirror sigue siendo el canónico", 1365.0, fila.precioBs, 0.001)
        assertNotNull("el precio anterior debe sobrevivir al merge", fila.precioAnteriorCec)
        assertEquals(1.639658870012853, fila.precioAnteriorCec!!, 0.001)
    }

    @Test
    fun `productos del graphql tienen barcode y tags parciales`() {
        val payload = payloadOficial()
        val conBarcode = payload.products!!.count { it.model.barcode?.isNotBlank() == true }
        assertTrue("todos tienen barcode hoy: $conBarcode", conBarcode > 400)
    }

    private fun payloadOficial(): PayloadOficial {
        val envelope = json.decodeFromString(
            GraphqlEnvelope.serializer(),
            fixture("downloadfile_graphql.json"),
        )
        return json.decodeFromString(PayloadOficial.serializer(), envelope.data!!.downloadFile!!)
    }
}

class NormalizaTest {
    @Test
    fun `normaliza para matching`() {
        assertEquals("mayonesa mavesa 910gr", normalizarNombre("Mayonesa Mavesa 910gr"))
        assertEquals("jabon en polvo alive limon", normalizarNombre("Jabón  en polvo Alive   Limón "))
        assertEquals("gelatina sonrisa cereza", normalizarNombre("GELATINA SONRISA CEREZA"))
    }
}
