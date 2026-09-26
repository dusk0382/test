package com.dusk0382.cecosesolaprecios

import com.dusk0382.cecosesolaprecios.data.repository.MergeEngine
import com.dusk0382.cecosesolaprecios.domain.aHttps
import java.io.BufferedReader
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La API oficial entrega las imágenes en `http://`. Con el `network_security_config`
 * allowlistando un solo host, 146 de las 453 URLs del fixture no cargaban nunca y
 * la tarjeta se quedaba con el contenedor tonal vacío, sin explicación.
 *
 * Este test ata la corrección a los datos reales: si el fixture dejara de traer
 * imágenes http, la función sobraría y habría que borrarla.
 */
class UrlsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun `una url http pasa a https y una https no se toca`() {
        assertEquals(
            "https://kana.imk.cecosesola.coop/x.png",
            aHttps("http://kana.imk.cecosesola.coop/x.png"),
        )
        assertEquals(
            "https://raw.githubusercontent.com/dusk0382/x/1.jpg",
            aHttps("https://raw.githubusercontent.com/dusk0382/x/1.jpg"),
        )
        // Mayúsculas en el esquema: las URLs no son case-insensitive en la
        // práctica, pero el chequeo no debe fallar si llega una así.
        assertEquals("https://ejemplo/x.png", aHttps("HTTP://ejemplo/x.png"))
    }

    @Test
    fun `una url que no es http se devuelve igual`() {
        assertEquals("kana.imk.cecosesola.coop/x.png", aHttps("kana.imk.cecosesola.coop/x.png"))
        assertEquals("", aHttps(""))
    }

    /**
     * El motivo del cambio: después de pasar por [MergeEngine], ninguna imagen
     * guardada puede seguir en http. Con cleartext ya no permitido, una URL http
     * es una imagen rota garantizado.
     */
    @Test
    fun `tras el merge ninguna imagen queda en http`() {
        val inner = json.decodeFromString(
            com.dusk0382.cecosesolaprecios.data.remote.dto.GraphqlEnvelope.serializer(),
            javaClass.classLoader!!.getResourceAsStream("fixtures/downloadfile_graphql.json")!!
                .bufferedReader().use(BufferedReader::readText),
        ).data!!.downloadFile!!
        val payload = json.decodeFromString(
            com.dusk0382.cecosesolaprecios.data.remote.dto.PayloadOficial.serializer(),
            inner,
        )
        val enriquecidos = MergeEngine.enriquecidosDesdePayload(payload)
        val conImagen = enriquecidos.filter { !it.imagen.isNullOrBlank() }
        assertTrue("el fixture deberia traer imágenes", conImagen.isNotEmpty())
        val enHttp = conImagen.filter { it.imagen!!.startsWith("http://", ignoreCase = true) }
        assertTrue(
            "quedaron ${enHttp.size} imágenes en http de ${conImagen.size}: no van a cargar. " +
                "Ejemplos: ${enHttp.take(3).map { it.imagen }}",
            enHttp.isEmpty(),
        )
    }

    @Test
    fun `el fixture real sigue trayendo el http que hay que corregir`() {
        // Si la API dejara de servir http, este test avisa de que aHttps() quedó
        // como un no-op y el cleartext se podría volver a permitir sin riesgo.
        val inner = json.decodeFromString(
            com.dusk0382.cecosesolaprecios.data.remote.dto.GraphqlEnvelope.serializer(),
            javaClass.classLoader!!.getResourceAsStream("fixtures/downloadfile_graphql.json")!!
                .bufferedReader().use(BufferedReader::readText),
        ).data!!.downloadFile!!
        assertTrue(
            "el fixture ya no trae URLs http: la premisa de aHttps() cambio",
            inner.contains("http://kana.imk.cecosesola.coop"),
        )
        assertFalse(
            "el fixture ya no trae el segundo host de Cecosesola",
            inner.contains("imolko.net"),
        )
    }
}
