/**
 * Precio a pintar: siempre en USD (dólares), usando el precio CEC de la API.
 * El CEC es el código interno con el que la API nombra el precio solidario,
 * y su propio officialRate dice `base: "CEC"` con `forSales: [{USD, 1}, {VED, 832.49}]`,
 * o sea **1 CEC = 1 USD** y 832,49 son los bolívares de un dólar.
 *
 * Los nombres de campo (`precioCec`, `precioAnteriorCec`) se dejan como están
 * porque describen lo que trae el origen, que es lo que hay que preservar.
 *
 * SIEMPRE se muestra USD: sin toggle, sin conmutación, sin "solidario" como etiqueta
 * separada. El precio es el precio.
 */
val LocalUsdPrecio = compositionLocalOf { true } // Siempre activado: precio en USD

/**
 * Precio a pintar + su etiqueta de moneda, SIEMPRE en USD.
 * El CEC ("precio solidario") solo existe cuando la API oficial ya enriqueció:
 * si no, siempre mostramos el precio BS con aviso, nunca una conversión inventada.
 */
@Composable
@ReadOnlyComposable
fun ProductEntity.precioMostrado(): Pair<Double, String> {
    val cec = precioCec
    // SIEMPRE USD: si hay CEC, mostraremos USD; si no hay CEC, mostraremos BS con aviso
    return if (cec != null) cec to "USD" else precioBs to "Bs (sin enriquecer)"
}

/**
 * Igual que [precioMostrado] pero para una línea del carrito.
 * Misma regla: sin precio CEC guardado, se muestra BS — nunca una conversión
 * calculada al vuelo con la tasa del día.
 */
fun CartLine.precioMostrado(): Pair<Double, String> {
    val cec = precioCec
    return if (cec != null) cec to "USD" else precioBs to "Bs (sin enriquecer)"
}

/**
 * Importe de la línea (unitario × cantidad) como valor + moneda, listo para
 * [PriceText]. Devuelve el número, no el texto: el formateo y el color los pone
 * el componente, para que no haya dos lugares que pinten un precio.
 */
fun CartLine.importeLinea(): Pair<Double, String> {
    val (precio, moneda) = precioMostrado()
    return precio * quantity to moneda
}