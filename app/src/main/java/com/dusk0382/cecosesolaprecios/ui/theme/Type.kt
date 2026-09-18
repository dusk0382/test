package com.dusk0382.cecosesolaprecios.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Rampa reducida y explícita (DESIGN.md §5). Usa la fuente del sistema: empaquetar
 * una fuente cuesta ~200 KB y trabajo de carga que en un Helio G25 no se paga.
 *
 * Dos disciplinas:
 * 1. Se usan los estilos **Emphasized** de M3 Expressive para títulos y etiquetas:
 *    dan peso y ancho sin inventar tamaños a mano.
 * 2. La rampa es corta: display * no se usa nunca, y los tamaños que aparecen en
 *    pantalla son a lo sumo tres por vista. Menos rampa = jerarquía legible.
 */
val Typography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineSmallEmphasized = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLargeEmphasized = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMediumEmphasized = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    // Etiquetas de sección: sentence case, nunca MAYÚSCULAS decorativas.
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLargeEmphasized = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

/**
 * Precio con cifras tabulares: en una grilla los precios quedan alineados por
 * dígito y se comparan de un vistazo. `tnum` no cambia el ancho del texto en
 * columnas de distinto largo, que es justo lo que hace ruidosa una lista de precios.
 */
val PrecioDetalle: TextStyle = Typography.headlineMedium.copy(fontFeatureSettings = "tnum")

/** Precio en tarjeta: 20 sp semi-bold con cifras tabulares. */
val PrecioTarjeta: TextStyle = Typography.titleLargeEmphasized.copy(
    fontSize = 20.sp,
    lineHeight = 26.sp,
    fontFeatureSettings = "tnum",
)

/** Cifra secundaria (total del carrito, precio por unidad) sin robar jerarquía. */
val PrecioApoyo: TextStyle = Typography.titleMediumEmphasized.copy(fontFeatureSettings = "tnum")
