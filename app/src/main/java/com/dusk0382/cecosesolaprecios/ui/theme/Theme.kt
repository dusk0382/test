package com.dusk0382.cecosesolaprecios.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Esquemas **completos**. Esto no es relleno: la versión anterior definía sólo
 * nueve roles, así que todo lo demás (`secondary`, `tertiary`, `secondaryContainer`,
 * `surfaceContainerHighest`, `outline`…) caía al baseline lila/violeta de Material3.
 * Ése fue el origen del buscador rosado: el color por defecto del generador, no una
 * decisión. Aquí cada rol que puede aparecer en pantalla está definido y medido.
 */
private val EsquemaClaro = lightColorScheme(
    primary = Color(Paleta.NaranjaMarca),
    onPrimary = Color(Paleta.TintaSobreMarca),
    primaryContainer = Color(Paleta.MarcaContenedorClaro),
    onPrimaryContainer = Color(Paleta.SobreMarcaContenedorClaro),

    secondary = Color(Paleta.SecundarioClaro),
    onSecondary = Color(Paleta.SobreSecundarioClaro),
    secondaryContainer = Color(Paleta.SecundarioContenedorClaro),
    onSecondaryContainer = Color(Paleta.SobreSecundarioContenedorClaro),
    tertiary = Color(Paleta.TerciarioClaro),
    onTertiary = Color(Paleta.SobreTerciarioClaro),
    tertiaryContainer = Color(Paleta.TerciarioContenedorClaro),
    onTertiaryContainer = Color(Paleta.SobreTerciarioContenedorClaro),

    background = Color(Paleta.FondoClaro),
    onBackground = Color(Paleta.SobreSuperficieClaro),
    surface = Color(Paleta.SuperficieClaro),
    onSurface = Color(Paleta.SobreSuperficieClaro),
    surfaceVariant = Color(Paleta.SuperficieVarianteClaro),
    onSurfaceVariant = Color(Paleta.SobreSuperficieVarianteClaro),
    surfaceContainerLow = Color(Paleta.SuperficieClaro),
    surfaceContainer = Color(Paleta.SuperficieAltaClaro),
    surfaceContainerHigh = Color(Paleta.SuperficieAltaClaro),
    surfaceContainerHighest = Color(Paleta.SuperficieMaximaClaro),
    outline = Color(Paleta.ContornoClaro),
    outlineVariant = Color(Paleta.ContornoVarianteClaro),
    scrim = Color(Paleta.Scrim),

    error = Color(Paleta.ErrorClaro),
    errorContainer = Color(Paleta.ErrorContenedorClaro),
    onErrorContainer = Color(Paleta.SobreErrorContenedorClaro),
)

private val EsquemaOscuro = darkColorScheme(
    primary = Color(Paleta.AcentoPrecioOscuro),
    onPrimary = Color(Paleta.TintaSobreMarca),
    primaryContainer = Color(Paleta.MarcaContenedorOscuro),
    onPrimaryContainer = Color(Paleta.SobreMarcaContenedorOscuro),

    secondary = Color(Paleta.SecundarioOscuro),
    onSecondary = Color(Paleta.SobreSecundarioOscuro),
    secondaryContainer = Color(Paleta.SecundarioContenedorOscuro),
    onSecondaryContainer = Color(Paleta.SobreSecundarioContenedorOscuro),
    tertiary = Color(Paleta.TerciarioOscuro),
    onTertiary = Color(Paleta.SobreTerciarioOscuro),
    tertiaryContainer = Color(Paleta.TerciarioContenedorOscuro),
    onTertiaryContainer = Color(Paleta.SobreTerciarioContenedorOscuro),

    background = Color(Paleta.FondoOscuro),
    onBackground = Color(Paleta.SobreSuperficieOscuro),
    surface = Color(Paleta.SuperficieOscura),
    onSurface = Color(Paleta.SobreSuperficieOscuro),
    surfaceVariant = Color(Paleta.SuperficieVarianteOscura),
    onSurfaceVariant = Color(Paleta.SobreSuperficieVarianteOscuro),
    surfaceContainerLow = Color(Paleta.SuperficieOscura),
    surfaceContainer = Color(Paleta.SuperficieAltaOscura),
    surfaceContainerHigh = Color(Paleta.SuperficieAltaOscura),
    surfaceContainerHighest = Color(Paleta.SuperficieMaximaOscura),
    outline = Color(Paleta.ContornoOscuro),
    outlineVariant = Color(Paleta.ContornoVarianteOscuro),
    scrim = Color(Paleta.Scrim),

    error = Color(Paleta.ErrorOscuro),
    errorContainer = Color(Paleta.ErrorContenedorOscuro),
    onErrorContainer = Color(Paleta.SobreErrorContenedorOscuro),
)

/**
 * Roles que Material3 no tiene y que este producto necesita: el acento de precio
 * (que no puede ser `primary` porque `primary` es relleno y falla AA como texto) y
 * la semántica de variación, siempre CEC contra CEC.
 *
 * Se expone por CompositionLocal en vez de por `MaterialTheme.colorScheme` para no
 * fingir que son roles del sistema.
 */
@Immutable
data class ColoresPrecio(
    val acento: Color,
    val sube: Color,
    val baja: Color,
)

private val ColoresPrecioClaro = ColoresPrecio(
    acento = Color(Paleta.AcentoPrecioClaro),
    sube = Color(Paleta.SubeClaro),
    baja = Color(Paleta.BajaClaro),
)

private val ColoresPrecioOscuro = ColoresPrecio(
    acento = Color(Paleta.AcentoPrecioOscuro),
    sube = Color(Paleta.SubeOscuro),
    baja = Color(Paleta.BajaOscuro),
)

val LocalColoresPrecio = staticCompositionLocalOf { ColoresPrecioClaro }

/**
 * Color dinámico OFF a propósito: paleta fija = predecible, más barato de resolver
 * y consistente en dispositivos Go (Helio G25) que ni soportan Material You.
 */
@Composable
fun CecosesolaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            // Ambas barras: sin esto, en tema claro los iconos de la barra de
            // navegación quedaban blancos (ilegibles) — DESIGN.md §8.5.
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
            // Evita el scrim translúcido del sistema sobre nuestra NavigationBar:
            // el color del tema debe llegar hasta el borde real de la pantalla.
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }
    CompositionLocalProvider(
        LocalColoresPrecio provides if (darkTheme) ColoresPrecioOscuro else ColoresPrecioClaro,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
            shapes = Formas,
            typography = Typography,
            content = content,
        )
    }
}
