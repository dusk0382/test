package com.cecosesola.coop.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cecosesola.coop.data.repository.PreciosRepository
import com.cecosesola.coop.domain.model.Producto
import com.cecosesola.coop.presentation.utils.DiasUtilesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class MainViewModel(private val repository: PreciosRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val _soloFavoritos = MutableStateFlow(false)
    val soloFavoritos: StateFlow<Boolean> = _soloFavoritos.asStateFlow()

    // Paginación
    private val PAGE_SIZE = 30
    private val _currentPage = MutableStateFlow(0)

    private val favoritosIds: StateFlow<Set<String>> = repository.getFavoritosFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val busquedasRecientes: StateFlow<List<String>> = repository.getBusquedasRecientesFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Todos los productos filtrados
    private val productosFiltrados: StateFlow<List<Producto>> = combine(
        repository.getProductosFlow(),
        _searchQuery.debounce(150),
        _soloFavoritos
    ) { productos, query, soloFavs ->
        var result = productos
        if (soloFavs) {
            val favs = favoritosIds.value
            result = result.filter { favs.contains(it.id) }
        }
        if (query.isNotBlank()) {
            val q = query.trim()
            result = result.filter { p ->
                p.nombre.contains(q, ignoreCase = true) ||
                    (p.categoria?.contains(q, ignoreCase = true) == true)
            }
        }
        result
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Productos paginados (los que se muestran)
    val productos: StateFlow<List<Producto>> = combine(
        productosFiltrados,
        _currentPage
    ) { todos, page ->
        todos.take((page + 1) * PAGE_SIZE)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hayMasProductos: StateFlow<Boolean> = combine(
        productosFiltrados,
        productos
    ) { todos, paginados ->
        paginados.size < todos.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val productosRestantes: StateFlow<Int> = combine(
        productosFiltrados,
        productos
    ) { todos, paginados ->
        todos.size - paginados.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val ultimaActualizacion: StateFlow<Long?> = flow {
        emit(repository.getUltimaSincronizacion())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val mensajeContextual: StateFlow<String> = ultimaActualizacion
        .map { DiasUtilesHelper.getMensajeContextual(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    init {
        Log.d("Cecosesola", "🔵 MainViewModel INIT")
        refrescar(mostrarLoading = false)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        resetPaginacion()
    }

    fun onSearchSubmit(query: String) {
        viewModelScope.launch {
            repository.guardarBusqueda(query)
        }
    }

    fun toggleSoloFavoritos() {
        _soloFavoritos.value = !_soloFavoritos.value
        resetPaginacion()
    }

    fun toggleFavorito(productoId: String) {
        viewModelScope.launch {
            repository.toggleFavorito(productoId)
        }
    }

    fun isFavorito(productoId: String): Boolean = favoritosIds.value.contains(productoId)

    fun eliminarBusqueda(query: String) {
        viewModelScope.launch { repository.eliminarBusqueda(query) }
    }

    fun cargarMas() {
        if (hayMasProductos.value) {
            _currentPage.update { it + 1 }
        }
    }

    private fun resetPaginacion() {
        _currentPage.value = 0
    }

    fun refrescar(mostrarLoading: Boolean = true) {
        viewModelScope.launch {
            if (mostrarLoading) _isLoading.value = true
            val r = repository.refrescarSiEsNecesario()
            if (r.isFailure) {
                Log.e("Cecosesola", "🔴 refresh falló: ${r.exceptionOrNull()?.message}")
                _errorMessage.emit("No se pudieron actualizar los precios")
            }
            if (mostrarLoading) _isLoading.value = false
            resetPaginacion()
        }
    }
}
