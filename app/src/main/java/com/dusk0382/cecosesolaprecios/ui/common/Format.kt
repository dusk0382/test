package com.dusk0382.cecosesolaprecios.ui.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import com.dusk0382.cecosesolaprecios.ui.theme.LocalColoresPrecio
import com.dusk0382.cecosesolaprecios.ui.theme.PrecioTarjeta
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formato venezolano fijo (1.365,50), sin mirar el locale del SO: en Android Go
 * las tablas de locale a veces vienen recortadas y cambiarían el look de la app
 * sin avisar. ThreadLocal porque DecimalFormat no es thread-safe.
 *
 * **Siempre dos decimales.** Antes el patrón era `#,##0.##`, que los quitaba al
 * final: una lista de precios mezclaba "2,1" con "6,42" y "5,05" en la misma
 * columna. En un cartel de precios, donde la comparación es de un vistazo, los
 * decimales desiguales rompen la alineación y hacen más difícil comparar cuál es
 * más barato. El dinero se muestra con dos decimales siempre.
 */
private val symbols = DecimalFormatSymbols().apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}
private val numberFormat = ThreadLocal.withInitial { DecimalFormat("#,##0.00", symbols) }
private val fechaFormat = ThreadLocal.withInitial {
    SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale("es", "VE"))
}
/** El formato crudo del mirror: "2026-09-26 19:00:24". Sin zona: es hora local
 *  del servidor que corrió el scraper. */
private val fechaMirror = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("es", "VE"))
}

fun formatBs(valor: Double): String = numberFormat.get()!!.format(valor)

/** Epoch millis → "13/09/2026 a las 04:12". */
fun formatFechaHora(millis: Long): String =
    fechaFormat.get()!!.format(Date(millis))

/**
 * El mirror guarda su fecha de actualización como texto crudo "2026-09-26
 * 19:00:24" —es el campo `fecha_actualizacion` de precios.json, tal cual—,
 * mientras que la API oficial se guarda como epoch millis. Se normalizan al
 * mismo formato que [formatFechaHora] para que las dos filas de Ajustes sean
 * comparables de un vistazo: en el screenshot se veía "2026-09-26 19:00:24" al
 * lado de "26/09/2026 a las 01:43", en la misma columna.
 *
 * Sin sufijo de zona: esa hora es la del servidor que corrió el scraper, en su
 * propio huso. Agregar "Z" la correría al leerla y mostraría una hora que no
 * coincide con la que dice el origen.
 *
 * Si el formato no fuera el esperado se devuelve el texto tal cual, sin fallar:
 * es un dato de terceros y no vale la pena romper una pantalla por una fecha rara.
 */
fun formatearFechaMirror(iso: String): String = try {
    fechaMirror.get()!!.parse(iso.trim())?.let { fechaFormat.get()!!.format(it) } ?: iso
} catch (e: Exception) {
    iso
}

/**
 * Precio con numerales tabulares para que las cifras no bailen al alinearse.
 *
 * Es el ÚNICO lugar donde se pinta un precio: catálogo, detalle y carrito pasan
 * por acá. Antes cada pantalla escribía su propio `Text` con su propio color y su
 * propio `.copy(fontFeatureSettings = "tnum")` inlineado, y por eso el precio del
 * detalle salió en `colorScheme.primary` (naranja de marca, 3.26:1 — falla AA)
 * mientras el de la tarjeta usaba el token de acento (5.66:1). Dos precios, dos
 * reglas, y la que no cumplía era justo la más importante.
 *
 * El color por defecto es el token de acento del precio, no `primary`: el
 * naranja de marca es relleno (DESIGN.md §3.11).
 *
 * El `formatBs` va en `remember` porque el precio se recompone en cada cambio de
 * estado de la pantalla y `DecimalFormat.format` no es gratis.
 */
@Composable
fun PriceText(
    valor: Double,
    prefix: String = "Bs",
    modifier: Modifier = Modifier,
    style: TextStyle = PrecioTarjeta,
    color: Color = LocalColoresPrecio.current.acento,
    textAlign: TextAlign? = null,
) {
    val texto = remember(valor, prefix) {
        val numero = formatBs(valor)
        if (prefix.isBlank()) numero else "$prefix $numero"
    }
    Text(
        text = texto,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
    )
}
