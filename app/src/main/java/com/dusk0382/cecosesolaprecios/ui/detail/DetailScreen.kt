package com.dusk0382.cecosesolaprecios.ui.detail

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.dusk0382.cecosesolaprecios.domain.esBarcodeValido
import com.dusk0382.cecosesolaprecios.domain.formatearNombreProducto
import com.dusk0382.cecosesolaprecios.ui.common.DeltaBadge
import com.dusk0382.cecosesolaprecios.ui.common.FilaDato
import com.dusk0382.cecosesolaprecios.ui.common.PriceText
import com.dusk0382.cecosesolaprecios.ui.common.Stepper
import com.dusk0382.cecosesolaprecios.ui.theme.Espacio
import com.dusk0382.cecosesolaprecios.ui.theme.PlatoImagen
import com.dusk0382.cecosesolaprecios.ui.theme.PrecioDetalle
import com.dusk0382.cecosesolaprecios.ui.common.formatBs
import com.dusk0382.cecosesolaprecios.ui.common.formatFechaHora
import com.dusk0382.cecosesolaprecios.ui.common.formatearFechaIso
import com.dusk0382.cecosesolaprecios.ui.common.precioMostrado
import com.dusk0382.cecosesolaprecios.ui.theme.LocalColoresPrecio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    vm: DetailViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val p = (estado as? EstadoDetalle.Listo)?.producto
    val fav by vm.esFavorito.collectAsStateWithLifecycle()
    val qty by vm.cantidadEnCarrito.collectAsStateWithLifecycle()
    val ultimaUsd by vm.ultimaActualizacionUsd.collectAsStateWithLifecycle()

    Scaffold(
        // El Scaffold de MainActivity ya aplicó los insets del sistema; sin esto
        // el status bar se suma dos veces y el contenido queda un status bar más abajo.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                // Por lo mismo: el app bar no debe volver a reservar la barra de estado.
                windowInsets = WindowInsets(0, 0, 0, 0),
                // Sin `take(30)`: cortaba el nombre a mitad de palabra y con
                // `maxLines = 1` sin `overflow` el texto se recortaba a media
                // palabra sin ninguna señal de que siguiera. `maxLines` solo sin
                // `overflow` usa Clip, que es indistinguible de un bug de render.
                // Esto ya se intentó arreglar una vez y el cambio no aplicó: el
                // texto era posicional, no `text = `, así que el replace no
                // encontró nada. Con assert esta vez.
                title = {
                    Text(
                        text = p?.let { formatearNombreProducto(it.nombre) }.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
                },
                actions = {
                    IconButton({ vm.toggleFavorite() }) {
                        Icon(
                            if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            // La etiqueta dice qué hace el botón, no qué es. Con
                            // "Favorito" fijo, quien usa TalkBack oía lo mismo
                            // estando o no el producto en favoritos, igual que en
                            // la tarjeta.
                            contentDescription = if (fav) "Quitar de favoritos"
                            else "Agregar a favoritos",
                            // El acento de precio está reservado al precio (§5);
                            // el relleno del corazón ya distingue el estado.
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { insets ->
        // Antes: `p ?: return@Scaffold`, que pintaba absolutamente nada. Ahora
        // cada caso dice qué está pasando.
        val prod = p
        if (prod == null) {
            Column(
                Modifier.fillMaxSize().padding(insets),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (estado is EstadoDetalle.Cargando) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        "Este producto ya no está en el catálogo.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.l, vertical = Espacio.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                color = PlatoImagen,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AsyncImage(
                    model = prod.imagenGrandeUrl ?: prod.imagenUrl,
                    // El nombre está abajo como texto. Con descripción en la imagen,
                    // TalkBack lo anuncia dos veces seguidas.
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 320.dp)
                        .padding(Espacio.l),
                )
            }

            Spacer(Modifier.height(Espacio.l))
            Text(
                formatearNombreProducto(prod.nombre),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )

            Spacer(Modifier.height(Espacio.m))
            val (precio, moneda) = prod.precioMostrado()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.m),
            ) {
                PriceText(
                    valor = precio,
                    prefix = moneda,
                    style = PrecioDetalle.copy(fontWeight = FontWeight.ExtraBold),
                )
                DeltaBadge(prod.precioCec, prod.precioAnteriorCec) // CEC↔CEC: misma moneda
            }

            val filas = buildList {
                prod.marca?.takeUnless { it.isBlank() || it == prod.categoria }
                    ?.let { add("Marca" to it) }
                prod.presentacion?.takeUnless { it.isBlank() || it.equals("item", ignoreCase = true) }
                    ?.let { add("Presentación" to it) }
                prod.categoria?.takeUnless { it.isBlank() }
                    ?.let { add("Categoría" to it.replaceFirstChar { c -> c.uppercase() }) }
                prod.barcode?.takeIf { esBarcodeValido(it) }?.let { add("Código de barras" to it) }
                prod.updatedAt?.takeUnless { it.isBlank() }
                    ?.let { add("Actualizado" to formatearFechaIso(it)) }
            }
            if (filas.isNotEmpty()) {
                Spacer(Modifier.height(Espacio.l))
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(Espacio.l),
                        verticalArrangement = Arrangement.spacedBy(Espacio.s),
                    ) {
                        filas.forEach { (etiqueta, valor) -> FilaDato(etiqueta, valor) }
                    }
                }
            }

            Spacer(Modifier.height(Espacio.xl))
            Crossfade(
                targetState = qty > 0,
                animationSpec = tween(150),
                label = "detalle-accion",
            ) { enCarrito ->
                if (enCarrito) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(Espacio.m),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Stepper(cantidad = qty, onCantidad = vm::setQuantity)
                            Text(
                                " en el carrito",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { vm.setQuantity(1) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = Espacio.toqueMinimo),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            "Agregar al carrito",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
            Spacer(Modifier.height(Espacio.l))
        }
    }
}


