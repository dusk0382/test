package com.dusk0382.cecosesolaprecios.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.dusk0382.cecosesolaprecios.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.ui.common.DeltaBadge
import com.dusk0382.cecosesolaprecios.ui.common.PriceText
import com.dusk0382.cecosesolaprecios.ui.common.precioMostrado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    vm: CatalogViewModel,
    onOpenDetail: (Long) -> Unit,
    onScan: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val productos by vm.productos.collectAsStateWithLifecycle()
    val categorias by vm.categorias.collectAsStateWithLifecycle()
    val categoriaSel by vm.categoria.collectAsStateWithLifecycle()
    val orden by vm.orden.collectAsStateWithLifecycle()
    val sincronizando by vm.sincronizando.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val yaRefrescado by vm.yaRefrescado.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        // Cabecera propia en vez de TopAppBar: un TopAppBar con scrollBehavior
        // añade estado y mediciones que en la A53 se notan; esto es una Row.
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onSettings) {
                Icon(painterResource(R.drawable.ic_settings), "Ajustes")
            }
        }

        SearchField(query, vm::onQueryChange, sincronizando, onScan)

        if (categorias.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(listOf(null) + categorias) { cat ->
                    FilterChip(
                        selected = categoriaSel == cat,
                        onClick = { vm.onCategoriaClick(cat) },
                        label = { Text(cat?.replaceFirstChar { it.uppercase() } ?: "Todas") },
                    )
                }
            }
        }

        // orden: fila compacta de chips
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Orden.entries.forEach { o ->
                FilterChip(
                    selected = orden == o,
                    onClick = { vm.onOrdenClick(o) },
                    label = { Text(o.label, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = sincronizando,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (productos.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (total == 0) {
                            "Aún no hay precios cargados.\nJala hacia abajo para sincronizar."
                        } else {
                            if (yaRefrescado) "Sin resultados para tu búsqueda." else "Cargando precios…"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(170.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                ) {
                    // alias: `items` de LazyListScope (LazyRow, arriba) y el de
                    // LazyStaggeredGridScope son dos extensiones distintas con el
                    // mismo nombre; sin alias el de la grid no resuelve.
                    staggeredItems(productos, key = { it.localId }) { p ->
                        ProductoCard(p, onClick = { onOpenDetail(p.localId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, busy: Boolean, onScan: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = onChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    "Buscar producto…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (busy) Spacer(Modifier.width(8.dp))
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(4.dp))
                IconButton(onScan, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painterResource(R.drawable.ic_qr_scanner),
                        "Escanear código",
                        Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Card flat con outline: sombras elevadas cuestan en la Mali-G52. */
@Composable
fun ProductoCard(p: ProductEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            AsyncImage(
                model = p.imagenUrl ?: p.imagenGrandeUrl,
                contentDescription = p.nombre,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(110.dp).padding(8.dp),
            )
            Text(
                text = p.nombre,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            )
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val (precio, moneda) = p.precioMostrado()
                PriceText(precio, prefix = moneda, style = MaterialTheme.typography.titleMedium)
                DeltaBadge(p.precioCec, p.precioAnteriorCec) // CEC↔CEC: misma moneda
            }
            if (p.categoria != null) {
                Text(
                    text = p.categoria.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
                )
            }
        }
    }
}
