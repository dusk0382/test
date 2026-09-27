package com.dusk0382.cecosesolaprecios.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.dusk0382.cecosesolaprecios.data.local.CartLine
import com.dusk0382.cecosesolaprecios.data.local.ProductEntity

/**
 * ¿Mostrar los precios en USD (el precio solidario de la API, que llama CEC) en
 * vez de Bs?
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
    // Se muestra como **USD**. "CEC" es el código interno con el que la API
    // nombra el precio solidario, y no es una moneda que el usuario conozca: en
    // la app no aparece nunca. Que el payload lo llame CEC no lo convierte en
    // otra cosa — su propio `officialRate` dice `base: "CEC"` con
    // `forSales: [{USD, 1}, {VED, 832.49}]`, o sea **1 CEC = 1 USD** y 832,49
    // son justamente los Bs de un dólar.
    //
    // Los nombres de campo (`precioCec`, `precioAnteriorCec`) se dejan como
    // están porque describen lo que trae el origen, que es lo que hay que
    // preservar para no perder trazabilidad con la API.
    return if (cecActivado && cec != null) cec to "USD" else precioBs to "Bs"
}

/**
 * Igual que [precioMostrado] pero para una línea del carrito.
 * Misma regla: sin precio CEC guardado, se muestra Bs — nunca una conversión
 * calculada al vuelo con la tasa del día (el total no cuadraría con la feria).
 */
fun CartLine.precioMostrado(cecActivado: Boolean): Pair<Double, String> {
    val cec = precioCec
    return if (cecActivado && cec != null) cec to "USD" else precioBs to "Bs"
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
