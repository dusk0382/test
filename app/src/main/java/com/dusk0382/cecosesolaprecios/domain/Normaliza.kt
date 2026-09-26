package com.dusk0382.cecosesolaprecios.domain

import java.text.Normalizer

// A nivel de archivo: normalizarNombre corre por producto en cada sync (2×527)
// y por cada tecleo debounced. Compilar dos Regex por llamada es trabajo
// repetido en la ruta más caliente del merging.
private val RE_MARCAS = Regex("\\p{InCombiningDiacriticalMarks}+")
private val RE_ESPACIOS = Regex("\\s+")

/**
 * Clave de matching entre fuentes y de búsqueda: minúsculas, sin acentos,
 * espacios colapsados. Estable entre precios.json y el GraphQL oficial.
 */
fun normalizarNombre(s: String): String {
    val sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace(RE_MARCAS, "")
    return sinAcentos.lowercase().replace(RE_ESPACIOS, " ").trim()
}
