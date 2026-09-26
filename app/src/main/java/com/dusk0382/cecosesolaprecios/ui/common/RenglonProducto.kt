package com.dusk0382.cecosesolaprecios.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.domain.formatearNombreProducto
import com.dusk0382.cecosesolaprecios.ui.theme.AltoImagenTarjeta
import com.dusk0382.cecosesolaprecios.ui.theme.Espacio
import com.dusk0382.cecosesolaprecios.ui.theme.LocalColoresPrecio

/**
 * El primitivo único (DESIGN.md §4): imagen 1:1 sobre contenedor tonal, nombre
 * a 2 líneas, precio grande con cifras tabulares y control de carrito. Sin
 * categoría, sin marca, sin borde — la jerarquía la hacen el tono y el tamaño.
 */
@Composable
fun RenglonProducto(
    producto: ProductEntity,
    cantidad: Int,
    esFavorito: Boolean,
    onCantidad: (Int) -> Unit,
    onFavorito: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(AltoImagenTarjeta)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                AsyncImage(
                    model = producto.imagenUrl ?: producto.imagenGrandeUrl,
                    contentDescription = null, // el nombre ya está en texto: no duplicar para el lector
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(Espacio.s),
                )
                IconoFavorito(
                    activo = esFavorito,
                    onClick = onFavorito,
                    modifier = Modifier.align(Alignment.TopEnd).padding(Espacio.xs),
                )
            }
            Column(Modifier.padding(Espacio.s)) {
                Text(
                    text = formatearNombreProducto(producto.nombre),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                )
                Spacer(Modifier.height(Espacio.minimo))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val (precio, moneda) = producto.precioMostrado()
                    PriceText(
                        valor = precio,
                        prefix = moneda,
                    )
                    ControlCarrito(
                        cantidad = cantidad,
                        onCantidad = onCantidad,
                    )
                }
            }
        }
    }
}

/**
 * Corazón del primitivo: 48dp de área táctil (el mínimo de accesibilidad), icono
 * 20dp. El relleno de 48 es transparente — el área táctil es grande, el círculo
 * visible no existe, que es lo que evita el "puntito" sobre la foto.
 */
@Composable
private fun IconoFavorito(
    activo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(Espacio.toqueMinimo)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (activo) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (activo) "Quitar de favoritos" else "Agregar a favoritos",
            // El naranja de marca sobre el contenedor de la imagen daba 2.75:1, por
            // debajo del 3:1 de un icono: un corazón activo casi invisible. El token
            // de acento da 4.77:1. `primary` es relleno, no color de icono.
            tint = if (activo) LocalColoresPrecio.current.acento
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * El control de carrito en el mismo lugar siempre (Baymard: consistencia entre
 * ítems): botón "+" cuando no está en el carrito, stepper cuando sí. Cambiar
 * de uno a otro es el mismo espacio, no un layout nuevo.
 */
@Composable
private fun ControlCarrito(
    cantidad: Int,
    onCantidad: (Int) -> Unit,
) {
    if (cantidad > 0) {
        Stepper(cantidad = cantidad, onCantidad = onCantidad)
    } else {
        FilledIconButton(
            onClick = { onCantidad(1) },
            modifier = Modifier.size(Espacio.toqueMinimo),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Agregar al carrito", modifier = Modifier.size(20.dp))
        }
    }
}
