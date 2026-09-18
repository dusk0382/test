package com.dusk0382.cecosesolaprecios.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Rampa de formas con radios grandes pero no redondos-en-todo (DESIGN.md §3.7).
 * Lo full-round se reserva a lo interactivo (buscador, chips, FAB); las tarjetas
 * y los contenedores de imagen usan `large`/`medium` para que cada pantalla no
 * parezca una fila de píldoras.
 */
val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
