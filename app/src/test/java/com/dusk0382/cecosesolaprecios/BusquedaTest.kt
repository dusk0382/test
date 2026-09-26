package com.dusk0382.cecosesolaprecios

import com.dusk0382.cecosesolaprecios.data.remote.dto.GraphqlEnvelope
import com.dusk0382.cecosesolaprecios.data.remote.dto.PayloadOficial
import com.dusk0382.cecosesolaprecios.data.repository.barcodeDeConsulta
import com.dusk0382.cecosesolaprecios.data.repository.consultaDeBusqueda
import com.dusk0382.cecosesolaprecios.data.repository.escaparLike
import java.io.BufferedReader
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La búsqueda es la interacción más frecuente de la app y se armaba con tres
 * `string` pelados dentro del repositorio, sin ninguna prueba. Estas funciones
 * son puras justamente para poder verificarse acá: si `escaparLike` deja pasar un
 * `%`, el buscador devuelve los 518 productos y ningún test JVM se entera.
 */
class BusquedaTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun `la consulta se normaliza como la columna donde se busca`() {
        // `nombreNormalizado` no tiene acentos ni mayúsculas, así que la consulta
        // tiene que ir igual: por eso "Limón" tiene que encontrar "limon".
        assertEquals("limon", consultaDeBusqueda("Limón"))
        assertEquals("leche", consultaDeBusqueda("  LECHE  "))
        assertEquals("", consultaDeBusqueda(""))
    }

    @Test
    fun `los comodines de LIKE se escapan`() {
        // Sin escapar, "%" traía el catálogo entero y "_" emparejaba cualquier
        // carácter. Un "%" es un carácter normal en un nombre ("QUESO 100%") y el
        // usuario lo escribe sin saber que dispara un comodín.
        assertEquals("100\\%", escaparLike("100%"))
        assertEquals("a\\_b", escaparLike("a_b"))
        assertEquals("c\\\\d", escaparLike("c\\d"))
        assertEquals("limon", escaparLike("limon"))
    }

    @Test
    fun `la barra invertida se escapa antes que los comodines`() {
        // El orden importa. Si los comodines se reemplazaran primero, la barra ya
        // escapada a "\\" volvería a llevar sus dos barras y el LIKE leería un
        // comodín con una barra de más. "\\%" son 1 barra + "%", y el resultado
        // esperado son 3 barras + "%": una del escape de la barra, dos del escape
        // del comodín.
        val escapado = escaparLike("\\%")
        assertEquals(3, escapado.count { it == '\\' })
        assertTrue("debe terminar en %", escapado.endsWith("%"))
    }

    @Test
    fun `un barcode de 8 o 13 digitos se reconoce`() {
        // 13 dígitos: el EAN-13 real de Galleta Maria Puig en el fixture.
        assertEquals("7591082000307", barcodeDeConsulta("7591082000307"))
        assertEquals("12345670", barcodeDeConsulta("12345670"))
        assertEquals("7591082000307", barcodeDeConsulta("  7591082000307  "))
    }

    @Test
    fun `un numero que no es un barcode no se trata como barcode`() {
        // "2026" son 4 dígitos y es un año, no un código. Sin este filtro, buscar
        // un precio en el catálogo devolvería productos por coincidencia de
        // barcode, que es peor que no devolver nada.
        assertEquals("", barcodeDeConsulta("2026"))
        assertEquals("", barcodeDeConsulta("1234"))
        assertEquals("", barcodeDeConsulta(""))
        // 6 dígitos es ambiguo entre un código corto, un peso y una cantidad.
        assertEquals("", barcodeDeConsulta("123456"))
        // 13 dígitos pero con letras no es un barcode.
        assertEquals("", barcodeDeConsulta("75910820003AB"))
    }

    /**
     * El scanner produce códigos de barras y la base los guarda, pero la
     * búsqueda solo miraba el nombre: escribir el código de lo que acabás de
     * escanear no encontraba nada, que es el uso más obvio del campo en una app
     * que escanea.
     *
     * Este test ata la función al dato real: si el fixture dejara de traer
     * barcodes, la rama del código de barras quedaría muerta y habría que
     * borrarla.
     */
    @Test
    fun `el barcode del fixture real se reconoce como consulta`() {
        val inner = json.decodeFromString(
            GraphqlEnvelope.serializer(),
            javaClass.classLoader!!.getResourceAsStream("fixtures/downloadfile_graphql.json")!!
                .bufferedReader().use(BufferedReader::readText),
        ).data!!.downloadFile!!
        val payload = json.decodeFromString(PayloadOficial.serializer(), inner)
        val galleta = payload.products!!.first { it.model.name == "Galleta Maria Puig" }.model

        val barcode = galleta.barcode!!
        assertEquals("EAN-13 en el fixture real", 13, barcode.length)
        assertEquals(barcode, barcodeDeConsulta(barcode))
        assertTrue(
            "el fixture no tiene barcodes para la mayoría de los productos",
            payload.products!!.count { it.model.barcode?.isNotBlank() == true } > 400,
        )
    }
}
