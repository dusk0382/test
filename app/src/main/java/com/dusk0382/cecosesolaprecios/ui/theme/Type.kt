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
 * Nota medida en CI: los estilos **Emphasized** de M3 Expressive (`titleLargeEmphasized`
 * y compañía) son **internal** en material3 1.4.0, igual que `MotionScheme`. Se ven en
 * el bytecode con javap, pero el compilador de Kotlin los rechaza. Hasta que se pueda
 * subir a una línea de Compose que exige AGP 9, la «expresividad» de la tipografía se
 * hace con escala, peso y tracking propios: la rampa es corta (no se usa display *),
 * la jerarquía es tamaño + peso, y en pantalla no aparecen más de tres tamaños.
 */
val Typography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
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
    labelLarge = TextStyle(
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
val PrecioTarjeta: TextStyle = Typography.titleLarge.copy(
    fontSize = 20.sp,
    lineHeight = 26.sp,
    fontFeatureSettings = "tnum",
)

/** Cifra secundaria (total del carrito, precio por unidad) sin robar jerarquía. */
val PrecioApoyo: TextStyle = Typography.titleMedium.copy(fontFeatureSettings = "tnum")
