package com.dusk0382.cecosesolaprecios

import com.dusk0382.cecosesolaprecios.data.remote.dto.GraphqlEnvelope
import com.dusk0382.cecosesolaprecios.data.remote.dto.PayloadOficial
import com.dusk0382.cecosesolaprecios.data.remote.dto.PreciosRepoDto
import com.dusk0382.cecosesolaprecios.domain.Rubro
import com.dusk0382.cecosesolaprecios.domain.formatearNombreProducto
import com.dusk0382.cecosesolaprecios.domain.normalizarNombre
import com.dusk0382.cecosesolaprecios.domain.rubroDe
import java.io.BufferedReader
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El catálogo no trae taxonomía usable (ver KDoc de `Rubros.kt`), así que la
 * clasificación es nuestra y por eso se mide: si el vocabulario se rompe o alguien
 * reordena las reglas, CI avisa acá y no en el teléfono.
 *
 * Estos números salen de los fixtures reales; los umbrales son un piso, no el
 * resultado exacto, para que el test no se caiga por un producto nuevo.
 */
class RubrosTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun fixture(name: String): String =
        javaClass.classLoader!!.getResourceAsStream("fixtures/$name")!!
            .bufferedReader().use(BufferedReader::readText)

    private fun productosMirror(): List<Pair<String, List<String>>> {
        val mirror = json.decodeFromString(PreciosRepoDto.serializer(), fixture("precios.json"))

        val envelope = json.decodeFromString(GraphqlEnvelope.serializer(), fixture("downloadfile_graphql.json"))
        val payload = json.decodeFromString(PayloadOficial.serializer(), envelope.data!!.downloadFile!!)
        val tagsPorNombre = payload.products!!.mapNotNull { it.model }.associate {
            normalizarNombre(it.name ?: "") to (it.tags ?: emptyList())
        }

        return mirror.productos.map { it.nombre to (tagsPorNombre[normalizarNombre(it.nombre)] ?: emptyList()) }
    }

    private fun cobertura(): Triple<Int, Map<Rubro, Int>, List<String>> {
        val productos = productosMirror()
        val conteo = mutableMapOf<Rubro, Int>()
        val sinRubro = mutableListOf<String>()
        productos.forEach { (nombre, tags) ->
            val rubro = rubroDe(nombre, tags)
            if (rubro == Rubro.OTROS) sinRubro += nombre else conteo[rubro] = (conteo[rubro] ?: 0) + 1
        }
        return Triple(productos.size, conteo, sinRubro)
    }

    @Test
    fun `la clasificacion cubre al menos el 95 por ciento del catalogo real`() {
        val (total, conteo, sinRubro) = cobertura()
        val cubiertos = total - sinRubro.size
        assertTrue(
            "cobertura ${cubiertos * 100 / total}% sobre $total productos; sin rubro: ${sinRubro.take(20)}",
            cubiertos.toDouble() / total >= 0.95,
        )
        assertTrue("esperaba >500 productos, $total", total > 500)
    }

    @Test
    fun `ningun rubro se come el catalogo`() {
        val (total, conteo, _) = cobertura()
        // Si una regla se vuelve demasiado amplia, el catalogo colapsa en un rubro y
        // el selector de filtros deja de servir. El mas grande medido es ~26%.
        val mayor = conteo.maxByOrNull { it.value }!!
        assertTrue(
            "'${mayor.key.etiqueta}' concentra ${mayor.value} de $total",
            mayor.value.toDouble() / total < 0.35,
        )
        assertTrue("esperaba al menos 6 rubros con contenido, ${conteo.size}", conteo.size >= 6)
    }

    @Test
    fun `casos congelados del catalogo real`() {
        // nombre, tags, rubro esperado
        val casos = listOf(
            Triple("Galleta Maria Puig", listOf("confiteria"), Rubro.DULCES_Y_SNACKS),
            Triple("ABONO LIQUIDO", listOf("verdura"), Rubro.FRUTAS_Y_VERDURAS),
            Triple("perrarina super can adulto 500gr", emptyList(), Rubro.MASCOTAS),
            Triple("Jabon Las Llaves 250gr", emptyList(), Rubro.LIMPIEZA_Y_ASEO),
            Triple("bombillo led 15w", emptyList(), Rubro.FERRETERIA),
            Triple("Gelatina Rolda ", listOf("gelatina", "aseo"), Rubro.DULCES_Y_SNACKS),
            Triple("Pan de Sandwich Integral", emptyList(), Rubro.PANADERIA),
            Triple("harina de maiz precocida", emptyList(), Rubro.DESPENSA),
        )
        casos.forEach { (nombre, tags, esperado) ->
            assertEquals("$nombre -> ${rubroDe(nombre, tags)}", esperado, rubroDe(nombre, tags))
        }
    }

    @Test
    fun `falsos positivos que la medicion encontro siguen corregidos`() {
        // Cada uno de estos estuvo mal en alguna version de las reglas, y la causa
        // siempre fue la misma: alternativas sin limite de palabra y sin plural.
        // "perro" metia el pan de hot dog en Mascotas; "tornillo" metia la pasta
        // corta en Ferreteria; "crema" metia la leche descremada en Bebidas y
        // "crema dental" fuera de Limpieza; "pasta" metia "pasta dental" en Despensa.
        val casos = listOf(
            Triple("Pan de Perros Caliente", emptyList(), Rubro.PANADERIA),
            Triple("Pasta Especial Corta Tornillo", emptyList(), Rubro.DESPENSA),
            Triple("Leche Descremada Purisima 1lt", emptyList(), Rubro.BEBIDAS_Y_LACTEOS),
            Triple("CREMA DENTAL KIDS COLGATE", emptyList(), Rubro.LIMPIEZA_Y_ASEO),
            Triple("LAVAPLATOS LAS LLAVES CREMA 250gr", emptyList(), Rubro.LIMPIEZA_Y_ASEO),
            Triple("Gelatina Sonrisa uva", emptyList(), Rubro.DULCES_Y_SNACKS),
            Triple("Refresco Glup Uva 2 Litro", emptyList(), Rubro.BEBIDAS_Y_LACTEOS),
            Triple("Tallarines la especial 500 gr", emptyList(), Rubro.DESPENSA),
            Triple("protectores diarios maksim", emptyList(), Rubro.LIMPIEZA_Y_ASEO),
            Triple("PANALES HUGGIES G", emptyList(), Rubro.LIMPIEZA_Y_ASEO),
        )
        casos.forEach { (nombre, tags, esperado) ->
            assertEquals("$nombre -> ${rubroDe(nombre, tags)}", esperado, rubroDe(nombre, tags))
        }
    }

    @Test
    fun `el tag amplio no manda sobre el nombre`() {
        // "viveres", "hogar", "aseo" y "secundario" son canastas o banderas operativas:
        // si mandaran, la limpieza y las bebidas caerian en Despensa (medido: 90% -> 67%).
        assertEquals(Rubro.LIMPIEZA_Y_ASEO, rubroDe("Jabon en polvo", listOf("viveres", "hogar")))
        assertEquals(Rubro.OTROS, rubroDe("Nombre sin vocabulario", listOf("viveres", "secundario", "prioridad", "upc")))
        // El tag especifico si resuelve lo que el nombre no dice (marcas sueltas).
        assertEquals(Rubro.DESPENSA, rubroDe("Rikesa", listOf("cereales")))
    }

    @Test
    fun `nombres en mayusculas se muestran en sentence case y los mixtos no se tocan`() {
        assertEquals("Abono liquido", formatearNombreProducto("ABONO LIQUIDO"))
        assertEquals("Aceite de oliva", formatearNombreProducto("ACEITE   DE OLIVA"))
        // Mixto: no se toca (no destrozamos marcas ni siglas).
        assertEquals("Galleta Maria Puig", formatearNombreProducto("Galleta Maria Puig"))
        assertEquals("aceite de oliva extra virgen capri 250 cm3", formatearNombreProducto("aceite de oliva extra virgen capri 250 cm3"))
        assertEquals("", formatearNombreProducto("   "))
    }
}
