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
import kotlinx.coroutines.withContext

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

    /**
     * Set<String> en lugar de List<String> → isFavorito() pasa de O(n) a O(1).
     * Eagerly porque los favoritos se necesitan inmediatamente al componer cada card.
     */
    private val favoritosIds: StateFlow<Set<String>> = repository.getFavoritosFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val busquedasRecientes: StateFlow<List<String>> = repository.getBusquedasRecientesFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Pipeline de filtrado optimizado:
     *
     * 1. debounce(150ms) en la query: si el usuario escribe rápido, no se filtra
     *    en cada keystroke — solo cuando deja de escribir 150ms.
     *
     * 2. El combine no incluye favoritosIds directamente, porque cambiar un favorito
     *    NO debe re-filtrar la lista de productos (el filtrado solo es por texto y
     *    por soloFavoritos flag). Los favoritos se aplican con el Set que ya está
     *    en memoria, sin re-disparar todo el pipeline.
     *
     * 3. distinctUntilChanged() evita recomposiciones cuando el resultado es igual
     *    al anterior (ej. añadir un carácter que no cambia los resultados).
     *
     * 4. flowOn(IO) mueve el filtrado fuera del hilo principal.
     */
    val productos: StateFlow<List<Producto>> = combine(
        repository.getProductosFlow(),
        _searchQuery.debounce(150),
        _soloFavoritos
    ) { productos, query, soloFavs ->
        // El filtrado pesado ocurre en IO (via flowOn más abajo)
        var result = productos
        if (soloFavs) {
            val favs = favoritosIds.value   // lectura O(1) del StateFlow ya calculado
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
        .flowOn(Dispatchers.Default)   // filtrado en hilo worker, no en Main
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ultimaActualizacion: StateFlow<Long?> = flow {
        emit(repository.getUltimaSincronizacion())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val mensajeContextual: StateFlow<String> = ultimaActualizacion
        .map { DiasUtilesHelper.getMensajeContextual(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    init {
        Log.d("Cecosesola", "🔵 MainViewModel INIT")
        refrescar(mostrarLoading = false)   // silencioso al inicio — Room ya tiene datos
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        // No lanzamos corrutina aquí — el debounce en el Flow se encarga del throttle
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

    /**
     * O(1) — el Set ya está computado en favoritosIds.
     * Seguro llamarlo desde la UI en cada recomposición.
     */
    fun isFavorito(productoId: String): Boolean = favoritosIds.value.contains(productoId)

    fun eliminarBusqueda(query: String) {
        viewModelScope.launch { repository.eliminarBusqueda(query) }
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
        }
    }
}
