package com.cecosesola.coop.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cecosesola.coop.presentation.ui.components.ProductoCard
import com.cecosesola.coop.presentation.ui.components.SearchBar
import com.cecosesola.coop.presentation.ui.components.SkeletonProductCard
import com.cecosesola.coop.presentation.ui.theme.Surf
import com.cecosesola.coop.presentation.utils.DiasUtilesHelper
import com.cecosesola.coop.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val productos           by viewModel.productos.collectAsState()
    val searchQuery         by viewModel.searchQuery.collectAsState()
    val isLoading           by viewModel.isLoading.collectAsState()
    val soloFavoritos       by viewModel.soloFavoritos.collectAsState()
    val busquedasRecientes  by viewModel.busquedasRecientes.collectAsState()
    val ultimaActualizacion by viewModel.ultimaActualizacion.collectAsState()
    val snackbarHostState   = remember { SnackbarHostState() }
    val listState           = rememberLazyListState()

    val scrolled by remember { derivedStateOf {
        listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 4
    }}

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            CecoTopBar(
                soloFavoritos       = soloFavoritos,
                onToggleFavoritos   = { viewModel.toggleSoloFavoritos() },
                ultimaActualizacion = ultimaActualizacion,
                scrolled            = scrolled
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    modifier       = Modifier.padding(16.dp),
                    shape          = MaterialTheme.shapes.medium,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor   = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor    = MaterialTheme.colorScheme.inversePrimary
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = DiasUtilesHelper.esDiaDeOperacion() && !isLoading && productos.isNotEmpty(),
                enter   = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit    = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick        = { viewModel.refrescar() },
                    shape          = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation      = FloatingActionButtonDefaults.elevation(0.dp, 2.dp, 0.dp, 0.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                SearchBar(
                    query              = searchQuery,
                    onQueryChange      = viewModel::onSearchQueryChange,
                    onSearchSubmit     = viewModel::onSearchSubmit,
                    busquedasRecientes = busquedasRecientes,
                    onEliminarBusqueda = viewModel::eliminarBusqueda,
                    modifier           = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Separador con conteo
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnimatedContent(
                        targetState = when {
                            isLoading           -> "Cargando…"
                            soloFavoritos       -> "${productos.size} favoritos"
                            searchQuery.isNotBlank() -> "${productos.size} resultados"
                            else                -> "${productos.size} productos"
                        },
                        transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(160)) },
                        label = "count"
                    ) { text ->
                        Text(
                            text  = text,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            if (isLoading && productos.isEmpty()) {
                items(7) {
                    SkeletonProductCard(
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            } else {
                items(productos, key = { it.id }) { producto ->
                    ProductoCard(
                        producto        = producto,
                        isFavorito      = viewModel.isFavorito(producto.id),
                        onFavoritoClick = { viewModel.toggleFavorito(producto.id) },
                        modifier        = Modifier
                            .animateItem()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            if (productos.isEmpty() && !isLoading) {
                item {
                    EmptyState(soloFavoritos = soloFavoritos, hasQuery = searchQuery.isNotBlank())
                }
            }
        }
    }
}

// ── Top App Bar compacta M3 ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CecoTopBar(
    soloFavoritos: Boolean,
    onToggleFavoritos: () -> Unit,
    ultimaActualizacion: Long?,
    scrolled: Boolean
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "Cecosesola",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ultimaActualizacion?.let { epoch ->
                    val hora = remember(epoch) {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epoch))
                    }
                    Text(
                        "Actualizado: $hora",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
            }
        },
        actions = {
            // Botón favoritos con estado visual claro
            IconButton(onClick = onToggleFavoritos) {
                BadgedBox(
                    badge = {
                        if (soloFavoritos) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary)
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (soloFavoritos) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (soloFavoritos) "Mostrar todos" else "Solo favoritos",
                        tint = if (soloFavoritos) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (scrolled)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
            else
                MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(soloFavoritos: Boolean, hasQuery: Boolean) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue  = 0.6f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer { this.alpha = alpha },
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when {
                        soloFavoritos && !hasQuery -> Icons.Outlined.FavoriteBorder
                        hasQuery                  -> Icons.Outlined.SearchOff
                        else                      -> Icons.Outlined.Inventory2
                    },
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = when {
                soloFavoritos && !hasQuery -> "Sin favoritos"
                hasQuery                  -> "Sin resultados"
                else                      -> "Sin productos"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = when {
                soloFavoritos && !hasQuery -> "Toca el corazón en cualquier producto para guardarlo aquí."
                hasQuery                  -> "Intenta con otro término de búsqueda."
                else                      -> "Los productos aparecerán cuando haya disponibilidad en la feria."
            },
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
