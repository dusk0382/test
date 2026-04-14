package com.cecosesola.coop.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cecosesola.coop.R
import com.cecosesola.coop.domain.model.Producto

@Composable
fun ProductoCard(
    producto: Producto,
    isFavorito: Boolean = false,
    onFavoritoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // El precio formateado se calcula UNA SOLA VEZ por instancia, no en cada recomposición.
    val precioFormateado = remember(producto.precio) {
        "Bs. %.2f".format(producto.precio)
    }

    // ImageRequest construida una sola vez para esta URL.
    // Sin remember, Coil recibía un nuevo objeto en cada recomposición y podía
    // cancelar y relanzar la carga innecesariamente.
    val context = LocalContext.current
    val imageRequest = remember(producto.imagenUrl) {
        ImageRequest.Builder(context)
            .data(producto.imagenUrl)
            .size(140)       // 2x del tamaño visual (70dp) para hdpi
            .crossfade(true)
            .build()
    }

    // ELIMINADO: LaunchedEffect + delay(30) + mutableStateOf(visible).
    // Cada card lanzaba una corrutina, esperaba 30ms y causaba una recomposición extra.
    // Con 50 items visibles = 50 corrutinas + 50 recomposiciones al montar la lista.

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ELIMINADO: Card anidado alrededor de la imagen.
            // Era una segunda capa de shadow/clip completamente innecesaria.
            // clip() hace el mismo efecto sin el overhead de layout de Card.
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model              = imageRequest,
                    contentDescription = producto.nombre,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop,
                    placeholder        = painterResource(R.drawable.placeholder_producto),
                    error              = painterResource(R.drawable.placeholder_error)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    producto.nombre,
                    style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    color    = MaterialTheme.colorScheme.onSurface
                )
                if (producto.presentacion.isNotBlank()) {
                    Text(
                        producto.presentacion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        precioFormateado,
                        style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color    = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            IconButton(
                onClick  = onFavoritoClick,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (isFavorito) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Icon(
                    if (isFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (isFavorito) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
