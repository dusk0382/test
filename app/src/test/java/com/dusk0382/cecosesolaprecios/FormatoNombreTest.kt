package com.dusk0382.cecosesolaprecios

import com.dusk0382.cecosesolaprecios.domain.formatearNombreProducto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Puerta de formato de nombres (DESIGN.md §6.3).
 *
 * El catálogo viene sucio: MAYÚSCULAS mezcladas con minúsculas, y la UI no
 * inventa. La regla es de una sola dirección — un nombre **enteramente** en
 * mayúsculas pasa a sentence case, uno que ya viene en mixta no se toca. Lo
 * segundo importa tanto como lo primero: "destrozar" una marca ("Rikesa" →
 * "Rikesa" está bien, pero "OSITO" → "Osito" también) es un error distinto.
 *
 * Este test antes solo se cubría de ref incidental dentro de `RubrosTest`. Con el
 * formateador aplicado hoy en tres pantallas (tarjeta, detalle y carrito) un
 * cambio suyo se ve en todas, así que deja de ser un detalle de la clasificación.
 */
class FormatoNombreTest {

    @Test
    fun `un nombre enteramente en mayusculas pasa a sentence case`() {
        assertEquals("Abono liquido", formatearNombreProducto("ABONO LIQUIDO"))
        assertEquals("Jabón de baño dalan", formatearNombreProducto("JABÓN DE BAÑO DALAN"))
        assertEquals("Gelatina sonrisa", formatearNombreProducto("GELATINA SONRISA"))
    }

    @Test
    fun `un nombre que ya viene en mixta no se toca`() {
        // Si se destrozaran, "PREMIUM" dentro de un nombre mixto se volvería
        // "Premium" y la marca deja de ser la marca.
        assertEquals("Aceite de oliva_extra virgen", formatearNombreProducto("Aceite de oliva_extra virgen"))
        assertEquals("Leche PREMIUM", formatearNombreProducto("Leche PREMIUM"))
        assertEquals("Café 100%", formatearNombreProducto("Café 100%"))
    }

    @Test
    fun `colapsa espacios y recorta`() {
        assertEquals("Mayonesa mavesa", formatearNombreProducto("  MAYONESA   MAVESA  "))
    }

    @Test
    fun `no rompe los nombres con numeros ni simbolos`() {
        // "250GR" y "1LT" son parte del nombre real del catálogo: el formateador
        // solo mira letras para decidir si TODO era mayúscula, así que un nombre
        // con dígitos sigue siendo mayúscula y se convierte igual.
        assertEquals("Galleta maria puig 250gr", formatearNombreProducto("GALLETA MARIA PUIG 250GR"))
        // Ojo con la expectativa: sale "higienico" sin tilde, y es lo correcto. La
        // fuente trae "HIGIENICO" sin acento, así que no hay información para
        // recuperarlo — inventar la tilde sería mostrar algo que el dato no dice.
        assertEquals("Papel higienico 1lt", formatearNombreProducto("PAPEL HIGIENICO 1LT"))
    }

    @Test
    fun `un nombre sin letras no revienta`() {
        assertEquals("250", formatearNombreProducto("250"))
        assertEquals("", formatearNombreProducto(""))
    }

    /**
     * El formateador se aplica en las TRES pantallas que muestran un producto.
     * Antes el detalle y el carrito pintaban el nombre crudo, así que un producto
     * en MAYÚSCULAS se veía en minúsculas en la grilla y en mayúsculas en su
     * ficha: el mismo producto con dos nombres.
     */
    @Test
    fun `es idempotente`() {
        val crudo = "ARROZ BLANCO TIPO A"
        val una = formatearNombreProducto(crudo)
        assertEquals("Arroz blanco tipo a", una)
        assertEquals(una, formatearNombreProducto(una))
    }

    @Test
    fun `la salida nunca queda vacia si la entrada no lo estaba`() {
        listOf("QUESO BLANCO", "sal", "Leche", "123", "A").forEach { entrada ->
            assertTrue(
                "formatearNombreProducto(\"$entrada\") no debe devolver vacío",
                formatearNombreProducto(entrada).isNotEmpty(),
            )
        }
    }
}
