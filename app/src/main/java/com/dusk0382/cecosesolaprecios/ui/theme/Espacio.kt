package com.dusk0382.cecosesolaprecios.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Única fuente de verdad de espaciado (DESIGN.md §5). Si un número de separación
 * aparece suelto en una pantalla, es un defecto: se lee aquí.
 *
 * Base 4 dp. El ritmo es el que da "orden" sin agregar bordes ni sombras.
 */
object Espacio {
    /** 2 dp: separación entre líneas pegadas (nombre y precio de una tarjeta). */
    val minimo = 2.dp
    /** 4 dp: entre elementos de un mismo grupo. */
    val xs = 4.dp
    /** 8 dp: dentro de una tarjeta / entre icono y texto. */
    val s = 8.dp
    /** 12 dp: separación entre tarjetas de la grilla. */
    val m = 12.dp
    /** 16 dp: margen lateral de pantalla. */
    val l = 16.dp
    /** 24 dp: entre secciones. */
    val xl = 24.dp
    /** 32 dp: antes del primer bloque o en estados vacíos. */
    val xxl = 32.dp

    /** Área mínima táctil recomendada (accesibilidad táctil). */
    val toqueMinimo = 48.dp
}

/**
 * Reserva inferior de la grilla del catálogo: deja pasar el último renglón por
 * encima del FAB del escáner (56 dp + 16 de margen + 12 de gap). Sin esto, el
 * FAB tapa el control de carrito de la última tarjeta.
 */
val reservaFab = 88.dp
