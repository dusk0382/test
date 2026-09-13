package com.dusk0382.cecosesolaprecios.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.dusk0382.cecosesolaprecios.data.local.CartLine
import com.dusk0382.cecosesolaprecios.data.local.ProductEntity

/**
 * ¿Mostrar los precios en USD (CEC) en vez de Bs?
 * Un solo valor para toda la app: se provee en MainActivity desde AppPrefs y lo
 * leen las tarjetas, el detalle y el carrito sin ir pasando parámetros.
 */
val LocalUsdPrecio = staticCompositionLocalOf { false }

/**
 * Precio a pintar + su etiqueta de moneda, según el toggle.
 * El CEC ("precio solidario") solo existe cuando la API oficial ya enriqueció:
 * si no, siempre Bs — nunca se inventa una conversión.
 */
@Composable
fun ProductEntity.precioMostrado(): Pair<Double, String> {
    val usd = LocalUsdPrecio.current
    val cec = precioCec
    return if (usd && cec != null) cec to "USD" else precioBs to "Bs"
}

/**
 * Igual que [precioMostrado] pero para una línea del carrito.
 * Misma regla: sin precio CEC guardado, se muestra Bs — nunca una conversión
 * calculada al vuelo con la tasa del día (el total no cuadraría con la feria).
 */
fun CartLine.precioMostrado(usd: Boolean): Pair<Double, String> {
    val cec = precioCec
    return if (usd && cec != null) cec to "USD" else precioBs to "Bs"
}

/** Importe de la línea (precio unitario × cantidad) ya formateado con su moneda. */
fun CartLine.importeLinea(usd: Boolean, cantidad: Int = quantity): String {
    val (precio, moneda) = precioMostrado(usd)
    return "$moneda ${formatBs(precio * cantidad)}"
}
