package com.dusk0382.cecosesolaprecios.ui.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.dusk0382.cecosesolaprecios.R
import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.domain.Rubro
import com.dusk0382.cecosesolaprecios.domain.etiquetaVisible
import com.dusk0382.cecosesolaprecios.domain.formatearNombreProducto
import com.dusk0382.cecosesolaprecios.ui.common.PriceText
import com.dusk0382.cecosesolaprecios.ui.common.Stepper
import com.dusk0382.cecosesolaprecios.ui.common.precioMostrado
import com.dusk0382.cecosesolaprecios.ui.theme.AltoImagenTarjeta
import com.dusk0382.cecosesolaprecios.ui.theme.Espacio
import com.dusk0382.cecosesolaprecios.ui.theme.LocalColoresPrecio

/**
 * Catálogo según DESIGN.md §7: buscador + botón de filtros con badge + FAB de
 * escáner. Sin filas de chips de categoría: los ~100 tags de la API no son
 * navegables (los reemplaza la clasificación medida de `domain/Rubros.kt`).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CatalogScreen(
    vm: CatalogViewModel,
    onOpenDetail: (Long) -> Unit,
    onScan: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val productos by vm.productos.collectAsStateWithLifecycle()
    val clasesSel by vm.clasesSel.collectAsStateWithLifecycle()
    val conteoClases by vm.conteoClases.collectAsStateWithLifecycle()
    val orden by vm.orden.collectAsStateWithLifecycle()
    val sincronizando by vm.sincronizando.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val yaRefrescado by vm.yaRefrescado.collectAsStateWithLifecycle()
    val cantidades by vm.cantidades.collectAsStateWithLifecycle()
    val favoritos by vm.favoritos.collectAsStateWithLifecycle()

    // El campo de búsqueda es dueño de su estado: cada tecleo sólo recompone el
    // campo, no los chips ni la grilla (DESIGN.md §8.2). Al VM llega el valor,
    // y el debounce decide cuándo reconsultar.
    var query by rememberSaveable { mutableStateOf(vm.busquedaInicial()) }
    var filtrosAbiertos by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // — Cabecera: título + filtros (con badge de activos) + ajustes —
            Row(
                Modifier.fillMaxWidth().padding(start = Espacio.l, end = Espacio.xs, top = Espacio.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Cecosesola",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { filtrosAbiertos = true }) {
                    BadgedBox(
                        badge = {
                            if (clasesSel.isNotEmpty()) {
                                Badge { Text("${clasesSel.size}") }
                            }
                        },
                    ) {
                        Icon(painterResource(R.drawable.ic_filter), "Filtros")
                    }
                }
                IconButton(onSettings) {
                    Icon(painterResource(R.drawable.ic_settings), "Ajustes")
                }
            }

            // — Buscador: icono de búsqueda, texto, limpiar —
            CampoBusqueda(
                query = query,
                onChange = { query = it; vm.onBusquedaChange(it) },
                modifier = Modifier.padding(horizontal = Espacio.l, vertical = Espacio.s),
            )

            // — Filtros activos como chips descartables, sólo mientras existan —
            if (clasesSel.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Espacio.l),
                    horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                ) {
                    lazyItems(clasesSel.sorted()) { clase ->
                        FilterChip(
                            selected = true,
                            onClick = { vm.toggleClase(clase) },
                            label = { Text(etiquetaClase(clase)) },
                        )
                    }
                }
            }

            // — Contenido: estado vacío ↔ grilla, con crossfade (no corte seco) —
            PullToRefreshBox(
                isRefreshing = sincronizando,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                Crossfade(
                    targetState = productos.isEmpty(),
                    label = "catalogo-estado",
                    modifier = Modifier.fillMaxSize(),
                ) { vacio ->
                    if (vacio) {
                        EstadoVacio(total, yaRefrescado)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(Espacio.m),
                            horizontalArrangement = Arrangement.spacedBy(Espacio.m),
                            verticalArrangement = Arrangement.spacedBy(Espacio.m),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(productos, key = { it.localId }) { p ->
                                RenglonProducto(
                                    producto = p,
                                    cantidad = cantidades[p.localId] ?: 0,
                                    esFavorito = p.localId in favoritos,
                                    onCantidad = { vm.setCantidad(p.localId, it) },
                                    onFavorito = { vm.toggleFavorito(p.localId) },
                                    onClick = { onOpenDetail(p.localId) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }

        // — FAB de escáner: entra con spring, se va al navegar (no teletransportación) —
        AnimatedVisibility(
            visible = !sincronizando,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Espacio.l),
        ) {
            FloatingActionButton(
                onClick = onScan,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(painterResource(R.drawable.ic_qr_scanner), "Escanear código de barras")
            }
        }
    }

    if (filtrosAbiertos) {
        HojaFiltros(
            clasesSel = clasesSel,
            conteoClases = conteoClases,
            orden = orden,
            onOrden = vm::onOrdenClick,
            onToggleClase = vm::toggleClase,
            onLimpiar = vm::limpiarFiltros,
            onCerrar = { filtrosAbiertos = false },
        )
    }
}

/** "DESPENSA" → "Despensa" con fallback seguro si el nombre no es un rubro. */
private fun etiquetaClase(nombre: String): String =
    runCatching { Rubro.valueOf(nombre).etiquetaVisible() }.getOrDefault(nombre)

@Composable
private fun CampoBusqueda(
    query: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Espacio.toqueMinimo)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.extraLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Espacio.l).size(20.dp),
        )
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.weight(1f).padding(horizontal = Espacio.s),
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onChange("") }) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Limpiar búsqueda",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.width(Espacio.s))
    }
}

/** Placeholder del buscador: lo dibuja el propio campo (sin composable extra). */
@Composable
private fun EstadoVacio(total: Int, yaRefrescado: Boolean) {
    Box(Modifier.fillMaxSize().padding(Espacio.xxl), contentAlignment = Alignment.Center) {
        Text(
            text = when {
                total == 0 -> "Aún no hay precios cargados.\nJala hacia abajo para sincronizar."
                yaRefrescado -> "Sin resultados para tu búsqueda."
                else -> "Cargando precios…"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Hoja de filtros: orden (segmentado), rubros con conteo por opción
 * (Baymard: la mejora de mayor impacto) y multi-selección. Ver DESIGN.md §7.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HojaFiltros(
    clasesSel: Set<String>,
    conteoClases: List<com.dusk0382.cecosesolaprecios.data.local.ClaseConteo>,
    orden: Orden,
    onOrden: (Orden) -> Unit,
    onToggleClase: (String) -> Unit,
    onLimpiar: () -> Unit,
    onCerrar: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Espacio.l)
                .padding(bottom = Espacio.xxl),
        ) {
            Text("Orden", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Espacio.s))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Orden.entries.forEachIndexed { i, o ->
                    SegmentedButton(
                        selected = orden == o,
                        onClick = { onOrden(o) },
                        shape = SegmentedButtonDefaults.itemShape(i, Orden.entries.size),
                    ) {
                        Text(o.label)
                    }
                }
            }

            Spacer(Modifier.height(Espacio.xl))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Rubros", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onLimpiar, enabled = clasesSel.isNotEmpty()) {
                    Text("Limpiar todo")
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                verticalArrangement = Arrangement.spacedBy(Espacio.s),
            ) {
                conteoClases.forEach { cc ->
                    FilterChip(
                        selected = cc.clase in clasesSel,
                        onClick = { onToggleClase(cc.clase) },
                        label = { Text("${etiquetaClase(cc.clase)} (${cc.total})") },
                    )
                }
            }
        }
    }
}

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
                        color = LocalColoresPrecio.current.acento,
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

/** Corazón del primitivo: 40dp de área táctil, icono 20dp, sin ruido visual. */
@Composable
private fun IconoFavorito(
    activo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (activo) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (activo) "Quitar de favoritos" else "Agregar a favoritos",
            tint = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Agregar al carrito", modifier = Modifier.size(20.dp))
        }
    }
}
