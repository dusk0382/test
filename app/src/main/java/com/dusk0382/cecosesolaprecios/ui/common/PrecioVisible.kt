package com.dusk0382.cecosesolaprecios.ui.common

import com.dusk0382.cecosesolaprecios.data.local.CartLine
import com.dusk0382.cecosesolaprecios.data.local.ProductEntity

/**
 * Precio a pintar + etiqueta de moneda.
 * Se muestra en USD cuando la API oficial ya enriqueció el producto (precioCec);
 * si no hay dato en dólares, se muestra el precio en bolívares como respaldo.
 * Sin toggle ni conmutación: el precio es el precio.
 */
fun ProductEntity.precioMostrado(): Pair<Double, String> {
    val cec = precioCec
    return if (cec != null) cec to "USD" else precioBs to "Bs"
}

/**
 * Igual que [precioMostrado] pero para una línea del carrito.
 */
fun CartLine.precioMostrado(): Pair<Double, String> {
    val cec = precioCec
    return if (cec != null) cec to "USD" else precioBs to "Bs"
}

/**
 * Importe de la línea (unitario × cantidad) como valor + moneda.
 */
fun CartLine.importeLinea(): Pair<Double, String> {
    val (precio, moneda) = precioMostrado()
    return precio * quantity to moneda
}
