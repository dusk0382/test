package com.dusk0382.cecosesolaprecios.domain

import java.text.Normalizer

/**
 * Clave de matching entre fuentes y de búsqueda: minúsculas, sin acentos,
 * espacios colapsados. Estable entre precios.json y el GraphQL oficial.
 */
fun normalizarNombre(s: String): String {
    val sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    return sinAcentos.lowercase().replace(Regex("\\s+"), " ").trim()
}
