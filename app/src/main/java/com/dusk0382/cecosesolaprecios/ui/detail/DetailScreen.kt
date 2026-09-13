package com.dusk0382.cecosesolaprecios.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.dusk0382.cecosesolaprecios.ui.common.DeltaBadge
import com.dusk0382.cecosesolaprecios.ui.common.formatBs
import com.dusk0382.cecosesolaprecios.ui.common.precioMostrado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    vm: DetailViewModel = hiltViewModel(),
) {
    val p by vm.producto.collectAsStateWithLifecycle()
    val fav by vm.esFavorito.collectAsStateWithLifecycle()
    val qty by vm.cantidadEnCarrito.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(p?.nombre?.take(30) ?: "Producto", maxLines = 1) },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
                },
                actions = {
                    IconButton({ vm.toggleFavorite() }) {
                        Icon(
                            if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            "Favorito",
                            tint = if (fav) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { insets ->
        val prod = p ?: return@Scaffold
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AsyncImage(
                model = prod.imagenGrandeUrl ?: prod.imagenUrl,
                contentDescription = prod.nombre,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(220.dp).clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.height(16.dp))
            Text(prod.nombre, style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(12.dp))
            val (precio, moneda) = prod.precioMostrado()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "$moneda ${formatBs(precio)}",
                    style = MaterialTheme.typography.largeTitle.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.primary,
                )
                DeltaBadge(prod.precioCec, prod.precioAnteriorCec) // CEC↔CEC: misma moneda
            }
            // La otra moneda, tal cual viene de la fuente: el precio en Bs y el
            // solidario (CEC) son precios distintos, no una conversión — por eso
            // se rotula "precio en bolívares" y nunca "equivalente".
            val secundario = if (moneda == "USD") {
                "Precio en bolívares: Bs ${formatBs(prod.precioBs)}"
            } else {
                prod.precioCec?.takeIf { it > 0 }?.let { "Precio solidario: ${formatBs(it)} CEC" }
            }
            if (secundario != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    secundario,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                prod.marca?.let { Dato("Marca", it) }
                prod.presentacion?.let { Dato("Presentación", it) }
                prod.categoria?.let { Dato("Categoría", it.replaceFirstChar { c -> c.uppercase() }) }
                prod.barcode?.let { Dato("Código de barras", it) }
                prod.updatedAt?.let { Dato("Actualizado", it.substringBefore('T')) }
            }

            Spacer(Modifier.height(24.dp))
            if (qty > 0) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QtyButton("−") { vm.setQuantity(qty - 1) }
                    Text("${qty} en el carrito", style = MaterialTheme.typography.bodyLarge)
                    QtyButton("+") { vm.setQuantity(qty + 1) }
                }
            } else {
                Button(onClick = { vm.setQuantity(1) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Agregar al carrito")
                }
            }
        }
    }
}

@Composable
private fun Dato(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun QtyButton(text: String, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Text(text, style = MaterialTheme.typography.titleLarge)
    }
}
