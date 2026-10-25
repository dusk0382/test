package com.dusk0382.cecosesolaprecios.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.dusk0382.cecosesolaprecios.ui.theme.Espacio
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.data.repository.ProductRepository
import com.dusk0382.cecosesolaprecios.ui.common.RenglonProducto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repo: ProductRepository,
) : ViewModel() {
    private val _cargado = MutableStateFlow(false)

    /** True desde la primera emisión de Room: la pantalla no muestra vacío antes. */
    val cargado: StateFlow<Boolean> = _cargado.asStateFlow()

    val favoritos: StateFlow<List<ProductEntity>> = repo.favoritosFlow()
        .onEach { _cargado.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Mismo primitivo que el catálogo: stepper y corazón funcionan igual aquí. */
    val cantidades: StateFlow<Map<Long, Int>> = repo.cartQuantitiesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

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
    val cargado by vm.cargado.collectAsStateWithLifecycle()
    val cantidades by vm.cantidades.collectAsStateWithLifecycle()
    val abrirDetalle by rememberUpdatedState(onOpenDetail)
    var ordenPorNombre by rememberSaveable { mutableStateOf(false) }
    val lista = remember(favoritos, ordenPorNombre) {
        if (ordenPorNombre) favoritos.sortedBy { it.nombreNormalizado } else favoritos
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Favoritos",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Espacio.l, vertical = Espacio.s),
            )
            // Solo con favoritos: en el vacío no hay nada que ordenar.
            if (favoritos.isNotEmpty()) {
                TextButton(
                    onClick = { ordenPorNombre = !ordenPorNombre },
                    // El texto visible dice el orden actual, no la acción: sin
                    // esto TalkBack anunciaría "A–Z" como si fuera un botón de
                    // letra suelta. El estado va aparte para no perderlo.
                    modifier = Modifier.semantics {
                        contentDescription = "Cambiar el orden de favoritos"
                        stateDescription = if (ordenPorNombre) {
                            "Orden actual: A–Z"
                        } else {
                            "Orden actual: recientes primero"
                        }
                    },
                ) {
                    Text(if (ordenPorNombre) "A–Z" else "Recientes")
                }
            }
        }
        when {
            // Room todavía no respondió: un hueco honesto, no un vacío falso.
            !cargado -> Box(Modifier.fillMaxWidth().weight(1f))

            favoritos.isEmpty() -> Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sin favoritos todavía", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Toca el corazón en un producto para tenerlo a mano.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Espacio.xs),
                    )
                }
            }

            // Misma grilla fija de 2 que el catálogo: el primitivo se ve y se
            // comporta idéntico en ambas pantallas (DESIGN.md §4).
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(Espacio.m),
                horizontalArrangement = Arrangement.spacedBy(Espacio.m),
                verticalArrangement = Arrangement.spacedBy(Espacio.m),
            ) {
                items(lista, key = { it.localId }) { p ->
                    val onCantidad: (Int) -> Unit = remember(p.localId) {
                        { q -> vm.setCantidad(p.localId, q) }
                    }
                    val onFavorito: () -> Unit = remember(p.localId) {
                        { vm.toggleFavorito(p.localId) }
                    }
                    val onClick: () -> Unit = remember(p.localId) {
                        { abrirDetalle(p.localId) }
                    }
                    RenglonProducto(
                        producto = p,
                        cantidad = cantidades[p.localId] ?: 0,
                        // Esta pantalla ya es la lista de favoritos: todos los
                        // ítems lo son. Evita una segunda consulta a Room.
                        esFavorito = true,
                        onCantidad = onCantidad,
                        onFavorito = onFavorito,
                        onClick = onClick,
                    )
                }
            }
        }
    }
}
