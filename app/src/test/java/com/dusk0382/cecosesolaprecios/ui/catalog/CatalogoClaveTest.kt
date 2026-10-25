package com.dusk0382.cecosesolaprecios.ui.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Contrato de [claveCatalogo]. La clave decide si la lista que está en pantalla
 * corresponde a la consulta, los filtros y el orden actuales; sin estabilidad,
 * el scroll se restauraría contra la lista anterior.
 *
 * La función no normaliza acentos a propósito. La consulta SQL sí lo hace
 * (`consultaDeBusqueda` aplica `normalizarNombre`), pero la clave identifica el
 * estado de la UI —lo que el usuario escribió—, no el resultado de Room.
 */
class CatalogoClaveTest {

    @Test
    fun `misma consulta filtros y orden producen la misma clave`() {
        val una = claveCatalogo("leche", setOf("Despensa", "Lácteos"), Orden.PRECIO_ASC)
        val otra = claveCatalogo("leche", setOf("Despensa", "Lácteos"), Orden.PRECIO_ASC)
        assertEquals(una, otra)
    }

    @Test
    fun `el orden de insercion del set de clases no altera la clave`() {
        val una = claveCatalogo("leche", setOf("Despensa", "Lácteos", "Bebidas"), Orden.NOMBRE)
        val otra = claveCatalogo("leche", setOf("Bebidas", "Despensa", "Lácteos"), Orden.NOMBRE)
        assertEquals(una, otra)
    }

    @Test
    fun `cambiar el orden cambia la clave`() {
        val nombre = claveCatalogo("leche", setOf("Despensa"), Orden.NOMBRE)
        val ascendente = claveCatalogo("leche", setOf("Despensa"), Orden.PRECIO_ASC)
        val descendente = claveCatalogo("leche", setOf("Despensa"), Orden.PRECIO_DESC)

        assertNotEquals(nombre, ascendente)
        assertNotEquals(nombre, descendente)
        assertNotEquals(ascendente, descendente)
    }

    @Test
    fun `la consulta se recorta y se compara en minusculas`() {
        val limpia = claveCatalogo("limón", emptySet(), Orden.NOMBRE)
        assertEquals(limpia, claveCatalogo(" Limón ", emptySet(), Orden.NOMBRE))
        assertEquals(limpia, claveCatalogo("LIMÓN", emptySet(), Orden.NOMBRE))
    }

    @Test
    fun `el acento distingue la clave aunque la consulta SQL lo ignore`() {
        // `claveCatalogo` solo hace trim + lowercase. La normalización de acentos
        // ocurre más abajo, en la consulta SQL (`consultaDeBusqueda` →
        // `normalizarNombre`), así que "limón" y "limon" traen el mismo listado
        // pero producen claves distintas. Es intencional: la clave representa el
        // estado de la UI, no el conjunto de resultados de Room.
        assertNotEquals(
            claveCatalogo("limón", emptySet(), Orden.NOMBRE),
            claveCatalogo("limon", emptySet(), Orden.NOMBRE),
        )
    }

    @Test
    fun `la clave vacia del estado inicial no es un estado real y es estable`() {
        // `ResultadosCatalogo` arranca con clave = "" mientras Room no respondió.
        val inicial = ""
        val estadoVacio = claveCatalogo("", emptySet(), Orden.NOMBRE)

        assertNotEquals(inicial, estadoVacio)
        assertNotEquals(estadoVacio, claveCatalogo("leche", emptySet(), Orden.NOMBRE))
        // Estable entre llamadas: dos emisiones del mismo estado no mueven el scroll.
        assertEquals(estadoVacio, claveCatalogo("", emptySet(), Orden.NOMBRE))
    }
}
