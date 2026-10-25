package com.dusk0382.cecosesolaprecios.ui.cart

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dusk0382.cecosesolaprecios.data.local.CartLine
import com.dusk0382.cecosesolaprecios.domain.formatearNombreProducto
import com.dusk0382.cecosesolaprecios.data.repository.ProductRepository
import com.dusk0382.cecosesolaprecios.ui.common.LocalUsdPrecio
import com.dusk0382.cecosesolaprecios.ui.common.formatBs
import com.dusk0382.cecosesolaprecios.ui.common.importeLinea
import com.dusk0382.cecosesolaprecios.ui.common.precioMostrado
import com.dusk0382.cecosesolaprecios.ui.common.Stepper
import com.dusk0382.cecosesolaprecios.ui.theme.PrecioDetalle
import com.dusk0382.cecosesolaprecios.ui.theme.PrecioApoyo
import com.dusk0382.cecosesolaprecios.ui.theme.PrecioTarjeta
import com.dusk0382.cecosesolaprecios.ui.theme.Espacio
import com.dusk0382.cecosesolaprecios.ui.common.PriceText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CartViewModel @Inject constructor(
    private val repo: ProductRepository,
) : ViewModel() {

    private val _cargado = MutableStateFlow(false)

    /** True desde la primera emisión de Room: la pantalla no muestra vacío antes. */
    val cargado: StateFlow<Boolean> = _cargado.asStateFlow()

    // Una sola colección del flow de Room. Antes `lineas`, `totalBs` y `totalCec`
    // hacían tres `stateIn` sobre el flow frío y disparaban tres consultas a
    // `cart_items` por cada cambio. Los totales se derivan del StateFlow ya
    // compartido: el map corre en memoria y Room se consulta una vez.
    private val lineasState = repo.cartFlow()
        .onEach { _cargado.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lineas: StateFlow<List<CartLine>> = lineasState

    /** Total en Bs. Siempre disponible. */
    val totalBs: StateFlow<Double> = lineasState
        .map { ls -> ls.sumOf { it.precioBs * it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /**
     * Total del precio solidario. **null si algún producto del carrito aún no tiene
     * solidario**: sumar solo los que sí lo tienen daría un total menor al real y
     * el usuario lo descubriría en la caja. En ese caso se muestra Bs.
     */
    val totalCec: StateFlow<Double?> = lineasState
        .map { ls ->
            when {
                ls.isEmpty() -> null
                ls.any { it.precioCec == null } -> null
                else -> ls.sumOf { it.precioCec!! * it.quantity }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Para el badge del NavigationBar: unidades, no líneas. */
    val count: StateFlow<Int> = repo.cartUnitsCountFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun setQuantity(productId: Long, qty: Int) =
        viewModelScope.launch { repo.setQuantity(productId, qty) }

    fun quitar(productId: Long) = viewModelScope.launch { repo.setQuantity(productId, 0) }

    fun vaciar() = viewModelScope.launch { repo.clearCart() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(vm: CartViewModel = hiltViewModel()) {
    val lineas by vm.lineas.collectAsStateWithLifecycle()
    val cargado by vm.cargado.collectAsStateWithLifecycle()
    val totalBs by vm.totalBs.collectAsStateWithLifecycle()
    val totalCec by vm.totalCec.collectAsStateWithLifecycle()
    val usd = LocalUsdPrecio.current
    val context = LocalContext.current
    var confirmarVaciar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // El total sigue la moneda del toggle solo si TODOS los ítems tienen ese precio.
    val mostrarTotalUsd = usd && totalCec != null

    // Quitar es reversible mientras el snackbar siga abierto: la línea vuelve
    // con la cantidad exacta que tenía. Si se cierra sin tocar "Deshacer", no
    // se restaura nada.
    fun quitarConUndo(linea: CartLine) {
        vm.quitar(linea.productId)
        scope.launch {
            // Si otro quita ya tenía su snackbar abierto, se cierra: el undo que
            // importa es el de la última acción, no una cola de 10 s por línea.
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "«${formatearNombreProducto(linea.nombre)}» quitado",
                actionLabel = "Deshacer",
                withDismissAction = false,
                // Con actionLabel, Material3 usa Indefinite por defecto: el
                // snackbar no se iba nunca y los borrados se encolaban. 10 s
                // alcanzan para deshacer sin bloquear el siguiente.
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                vm.setQuantity(linea.productId, linea.quantity)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            if (!cargado) {
                // Room todavía no respondió: un hueco honesto, no un vacío falso.
                Box(Modifier.fillMaxWidth().weight(1f))
            } else if (lineas.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "El carrito está vacío.\nAgrega productos desde la lista.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(Espacio.m),
                    verticalArrangement = Arrangement.spacedBy(Espacio.s),
                ) {
                    items(lineas, key = { it.productId }) { linea ->
                        // Sin `remember(productId)`: las lambdas deben capturar la
                        // línea actual, no la de la primera composición, porque el
                        // undo restaura la cantidad vigente al momento de quitar.
                        CartRow(
                            linea = linea,
                            usd = usd,
                            onQty = { q ->
                                if (q <= 0) quitarConUndo(linea)
                                else vm.setQuantity(linea.productId, q)
                            },
                            onQuitar = { quitarConUndo(linea) },
                        )
                    }
                }
            }

            if (lineas.isNotEmpty()) {
                Spacer(Modifier.height(Espacio.s))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Espacio.l, vertical = Espacio.s),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Total", style = MaterialTheme.typography.titleMedium)
                        if (usd && totalCec == null) {
                            Text(
                                "Algunos precios aún no tienen valor en USD",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    PriceText(
                        valor = if (mostrarTotalUsd) totalCec!! else totalBs,
                        prefix = if (mostrarTotalUsd) "USD" else "Bs",
                        style = PrecioDetalle,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Espacio.m, vertical = Espacio.s),
                    horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                ) {
                    Button(
                        onClick = { compartirCarrito(context, lineas, usd) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(Espacio.s))
                        Text("Compartir")
                    }
                    OutlinedButton(onClick = { confirmarVaciar = true }) {
                        Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(Espacio.s))
                        Text("Vaciar")
                    }
                }
            }
        }
    }

    if (confirmarVaciar) {
        AlertDialog(
            onDismissRequest = { confirmarVaciar = false },
            title = { Text("¿Vaciar el carrito?") },
            confirmButton = {
                TextButton(onClick = { vm.vaciar(); confirmarVaciar = false }) { Text("Vaciar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarVaciar = false }) { Text("Cancelar") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartRow(linea: CartLine, usd: Boolean, onQty: (Int) -> Unit, onQuitar: () -> Unit) {
    // La fila se borra desde `onQuitar` en el gesto, no desde confirmValueChange:
    // borrar ahí compite con la animación de settle y el renglón desaparecía
    // a media transición. Además hay un camino sin gesto: el "−" del stepper en
    // cantidad 1 quita la línea, que es lo que necesita un lector de pantalla.
    var descartada by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.4f },
        confirmValueChange = { valor ->
            if (valor == SwipeToDismissBoxValue.EndToStart) {
                descartada = true
                true
            } else {
                false
            }
        },
    )
    // El estado del swipe es saveable y la fila deshecha vuelve con la misma
    // key: si el gesto quedó guardado como descartado, se asienta para que la
    // línea reaparezca entera en vez de fuera de pantalla.
    LaunchedEffect(Unit) {
        if (!descartada && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }
    if (descartada) {
        LaunchedEffect(linea.productId) {
            onQuitar()
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().padding(horizontal = Espacio.l),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, "Quitar", tint = MaterialTheme.colorScheme.error)
            }
        },
    ) {
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            // Sin borde: la jerarquía la da el tono del contenedor (DESIGN.md §3.2).
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(Espacio.m)) {
                // El nombre usa el ancho completo: antes el stepper y el importe
                // le robaban la mitad de la fila y "Aceite de Oliva 250ml Extra
                // Virgen" se cortaba en "Extra Vi…".
                Text(
                    text = formatearNombreProducto(linea.nombre),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Espacio.s))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Stepper(cantidad = linea.quantity, onCantidad = onQty)
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        val (importe, monedaLinea) = linea.importeLinea(usd)
                        PriceText(
                            valor = importe,
                            prefix = monedaLinea,
                            style = PrecioTarjeta,
                        )
                        // El precio por unidad **solo si la cantidad no es 1**. Con
                        // una unidad, el "Bs 855 c/u" y el importe de la línea son
                        // el mismo número impreso dos veces, y en el screenshot se
                        // leía "USD c/u 2,1" al lado de "USD 2,1": ruido puro. Con
                        // dos o más unidades sí aporta información.
                        if (linea.quantity > 1) {
                            val (unitario, moneda) = linea.precioMostrado(usd)
                            PriceText(
                                valor = unitario,
                                prefix = "$moneda c/u",
                                style = PrecioApoyo,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Texto plano para compartir. El total usa la misma regla que la pantalla
 * (USD solo si todos los ítems lo tienen); el precio unitario se rotula por
 * línea, así el mensaje nunca mezcla monedas bajo un solo "Total".
 */
private fun compartirCarrito(context: Context, lineas: List<CartLine>, usd: Boolean) {
    // Una sola moneda en todo el mensaje: USD solo si todos los ítems lo tienen.
    val usarUsd = usd && lineas.isNotEmpty() && lineas.all { it.precioCec != null }
    val sb = StringBuilder()
    sb.append("Mi carrito — precios Cecosesola\n\n")
    lineas.forEach { l ->
        val (precio, moneda) = l.precioMostrado(usarUsd)
        sb.append("• ${formatearNombreProducto(l.nombre)} ×${l.quantity} = $moneda ${formatBs(precio * l.quantity)}\n")
    }
    val totalTexto = if (usarUsd) {
        "USD ${formatBs(lineas.sumOf { it.precioCec!! * it.quantity })}"
    } else {
        "Bs ${formatBs(lineas.sumOf { it.precioBs * it.quantity })}"
    }
    sb.append("\nTotal: $totalTexto")
    if (usd && !usarUsd) {
        sb.append("\n(Algunos productos no tienen precio solidario; se muestran en bolívares.)")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Compartir carrito"))
}
