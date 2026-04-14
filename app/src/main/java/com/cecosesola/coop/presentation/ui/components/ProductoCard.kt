package com.cecosesola.coop.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cecosesola.coop.R
import com.cecosesola.coop.domain.model.Producto
import com.cecosesola.coop.presentation.ui.theme.Surf
import kotlinx.coroutines.delay

/**
 * Tarjeta de producto siguiendo M3 Filled Card.
 *
 * Cuando [isFavorito] es true usa secondaryContainer para el tonal color,
 * de lo contrario usa surfaceContainerLow (surface nivel 1).
 * Sin acentos de borde, sin barras laterales — la elevación tonal lo hace todo.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductoCard(
    producto: Producto,
    isFavorito: Boolean = false,
    onFavoritoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }

    // Animación de entrada escalonada
    LaunchedEffect(Unit) { delay(16); visible = true }

    // Feedback táctil de escala
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.98f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label         = "scale"
    )

    // Color de fondo según estado favorito
    val bgColor by animateColorAsState(
        targetValue   = if (isFavorito)
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        animationSpec = tween(300),
        label         = "bg"
    )

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 6 }
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale },
            shape  = MaterialTheme.shapes.extraLarge,
            color  = bgColor,
            tonalElevation = if (isFavorito) 2.dp else 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick      = {},
                        onLongClick  = onFavoritoClick
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Imagen
                ProductImage(
                    url    = producto.imagenUrl,
                    nombre = producto.nombre
                )

                // Texto
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        producto.nombre,
                        style    = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        color    = MaterialTheme.colorScheme.onSurface
                    )
                    if (producto.presentacion.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            producto.presentacion,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    PriceRow(precio = producto.precio)
                }

                // Botón favorito
                FavButton(
                    isFavorito = isFavorito,
                    onClick    = onFavoritoClick
                )
            }
        }
    }
}

// ── Imagen ────────────────────────────────────────────────────────────────────

@Composable
private fun ProductImage(url: String?, nombre: String) {
    Surface(
        modifier       = Modifier.size(64.dp),
        shape          = MaterialTheme.shapes.large,
        color          = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .size(128)
                .crossfade(true)
                .build(),
            contentDescription = nombre,
            modifier           = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.large),
            contentScale       = ContentScale.Crop,
            placeholder        = painterResource(R.drawable.placeholder_producto),
            error              = painterResource(R.drawable.placeholder_error)
        )
    }
}

// ── Precio ────────────────────────────────────────────────────────────────────

@Composable
private fun PriceRow(precio: Double) {
    Row(verticalAlignment = Alignment.Baseline, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            "Bs.",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        Text(
            String.format("%.2f", precio),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ── Botón favorito ────────────────────────────────────────────────────────────

@Composable
private fun FavButton(isFavorito: Boolean, onClick: () -> Unit) {
    val heartScale by animateFloatAsState(
        targetValue   = if (isFavorito) 1f else 0.9f,
        animationSpec = spring(Spring.DampingRatioLowBouncy),
        label         = "heart"
    )

    FilledTonalIconToggleButton(
        checked         = isFavorito,
        onCheckedChange = { onClick() },
        modifier        = Modifier.size(40.dp),
        shape           = MaterialTheme.shapes.medium,
        colors          = IconButtonDefaults.filledTonalIconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            checkedContentColor   = MaterialTheme.colorScheme.onSecondaryContainer,
            containerColor        = MaterialTheme.colorScheme.surface,
            contentColor          = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Icon(
            imageVector     = if (isFavorito) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            modifier        = Modifier
                .size(20.dp)
                .graphicsLayer { scaleX = heartScale; scaleY = heartScale }
        )
    }
}
