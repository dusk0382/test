package com.dusk0382.cecosesolaprecios.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colores de Compose, construidos desde [Paleta] (hex puro y medido).
 * Los nombres que ya importaban las pantallas se conservan para no romper el build
 * mientras se migra la UI; los nuevos roles viven en los esquemas de `Theme.kt`.
 */
val OrangePrimary = Color(Paleta.NaranjaMarca)
val OrangeDark = Color(Paleta.AcentoPrecioOscuro)
val OrangeContainerLight = Color(Paleta.MarcaContenedorClaro)
val OnOrangeContainerLight = Color(Paleta.SobreMarcaContenedorClaro)

val NeutralBackgroundLight = Color(Paleta.FondoClaro)
val NeutralSurfaceLight = Color(Paleta.SuperficieClaro)
val NeutralVariantLight = Color(Paleta.SuperficieVarianteClaro)
val OnNeutralVariantLight = Color(Paleta.SobreSuperficieVarianteClaro)

val NeutralBackgroundDark = Color(Paleta.FondoOscuro)
val NeutralSurfaceDark = Color(Paleta.SuperficieOscura)
val NeutralVariantDark = Color(Paleta.SuperficieVarianteOscura)
val OnNeutralVariantDark = Color(Paleta.SobreSuperficieVarianteOscuro)

// Semántica de variación de precio: siempre CEC contra CEC (nunca Bs contra CEC).
val PriceUpRed = Color(Paleta.SubeClaro)
val PriceDownGreen = Color(Paleta.BajaClaro)
val PriceUpRedDark = Color(Paleta.SubeOscuro)
val PriceDownGreenDark = Color(Paleta.BajaOscuro)
