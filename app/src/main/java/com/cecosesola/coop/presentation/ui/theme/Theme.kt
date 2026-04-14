package com.cecosesola.coop.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Paleta Material You 3 generada desde seed #8B5E3C (terracota ámbar).
 * Tonos calculados con el algoritmo HCT de Material Color Utilities.
 */
object Palette {
    val Primary10 = Color(0xFF311300)
    val Primary20 = Color(0xFF4E2300)
    val Primary30 = Color(0xFF6E3A1E)
    val Primary40 = Color(0xFF8B5E3C)
    val Primary80 = Color(0xFFFFBA8E)
    val Primary90 = Color(0xFFFFDCC4)
    val Primary95 = Color(0xFFFFEDE2)
    val Primary99 = Color(0xFFFFFBFF)

    val Secondary10 = Color(0xFF2C160A)
    val Secondary20 = Color(0xFF432B1C)
    val Secondary30 = Color(0xFF5C4031)
    val Secondary40 = Color(0xFF765848)
    val Secondary80 = Color(0xFFE7BEA9)
    val Secondary90 = Color(0xFFFFDCC4)

    val Tertiary10 = Color(0xFF1E1C00)
    val Tertiary20 = Color(0xFF343108)
    val Tertiary30 = Color(0xFF4B4820)
    val Tertiary40 = Color(0xFF636032)
    val Tertiary80 = Color(0xFFCEC97E)
    val Tertiary90 = Color(0xFFEAE5AA)

    val Error10 = Color(0xFF410002)
    val Error40 = Color(0xFFBA1A1A)
    val Error80 = Color(0xFFFFB4AB)
    val Error90 = Color(0xFFFFDAD6)

    val Neutral10 = Color(0xFF211A16)
    val Neutral20 = Color(0xFF382E28)
    val Neutral80 = Color(0xFFD8C2BC)
    val Neutral90 = Color(0xFFF5DDD8)
    val Neutral95 = Color(0xFFFEEDE8)
    val Neutral99 = Color(0xFFFFF8F5)

    val NeutralVar20 = Color(0xFF392F28)
    val NeutralVar30 = Color(0xFF51443C)
    val NeutralVar40 = Color(0xFF6A5C53)
    val NeutralVar50 = Color(0xFF84746B)
    val NeutralVar60 = Color(0xFF9F8D84)
    val NeutralVar80 = Color(0xFFD4C3B9)
    val NeutralVar90 = Color(0xFFF2E0D5)
}

// Tokens de superficie con tonal elevation M3
object Surf {
    // Light mode (primary tint sobre neutral99)
    val L0 = Palette.Neutral99
    val L1 = Color(0xFFFBEEE7)
    val L2 = Color(0xFFF8E5DB)
    val L3 = Color(0xFFF5DDD0)
    val L4 = Color(0xFFF4D9CB)
    val L5 = Color(0xFFF2D3C3)

    // Dark mode (primary tint sobre neutral10)
    val D0 = Palette.Neutral10
    val D1 = Color(0xFF28201B)
    val D2 = Color(0xFF2E2620)
    val D3 = Color(0xFF342C25)
    val D4 = Color(0xFF362D26)
    val D5 = Color(0xFF3A3129)
}

private val LightScheme = lightColorScheme(
    primary = Palette.Primary40,
    onPrimary = Color.White,
    primaryContainer = Palette.Primary90,
    onPrimaryContainer = Palette.Primary10,
    secondary = Palette.Secondary40,
    onSecondary = Color.White,
    secondaryContainer = Palette.Secondary90,
    onSecondaryContainer = Palette.Secondary10,
    tertiary = Palette.Tertiary40,
    onTertiary = Color.White,
    tertiaryContainer = Palette.Tertiary90,
    onTertiaryContainer = Palette.Tertiary10,
    error = Palette.Error40,
    onError = Color.White,
    errorContainer = Palette.Error90,
    onErrorContainer = Palette.Error10,
    background = Palette.Neutral99,
    onBackground = Palette.Neutral10,
    surface = Palette.Neutral99,
    onSurface = Palette.Neutral10,
    surfaceVariant = Palette.NeutralVar90,
    onSurfaceVariant = Palette.NeutralVar40,
    outline = Palette.NeutralVar50,
    outlineVariant = Palette.NeutralVar80,
    inverseSurface = Palette.Neutral20,
    inverseOnSurface = Palette.Neutral95,
    inversePrimary = Palette.Primary80,
    surfaceTint = Palette.Primary40,
)

private val DarkScheme = darkColorScheme(
    primary = Palette.Primary80,
    onPrimary = Palette.Primary20,
    primaryContainer = Palette.Primary30,
    onPrimaryContainer = Palette.Primary90,
    secondary = Palette.Secondary80,
    onSecondary = Palette.Secondary20,
    secondaryContainer = Palette.Secondary30,
    onSecondaryContainer = Palette.Secondary90,
    tertiary = Palette.Tertiary80,
    onTertiary = Palette.Tertiary20,
    tertiaryContainer = Palette.Tertiary30,
    onTertiaryContainer = Palette.Tertiary90,
    error = Palette.Error80,
    onError = Palette.Error10,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Palette.Error90,
    background = Palette.Neutral10,
    onBackground = Palette.Neutral90,
    surface = Palette.Neutral10,
    onSurface = Palette.Neutral90,
    surfaceVariant = Palette.NeutralVar30,
    onSurfaceVariant = Palette.NeutralVar80,
    outline = Palette.NeutralVar60,
    outlineVariant = Palette.NeutralVar30,
    inverseSurface = Palette.Neutral90,
    inverseOnSurface = Palette.Neutral20,
    inversePrimary = Palette.Primary40,
    surfaceTint = Palette.Primary80,
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Tipografía M3 canónica — sans-serif del sistema (puede reemplazarse con una fuente custom). */
val AppTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 57.sp, lineHeight = 64.sp,  letterSpacing = (-0.25).sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.Normal,  fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 16.sp, lineHeight = 24.sp,  letterSpacing = 0.15.sp),
    titleSmall    = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 14.sp, lineHeight = 20.sp,  letterSpacing = 0.1.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 16.sp, lineHeight = 24.sp,  letterSpacing = 0.5.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 14.sp, lineHeight = 20.sp,  letterSpacing = 0.25.sp),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 12.sp, lineHeight = 16.sp,  letterSpacing = 0.4.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 14.sp, lineHeight = 20.sp,  letterSpacing = 0.1.sp),
    labelMedium   = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 12.sp, lineHeight = 16.sp,  letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 11.sp, lineHeight = 16.sp,  letterSpacing = 0.5.sp),
)

@Composable
fun CecosesolaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? androidx.activity.ComponentActivity)?.window
            window?.let { WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !darkTheme }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
