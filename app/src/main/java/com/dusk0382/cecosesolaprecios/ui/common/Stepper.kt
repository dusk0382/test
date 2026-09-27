package com.dusk0382.cecosesolaprecios.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import com.dusk0382.cecosesolaprecios.ui.theme.Espacio

/**
 * Control de cantidad: [−] cifra [+]. Un único componente para tarjeta, detalle
 * y carrito (antes había dos implementaciones con tamaños distintos — ver
 * DESIGN.md §8.1).
 *
 * `animateContentSize` absorbe el cambio de ancho de la cifra (9 → 10) sin que
 * el precio al lado dé un salto seco: motion funcional, 0 recomposiciones extra.
 *
 * Los dos botones usan `Text("−")` / `Text("+")` y no iconos (diseño: un "−"
 * tipográfico es más legible que un ícono de flecha a 20dp), pero eso sin
 * semántica le diría a TalkBack "menos" y "más" sin decir de qué ni cuántas
 * unidades hay. Cada botón lleva su descripción y la cifra lleva la cantidad
 * como estado, para que "3" no se lea como un número suelto.
 *
 * **No se fija un tamaño explícito en los botones, a propósito.** `IconButton`
 * ya aplica `minimumInteractiveComponentSize()` antes de `size(40.dp)`, o sea
 * que el objetivo táctil es de 48 dp igual, pero el círculo dibujado mide 40 y la
 * fila reserva 40 en vez de 48. Poner `size(48.dp)` desde acá —que es lo que
 * hacía— reservaba 48 dp por botón para dibujar 40: en la tarjeta de catálogo
 * (unos 158 dp de ancho) el precio más los tres elementos del stepper medían
 * 215 dp, y el botón "+" quedaba cortado contra el borde. El screenshot lo
 * mostraba como una astilla vertical, es decir el producto era casi
 * inagregable al carrito.
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
            modifier = Modifier.semantics { contentDescription = "Quitar uno" },
        ) {
            Text("−", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = "$cantidad",
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(Espacio.xl)
                .semantics { stateDescription = "$cantidad en el carrito" },
        )
        OutlinedIconButton(
            onClick = { onCantidad(cantidad + 1) },
            modifier = Modifier.semantics { contentDescription = "Agregar uno" },
        ) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }
    }
}
