package com.dusk0382.cecosesolaprecios

import com.dusk0382.cecosesolaprecios.ui.catalog.decidirScrollBusqueda
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * La política de scroll del catálogo cuando cambia la búsqueda asentada.
 *
 * El caso que la motivó, y que se pierde si alguien la simplifica: el usuario
 * está en el producto 60, busca, y al borrar la búsqueda tiene que volver al 60.
 * Borrar letra por letra no puede saltar a la posición de cada prefijo, y una
 * búsqueda restaurada después de morir el proceso no puede pisar el punto de
 * partida con la posición de los resultados.
 */
class ScrollBusquedaTest {

    @Test
    fun `empezar una busqueda guarda el punto de partida y va al inicio`() {
        val decision = decidirScrollBusqueda(
            consultaAnterior = "",
            consultaNueva = "leche",
            indiceActual = 60,
            posicionPrevia = null,
        )
        assertEquals(0L, decision.destino!!.toLong())
        assertEquals(60L, decision.posicionPrevia!!.toLong())
    }

    @Test
    fun `limpiar la busqueda vuelve al punto de partida`() {
        val decision = decidirScrollBusqueda(
            consultaAnterior = "leche",
            consultaNueva = "",
            indiceActual = 3,
            posicionPrevia = 60,
        )
        assertEquals(60L, decision.destino!!.toLong())
        assertNull(decision.posicionPrevia)
    }

    @Test
    fun `borrar una letra no mueve ni consume el punto de partida`() {
        val decision = decidirScrollBusqueda(
            consultaAnterior = "leche",
            consultaNueva = "lech",
            indiceActual = 8,
            posicionPrevia = 60,
        )
        assertNull(decision.destino)
        assertEquals(60L, decision.posicionPrevia!!.toLong())
    }

    @Test
    fun `una busqueda restaurada no se pisa con la posicion de los resultados`() {
        // Proceso muerto con una búsqueda activa: el ViewModel arranca en "" y la
        // consulta guardada vuelve después. La posición previa ya existe y no se
        // puede reemplazar por el índice de los resultados restaurados.
        val decision = decidirScrollBusqueda(
            consultaAnterior = "",
            consultaNueva = "leche",
            indiceActual = 12,
            posicionPrevia = 60,
        )
        assertNull(decision.destino)
        assertEquals(60L, decision.posicionPrevia!!.toLong())
    }

    @Test
    fun `limpiar sin punto de partida va al inicio`() {
        val decision = decidirScrollBusqueda(
            consultaAnterior = "leche",
            consultaNueva = "",
            indiceActual = 0,
            posicionPrevia = null,
        )
        assertEquals(0L, decision.destino!!.toLong())
        assertNull(decision.posicionPrevia)
    }

    @Test
    fun `la misma consulta no mueve nada`() {
        val decision = decidirScrollBusqueda(
            consultaAnterior = "leche",
            consultaNueva = "leche",
            indiceActual = 9,
            posicionPrevia = 60,
        )
        assertNull(decision.destino)
        assertEquals(60L, decision.posicionPrevia!!.toLong())
    }
}
