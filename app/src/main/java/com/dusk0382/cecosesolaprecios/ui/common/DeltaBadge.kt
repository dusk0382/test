package com.dusk0382.cecosesolaprecios.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dusk0382.cecosesolaprecios.R
import com.dusk0382.cecosesolaprecios.ui.theme.PriceDownGreen
import com.dusk0382.cecosesolaprecios.ui.theme.PriceDownGreenDark
import com.dusk0382.cecosesolaprecios.ui.theme.PriceUpRed
import com.dusk0382.cecosesolaprecios.ui.theme.PriceUpRedDark

/**
 * Variación de precio. **Ambos montos deben estar en la misma moneda**: se le
 * pasan los valores CEC (no Bs) porque el precio anterior solo existe en CEC —
 * mezclar Bs actual con CEC anterior daría un porcentaje inventado.
 * Si falta cualquiera de los dos, no se pinta nada (producto sin enriquecer).
 *
 * Vectores propios (ic_trending_*): material-icons-extended costaría ~10MB por
 * dos flechas en un APK que queremos chico.
 */
@Composable
fun DeltaBadge(
    actual: Double?,
    anterior: Double?,
    modifier: Modifier = Modifier,
) {
    if (actual == null || anterior == null) return
    if (anterior <= 0.0 || actual == anterior) return

    val subio = actual > anterior
    val pct = ((actual - anterior) / anterior * 100).toInt()
    if (pct == 0) return

    val dark = isSystemInDarkTheme()
    val tint = when {
        subio -> if (dark) PriceUpRedDark else PriceUpRed
        else -> if (dark) PriceDownGreenDark else PriceDownGreen
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Image(
            painter = painterResource(if (subio) R.drawable.ic_trending_up else R.drawable.ic_trending_down),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "${if (subio) "+" else "−"}$pct%",
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}
