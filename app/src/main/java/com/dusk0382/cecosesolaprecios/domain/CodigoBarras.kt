package com.dusk0382.cecosesolaprecios.domain

/**
 * Un código de barras real para esta app: EAN-8, UPC-A, EAN-13 o GTIN-14.
 *
 * La API oficial usa `0000000000000000000000` como relleno cuando un producto
 * no tiene código, y también hay códigos internos de 7 dígitos que no son EAN.
 * Ninguno sirve como identidad: si se aceptan, dos productos distintos pueden
 * matchear por el mismo "código" y la fila queda mezclada.
 */
fun esBarcodeValido(valor: String?): Boolean {
    val limpio = valor?.trim().orEmpty()
    return limpio.length in 8..14 &&
        limpio.all { it.isDigit() } &&
        limpio.any { it != '0' }
}
