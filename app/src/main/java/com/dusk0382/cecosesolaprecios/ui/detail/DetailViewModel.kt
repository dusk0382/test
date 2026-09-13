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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repo: ProductRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val id: Long = checkNotNull(savedStateHandle["productId"])

    val producto: StateFlow<ProductEntity?> = repo.byIdFlow(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val esFavorito: StateFlow<Boolean> = repo.isFavoriteFlow(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val cantidadEnCarrito: StateFlow<Int> = repo.cartQuantityFlow(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun toggleFavorite() = viewModelScope.launch { repo.toggleFavorite(id) }

    fun setQuantity(q: Int) = viewModelScope.launch { repo.setQuantity(id, q) }
}
