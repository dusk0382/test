package com.dusk0382.cecosesolaprecios.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.dusk0382.cecosesolaprecios.data.local.CartLine
import com.dusk0382.cecosesolaprecios.data.local.ProductEntity

/**
 * ¿Mostrar los precios en USD (CEC) en vez de Bs?
 * Un solo valor para toda la app: se provee en MainActivity desde AppPrefs y lo
 * leen las tarjetas, el detalle y el carrito sin ir pasando parámetros.
 *
 * `compositionLocalOf` y no `staticCompositionLocalOf`: el usuario puede cambiar
 * este toggle en Ajustes, y con la versión `static` el cambio de valor
 * recomponía **toda** la app (catálogo, detalle y carrito). La versión normal
 * solo invalida a quienes leen el valor: las 6 tarjetas visibles.
 */
val LocalUsdPrecio = compositionLocalOf { false }

/**
 * Precio a pintar + su etiqueta de moneda, según el toggle.
 * El CEC ("precio solidario") solo existe cuando la API oficial ya enriqueció:
 * si no, siempre Bs — nunca se inventa una conversión.
 */
@Composable
fun ProductEntity.precioMostrado(): Pair<Double, String> {
    val cecActivado = LocalUsdPrecio.current
    val cec = precioCec
    // La etiqueta dice **CEC**, no "USD". El número es el precio solidario en
    // unidades CEC, que no son dólares: el propio payload lo dice
    // (`priceBase.currencyCode == "CEC"`) y la tasa de Ajustes dice cuántos Bs
    // vale una unidad. Ponerle "USD" era mentirle al usuario dos veces: la
    // etiqueta, y el total del carrito que suma esos mismos números. Con el
    // nombre correcto, "CEC 1,68" se lee como lo que es.
    return if (cecActivado && cec != null) cec to "CEC" else precioBs to "Bs"
}

/**
 * Igual que [precioMostrado] pero para una línea del carrito.
 * Misma regla: sin precio CEC guardado, se muestra Bs — nunca una conversión
 * calculada al vuelo con la tasa del día (el total no cuadraría con la feria).
 */
fun CartLine.precioMostrado(cecActivado: Boolean): Pair<Double, String> {
    val cec = precioCec
    return if (cecActivado && cec != null) cec to "CEC" else precioBs to "Bs"
}

/**
 * Importe de la línea (unitario × cantidad) como valor + moneda, listo para
 * [PriceText]. Devuelve el número, no el texto: el formateo y el color los pone
 * el componente, para que no haya dos lugares que pinten un precio.
 */
fun CartLine.importeLinea(cecActivado: Boolean): Pair<Double, String> {
    val (precio, moneda) = precioMostrado(cecActivado)
    return precio * quantity to moneda
}
