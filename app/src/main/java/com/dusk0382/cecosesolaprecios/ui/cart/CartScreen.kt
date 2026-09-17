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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dusk0382.cecosesolaprecios.data.local.CartLine
import com.dusk0382.cecosesolaprecios.data.repository.ProductRepository
import com.dusk0382.cecosesolaprecios.ui.common.LocalUsdPrecio
import com.dusk0382.cecosesolaprecios.ui.common.formatBs
import com.dusk0382.cecosesolaprecios.ui.common.importeLinea
import com.dusk0382.cecosesolaprecios.ui.common.precioMostrado
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CartViewModel @Inject constructor(
    private val repo: ProductRepository,
) : ViewModel() {

    private val lineasFlow = repo.cartFlow()

    val lineas: StateFlow<List<CartLine>> = lineasFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Total en Bs. Siempre disponible. */
    val totalBs: StateFlow<Double> = lineasFlow
        .map { ls -> ls.sumOf { it.precioBs * it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /**
     * Total en CEC. **null si algún producto del carrito aún no tiene precio
     * solidario**: sumar solo los que sí lo tienen daría un total menor al real y
     * el usuario lo descubriría en la caja. En ese caso se muestra Bs.
     */
    val totalCec: StateFlow<Double?> = lineasFlow
        .map { ls ->
            when {
                ls.isEmpty() -> null
                ls.any { it.precioCec == null } -> null
                else -> ls.sumOf { it.precioCec!! * it.quantity }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Para el badge del NavigationBar. */
    val count: StateFlow<Int> = repo.cartCountFlow()
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
    val totalBs by vm.totalBs.collectAsStateWithLifecycle()
    val totalCec by vm.totalCec.collectAsStateWithLifecycle()
    val usd = LocalUsdPrecio.current
    val context = LocalContext.current
    var confirmarVaciar by remember { mutableStateOf(false) }

    // El total sigue la moneda del toggle solo si TODOS los ítems tienen CEC.
    val mostrarTotalUsd = usd && totalCec != null
    val totalTexto = if (mostrarTotalUsd) "USD ${formatBs(totalCec!!)}" else "Bs ${formatBs(totalBs)}"

    Scaffold { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            if (lineas.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "El carrito está vacío.\nAgrega productos desde la lista.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(lineas, key = { it.productId }) { linea ->
                        CartRow(
                            linea = linea,
                            usd = usd,
                            onQty = { vm.setQuantity(linea.productId, it) },
                            onQuitar = { vm.quitar(linea.productId) },
                        )
                    }
                }
            }

            if (lineas.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                    Text(
                        totalTexto,
                        style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { compartirCarrito(context, lineas, usd) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Compartir")
                    }
                    OutlinedButton(onClick = { confirmarVaciar = true }) {
                        Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
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
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.4f },
        confirmValueChange = { valor ->
            if (valor == SwipeToDismissBoxValue.EndToStart) { onQuitar(); true } else false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, "Quitar", tint = MaterialTheme.colorScheme.error)
            }
        },
    ) {
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(linea.nombre, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val (unitario, moneda) = linea.precioMostrado(usd)
                    Text(
                        "$moneda ${formatBs(unitario)} c/u",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Stepper(qty = linea.quantity, onQty = onQty)
                Spacer(Modifier.width(12.dp))
                Text(
                    linea.importeLinea(usd),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    modifier = Modifier.width(96.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun Stepper(qty: Int, onQty: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { onQty(qty - 1) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(36.dp)) {
            Text("−")
        }
        Text(
            "$qty",
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
            modifier = Modifier.width(width = 32.dp),
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = { onQty(qty + 1) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(36.dp)) {
            Text("+")
        }
    }
}

/**
 * Texto plano para compartir. El total usa la misma regla que la pantalla
 * (USD solo si todos los ítems lo tienen); el precio unitario se rotula por
 * línea, así el mensaje nunca mezcla monedas bajo un solo "Total".
 */
private fun compartirCarrito(context: Context, lineas: List<CartLine>, usd: Boolean) {
    val sb = StringBuilder()
    sb.append("🛒 Carrito — precios Cecosesola\n\n")
    lineas.forEach { l ->
        val (precio, moneda) = l.precioMostrado(usd)
        sb.append("• ${l.nombre} ×${l.quantity} = $moneda ${formatBs(precio * l.quantity)}\n")
    }
    val totalCec = if (usd && lineas.all { it.precioCec != null }) {
        lineas.sumOf { it.precioCec!! * it.quantity }
    } else {
        null
    }
    val totalTexto = if (totalCec != null) "USD ${formatBs(totalCec)}"
    else "Bs ${formatBs(lineas.sumOf { it.precioBs * it.quantity })}"
    sb.append("\nTotal: $totalTexto")

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Compartir carrito"))
}
