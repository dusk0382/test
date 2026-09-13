package com.dusk0382.cecosesolaprecios.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Orden(val label: String, val key: String) {
    NOMBRE("A–Z", "nombre"),
    PRECIO_ASC("Precio ↑", "precio_asc"),
    PRECIO_DESC("Precio ↓", "precio_desc"),
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repo: ProductRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _categoria = MutableStateFlow<String?>(null)
    val categoria: StateFlow<String?> = _categoria

    private val _orden = MutableStateFlow(Orden.NOMBRE)
    val orden: StateFlow<Orden> = _orden

    val sincronizando = MutableStateFlow(false)

    /** true cuando ya hubo al menos un intento de refresco en esta sesión */
    val yaRefrescado = MutableStateFlow(false)

    val categorias: StateFlow<List<String>> = repo.categoriasFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val total: StateFlow<Int> = repo.countFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val fechaRepo: StateFlow<String?> = MutableStateFlow<String?>(null).also { f ->
        viewModelScope.launch { f.value = repo.ultimaFechaRepo() }
    }

    val productos: StateFlow<List<ProductEntity>> = combine(
        _query.debounce(220L).distinctUntilChanged(),
        _categoria,
        _orden,
    ) { q, cat, ord -> Triple(q, cat, ord) }
        .flatMapLatest { (q, cat, ord) -> repo.searchFlow(q, cat, ord.key) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(v: String) { _query.value = v }
    fun onCategoriaClick(c: String?) { _categoria.value = c }
    fun onOrdenClick(o: Orden) { _orden.value = o }

    /** Refresco manual: base sí o sí (CDN, ~200ms); enriquecimiento en worker
     *  (puede tardar 40s; no debe bloquear la interacción). */
    fun refresh() {
        if (sincronizando.value) return
        viewModelScope.launch {
            sincronizando.value = true
            runCatching { repo.syncBase() }
            repo.requestEnrich()
            sincronizando.value = false
            yaRefrescado.value = true
        }
    }
}
