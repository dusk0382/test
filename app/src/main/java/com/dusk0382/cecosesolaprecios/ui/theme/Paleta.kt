package com.dusk0382.cecosesolaprecios.ui.theme

/**
 * Paleta explícita en hex puro y sin dependencias de Compose.
 *
 * Deliberadamente separada de `Color.kt`: así `TemaContrasteTest` puede calcular
 * los ratios WCAG en la JVM sin arrastrar `androidx.compose.ui.graphics.Color`.
 *
 * Cada valor está donde está por una medición, no por gusto (ver DESIGN.md §3 y §5):
 *
 * - El naranja de marca (#FD4902) es un color de **relleno**, no de texto: blanco
 *   sobre él da 3.43:1 y **falla** AA para texto normal; por eso el texto de los
 *   botones primarios es tinta oscura (#2A1200, 5.17:1).
 * - El precio en tarjeta usa un naranja oscuro (#B93300, 5.66:1 sobre superficie)
 *   en claro y uno claro (#FFB59B, 8.14:1) en oscuro.
 * - Los roles secundario/terciario estaban **sin definir**, así que caían al
 *   baseline lila/violeta de Material3: de ahí el buscador rosado de la versión
 *   anterior. Ahora son neutros cálidos de la misma familia que la marca.
 */
internal object Paleta {

    // — marca —
    const val NaranjaMarca = 0xFFFD4902L      // relleno de acción primaria
    const val TintaSobreMarca = 0xFF2A1200L   // 5.17:1 sobre NaranjaMarca
    const val MarcaContenedorClaro = 0xFFFFDBCFL
    const val SobreMarcaContenedorClaro = 0xFF3D0800L  // 13.21:1 sobre el contenedor
    const val AcentoPrecioClaro = 0xFFB93300L // 5.66:1 sobre superficie clara
    const val AcentoPrecioOscuro = 0xFFFFB59BL // 10.07:1 sobre superficie oscura

    // — superficies claras —
    const val FondoClaro = 0xFFFFF8F6L
    const val SuperficieClaro = 0xFFFFF8F6L
    const val SuperficieAltaClaro = 0xFFF7EDE9L
    const val SuperficieMaximaClaro = 0xFFF0E4DFL
    const val SobreSuperficieClaro = 0xFF241A17L     // 16.20:1
    const val SobreSuperficieVarianteClaro = 0xFF53433FL // 8.92:1
    const val SuperficieVarianteClaro = 0xFFF5DED6L
    const val ContornoClaro = 0xFF8A726BL     // 4.26:1 sobre superficie (bordes, UI 3:1)
    const val ContornoVarianteClaro = 0xFFDCC2B9L
    const val Scrim = 0xFF000000L

    // — secundario / terciario cálidos (evitan el lila por defecto) —
    const val SecundarioClaro = 0xFF5C4A44L   // 7.95:1
    const val SobreSecundarioClaro = 0xFFFFFFFFL
    const val SecundarioContenedorClaro = 0xFFF0DCD4L
    const val SobreSecundarioContenedorClaro = 0xFF3A2A25L // 10.33:1
    const val TerciarioClaro = 0xFF5A4A3FL
    const val SobreTerciarioClaro = 0xFFFFFFFFL
    const val TerciarioContenedorClaro = 0xFFEFE0D9L
    const val SobreTerciarioContenedorClaro = 0xFF3E2E28L

    // — superficies oscuras —
    const val FondoOscuro = 0xFF211A17L
    const val SuperficieOscura = 0xFF211A17L
    const val SuperficieAltaOscura = 0xFF2B211DL
    const val SuperficieMaximaOscura = 0xFF362A25L
    const val SobreSuperficieOscuro = 0xFFEDE2DEL    // 13.50:1
    const val SobreSuperficieVarianteOscuro = 0xFFCBBDB8L // 9.40:1
    const val SuperficieVarianteOscura = 0xFF53433FL
    const val ContornoOscuro = 0xFF9E8880L
    const val ContornoVarianteOscuro = 0xFF53433FL
    const val MarcaContenedorOscuro = 0xFF75270AL
    const val SobreMarcaContenedorOscuro = 0xFFFFDBCFL
    const val SecundarioOscuro = 0xFFE8D5CDL
    const val SobreSecundarioOscuro = 0xFF2A211EL   // 11.11:1
    const val SecundarioContenedorOscuro = 0xFF3A2A25L
    const val SobreSecundarioContenedorOscuro = 0xFFF0DCD4L // 10.33:1
    const val TerciarioOscuro = 0xFFD8C6BCL
    const val SobreTerciarioOscuro = 0xFF2A211EL
    const val TerciarioContenedorOscuro = 0xFF3E2E28L
    const val SobreTerciarioContenedorOscuro = 0xFFEFE0D9L

    // — semántica de variación de precio (siempre CEC contra CEC) —
    const val SubeClaro = 0xFFBA1A1AL         // 6.46:1
    const val BajaClaro = 0xFF1B6C34L         // 6.48:1
    const val SubeOscuro = 0xFFFFB4ABL        // 10.10:1
    const val BajaOscuro = 0xFF7FD69AL        // 9.79:1

    // — error —
    const val ErrorClaro = 0xFFBA1A1AL
    const val ErrorContenedorClaro = 0xFFFFDAD6L
    const val SobreErrorContenedorClaro = 0xFF410002L // 13.26:1
    const val ErrorOscuro = 0xFFFFB4ABL
    const val ErrorContenedorOscuro = 0xFF93000AL
    const val SobreErrorContenedorOscuro = 0xFFFFDAD6L // 7.24:1
}

/** Ratio de contraste WCAG 2.1 entre dos colores ARGB opacos. */
internal fun ratioContraste(a: Long, b: Long): Double {
    val la = luminanciaRelativa(a)
    val lb = luminanciaRelativa(b)
    val (claro, oscuro) = if (la >= lb) la to lb else lb to la
    return (claro + 0.05) / (oscuro + 0.05)
}

private fun luminanciaRelativa(argb: Long): Double {
    fun canal(v: Long): Double {
        val c = ((argb shr v) and 0xFFL) / 255.0
        return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * canal(16) + 0.7152 * canal(8) + 0.0722 * canal(0)
}
