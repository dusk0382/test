package com.dusk0382.cecosesolaprecios

import com.dusk0382.cecosesolaprecios.ui.common.formatBs
import com.dusk0382.cecosesolaprecios.ui.common.formatearFechaIso
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Puertas de formato de precios y fechas.
 *
 * `formatBs` es el único formateador de dinero de la app: si cambia, cambia en
 * catálogo, ficha y carrito a la vez. El formato es fijo (punto de miles, coma
 * decimal) y **siempre** con dos decimales, sin mirar el locale del SO.
 */
class FormatTest {

    @Test
    fun `formatBs usa separador es-VE y siempre dos decimales`() {
        assertEquals("1.365,00", formatBs(1365.0))
        assertEquals("0,50", formatBs(0.5))
        assertEquals("0,00", formatBs(0.0))
    }

    /**
     * El `updatedAt` de la API viene en UTC ("2026-09-12T21:24:27.906Z") y la
     * ficha lo muestra en hora local, con el mismo formato que Ajustes.
     *
     * El huso se fija a UTC para que el resultado no dependa del CI. El
     * `SimpleDateFormat` de `fechaFormat` es un ThreadLocal que toma el huso al
     * nacer: si otro test lo inicializó antes con el huso real, la hora cambia,
     * y entonces se verifica lo que vale en cualquier huso — la fecha formateada
     * y que ya no sea el ISO crudo.
     */
    @Test
    fun `formatearFechaIso pasa el ISO UTC a hora local`() {
        val husoOriginal = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val iso = "2026-09-12T21:24:27.906Z"
            val resultado = formatearFechaIso(iso)
            if (resultado != "12/09/2026 a las 21:24") {
                assertTrue(
                    "formatearFechaIso(\"$iso\") devolvió \"$resultado\"",
                    resultado.contains("12/09/2026"),
                )
                assertNotEquals(iso, resultado)
            }
        } finally {
            TimeZone.setDefault(husoOriginal)
        }
    }

    @Test
    fun `formatearFechaIso devuelve el texto crudo si no parsea`() {
        assertEquals("sin fecha", formatearFechaIso("sin fecha"))
    }
}
