package com.cecosesola.coop.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cecosesola.coop.data.repository.PreciosRepository
import com.cecosesola.coop.domain.model.Producto
import com.cecosesola.coop.presentation.utils.DiasUtilesHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(private val repository: PreciosRepository) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()
    
    private val _soloFavoritos = MutableStateFlow(false)
    val soloFavoritos: StateFlow<Boolean> = _soloFavoritos.asStateFlow()
    
    private val favoritosIds = repository.getFavoritosFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    val busquedasRecientes = repository.getBusquedasRecientesFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    val productos: StateFlow<List<Producto>> = combine(
        repository.getProductosFlow(),
        _searchQuery,
        _soloFavoritos,
        favoritosIds
    ) { productos, query, soloFavs, favs ->
        var result = productos
        if (soloFavs) {
            result = result.filter { favs.contains(it.id) }
        }
        if (query.isNotBlank()) {
            result = result.filter { p ->
                p.nombre.contains(query, true) ||
                (p.categoria?.contains(query, true) ?: false)
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val ultimaActualizacion: StateFlow<Long?> = flow { 
        emit(repository.getUltimaSincronizacion()) 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val mensajeContextual: StateFlow<String> = ultimaActualizacion.map { 
        DiasUtilesHelper.getMensajeContextual(it) 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Cargando...")
    
    init { 
        Log.d("Cecosesola", "🔵 MainViewModel INIT")
        refrescar(false) 
    }
    
    fun onSearchQueryChange(query: String) { 
        _searchQuery.value = query 
    }
    
    fun onSearchSubmit(query: String) {
        viewModelScope.launch {
            repository.guardarBusqueda(query)
        }
    }
    
    fun toggleSoloFavoritos() {
        _soloFavoritos.value = !_soloFavoritos.value
    }
    
    fun toggleFavorito(productoId: String) {
        viewModelScope.launch {
            repository.toggleFavorito(productoId)
        }
    }
    
    fun isFavorito(productoId: String): Boolean {
        return favoritosIds.value.contains(productoId)
    }
    
    fun eliminarBusqueda(query: String) {
        viewModelScope.launch {
            repository.eliminarBusqueda(query)
        }
    }
    
    fun refrescar(mostrarLoading: Boolean = true) {
        Log.d("Cecosesola", "🔵 refrescar() llamado")
        viewModelScope.launch {
            if (mostrarLoading) _isLoading.value = true
            val r = repository.refrescarSiEsNecesario()
            if (r.isFailure) {
                Log.e("Cecosesola", "🔴 refresh falló")
                _errorMessage.emit("No se pudieron actualizar los precios")
            }
            if (mostrarLoading) _isLoading.value = false
        }
    }
}
