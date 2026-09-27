package com.dusk0382.cecosesolaprecios.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dusk0382.cecosesolaprecios.ui.theme.Espacio

/**
 * Fila etiqueta → valor (ajustes y detalle). Unificada aquí porque existía
 * duplicada con dos layouts distintos (DESIGN.md §8.1).
 *
 * **El valor necesita un tope de ancho.** En un `Row`, los hijos sin peso se
 * miden primero con el ancho disponible completo, y el peso se reparte con lo que
 * sobra. La etiqueta tenía `weight(1f)` y el valor no tenía ninguna restricción,
 * así que un valor largo —la lista de ferias: "Feria Del Centro, Feria de Ruiz
 * Pineda, Feria del Este, Santa Cruz"— se llevaba la fila entera y a la etiqueta
 * le quedaba el ancho de un carácter. "Ferias" se dibujaba apilada en vertical,
 * una letra por renglón. En un screenshot se veía como "F e r i a s".
 *
 * Con el tope, los valores cortos siguen en una línea sin robarle espacio a la
 * etiqueta, y los largos se envuelven dentro de su caja en vez de aplastarla.
 */
private val AnchoMaximoValor = 200.dp

@Composable
fun FilaDato(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Espacio.xs),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).widthIn(min = 88.dp),
        )
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = AnchoMaximoValor).padding(start = Espacio.m),
        )
    }
}

