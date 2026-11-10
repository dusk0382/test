package com.dusk0382.cecosesolaprecios.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.dusk0382.cecosesolaprecios.R
import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.domain.formatearNombreProducto
import com.dusk0382.cecosesolaprecios.ui.theme.Espacio
import com.dusk0382.cecosesolaprecios.ui.theme.PlatoImagen
import com.dusk0382.cecosesolaprecios.ui.theme.TintaSobrePlato

/**
 * Tarjeta de producto: precio protagonista, nombre secundario, control discreto.
 * El precio es el elemento más grande para que la jerarquía se lea sola.
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
    val nombre = remember(producto.nombre) { formatearNombreProducto(producto.nombre) }

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(PlatoImagen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_image_placeholder),
                    contentDescription = null,
                    tint = TintaSobrePlato.copy(alpha = 0.25f),
                    modifier = Modifier.align(Alignment.Center).size(40.dp),
                )
                AsyncImage(
                    model = producto.imagenUrl ?: producto.imagenGrandeUrl,
                    contentDescription = null,
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
                    text = nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(Espacio.xs))

                val (precio, moneda) = producto.precioMostrado()
                PriceText(
                    valor = precio,
                    prefix = moneda,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(Espacio.s))

                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    ControlCarrito(cantidad = cantidad, onCantidad = onCantidad)
                }
            }
        }
    }
}

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
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

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
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Agregar al carrito", modifier = Modifier.size(20.dp))
        }
    }
}
