package com.dusk0382.cecosesolaprecios.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dusk0382.cecosesolaprecios.ui.theme.Espacio

/**
 * Control de cantidad: [−] cifra [+]. Un único componente para tarjeta, detalle
 * y carrito (antes había dos implementaciones con tamaños distintos — ver
 * DESIGN.md §8.1).
 *
 * `animateContentSize` absorbe el cambio de ancho de la cifra (9 → 10) sin que
 * el precio al lado dé un salto seco: motion funcional, 0 recomposiciones extra.
 */
@Composable
fun Stepper(
    cantidad: Int,
    onCantidad: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.animateContentSize(
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        OutlinedIconButton(
            onClick = { onCantidad(cantidad - 1) },
            modifier = Modifier.size(40.dp),
        ) {
            Text("−", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = "$cantidad",
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            textAlign = TextAlign.Center,
            modifier = Modifier.width(Espacio.xl),
        )
        OutlinedIconButton(
            onClick = { onCantidad(cantidad + 1) },
            modifier = Modifier.size(40.dp),
        ) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }
    }
}
