package com.dusk0382.cecosesolaprecios.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formato venezolano fijo (1.365,5), sin mirar el locale del SO: en Android Go
 * las tablas de locale a veces vienen recortadas y cambiarían el look de la app
 * sin avisar. ThreadLocal porque DecimalFormat no es thread-safe.
 */
private val symbols = DecimalFormatSymbols().apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}
private val numberFormat = ThreadLocal.withInitial { DecimalFormat("#,##0.##", symbols) }
private val fechaFormat = ThreadLocal.withInitial {
    SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale("es", "VE"))
}

fun formatBs(valor: Double): String = numberFormat.get()!!.format(valor)

/** Epoch millis → "13/09/2026 a las 04:12". */
fun formatFechaHora(millis: Long): String =
    fechaFormat.get()!!.format(Date(millis))

/** Precio con numerales tabulares para que las cifras no bailen al alinearse. */
@Composable
fun PriceText(
    valor: Double,
    prefix: String = "Bs",
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = "$prefix ${formatBs(valor)}",
        style = style.copy(fontFeatureSettings = "tnum"),
        color = color,
    )
}
