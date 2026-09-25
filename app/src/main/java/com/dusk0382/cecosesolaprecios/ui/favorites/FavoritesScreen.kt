package com.dusk0382.cecosesolaprecios.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.data.repository.ProductRepository
import com.dusk0382.cecosesolaprecios.ui.catalog.RenglonProducto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repo: ProductRepository,
) : ViewModel() {
    val favoritos: StateFlow<List<ProductEntity>> = repo.favoritosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Mismo primitivo que el catálogo: stepper y corazón funcionan igual aquí. */
    val cantidades: StateFlow<Map<Long, Int>> = repo.cartQuantitiesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val favoritoIds: StateFlow<Set<Long>> = repo.favoriteIdsFlow()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun setCantidad(productId: Long, cantidad: Int) = viewModelScope.launch {
        repo.setQuantity(productId, cantidad)
    }

    fun toggleFavorito(productId: Long) = viewModelScope.launch {
        repo.toggleFavorite(productId)
    }
}

@Composable
fun FavoritesScreen(
    onOpenDetail: (Long) -> Unit,
    vm: FavoritesViewModel = hiltViewModel(),
) {
    val favoritos by vm.favoritos.collectAsStateWithLifecycle()
    val cantidades by vm.cantidades.collectAsStateWithLifecycle()
    val favoritoIds by vm.favoritoIds.collectAsStateWithLifecycle()

    if (favoritos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sin favoritos todavía", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Toca el corazón en un producto para tenerlo a mano.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        return
    }
    // Misma grilla fija de 2 que el catálogo: el primitivo se ve y se comporta
    // idéntico en ambas pantallas (DESIGN.md §4).
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(favoritos, key = { it.localId }) { p ->
            RenglonProducto(
                producto = p,
                cantidad = cantidades[p.localId] ?: 0,
                esFavorito = p.localId in favoritoIds,
                onCantidad = { vm.setCantidad(p.localId, it) },
                onFavorito = { vm.toggleFavorito(p.localId) },
                onClick = { onOpenDetail(p.localId) },
            )
        }
    }
}
