package com.dusk0382.cecosesolaprecios.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repo: ProductRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val id: Long = checkNotNull(savedStateHandle["productId"])

    /**
     * Estado del producto con **tres** casos, no dos.
     *
     * Antes `producto: StateFlow<ProductEntity?>` usaba `null` para dos cosas
     * distintas: "Room todavía no emitió" y "no existe ese producto". La primera
     * emisión de Room es una consulta asíncrona sobre 518 filas, así que
     * **en cada apertura del detalle** la pantalla se quedaba un frame en blanco
     * con el título "Producto" antes de aparecer el contenido. Y si la fila
     * faltara, el resultado era un hueco permanente sin mensaje ni salida, sin
     * forma de distinguir "todavía no sé" de "no existe".
     *
     * `onStart` emite [Cargando] antes de la primera emisión de Room, así que los
     * tres estados quedan separados sin esperar un timeout para adivinar.
     */
    val estado: StateFlow<EstadoDetalle> = repo.byIdFlow(id)
        .map { producto ->
            if (producto == null) EstadoDetalle.NoEncontrado else EstadoDetalle.Listo(producto)
        }
        .onStart { emit(EstadoDetalle.Cargando) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoDetalle.Cargando)

    val esFavorito: StateFlow<Boolean> = repo.isFavoriteFlow(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** El DAO devuelve Int? (sin fila = no está en el carrito) → 0 para la UI. */
    val cantidadEnCarrito: StateFlow<Int> = repo.cartQuantityFlow(id)
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun toggleFavorite() = viewModelScope.launch { repo.toggleFavorite(id) }

    fun setQuantity(q: Int) = viewModelScope.launch { repo.setQuantity(id, q) }
}

/** Los tres estados posibles de la ficha. Ver [DetailViewModel.estado]. */
sealed interface EstadoDetalle {
    /** Room todavía no emitió. Se ve un spinner, no un hueco. */
    data object Cargando : EstadoDetalle

    /** La fila no existe. Hoy no es alcanzable (no se borran productos), pero si
     *  se alcanzara tiene que decirselo al usuario en vez de dejar un hueco. */
    data object NoEncontrado : EstadoDetalle

    data class Listo(val producto: ProductEntity) : EstadoDetalle
}
