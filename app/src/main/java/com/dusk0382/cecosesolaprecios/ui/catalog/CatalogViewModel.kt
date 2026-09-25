package com.dusk0382.cecosesolaprecios.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk0382.cecosesolaprecios.data.local.ClaseConteo
import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Orden(val label: String, val key: String) {
    NOMBRE("A–Z", "nombre"),
    PRECIO_ASC("Menor precio", "precio_asc"),
    PRECIO_DESC("Mayor precio", "precio_desc"),
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repo: ProductRepository,
) : ViewModel() {

    /**
     * Texto de búsqueda. La UI es dueña del campo (el tecleo no recompone la
     * pantalla completa — DESIGN.md §8.2); aquí sólo llega el valor final y el
     * debounce decide cuándo reconsultar.
     */
    private val _busqueda = MutableStateFlow("")

    /** Clases (rubros) seleccionadas: multi-selección, OR entre sí. */
    private val _clases = MutableStateFlow<Set<String>>(emptySet())
    val clasesSel: StateFlow<Set<String>> = _clases.asStateFlow()

    private val _orden = MutableStateFlow(Orden.NOMBRE)
    val orden: StateFlow<Orden> = _orden.asStateFlow()

    val sincronizando = MutableStateFlow(false)

    /** true cuando ya hubo al menos un intento de refresco en esta sesión. */
    val yaRefrescado = MutableStateFlow(false)

    val total: StateFlow<Int> = repo.countFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Conteo por clase para el selector de filtros. Baymard (ver DESIGN.md §7):
     * el conteo junto a cada opción es la mejora de mayor impacto en una UI de
     * filtros, y la multi-selección evita la fricción de la selección única.
     */
    val conteoClases: StateFlow<List<ClaseConteo>> = repo.conteoPorClaseFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Cantidad por producto en el carrito, para el stepper de la tarjeta. */
    val cantidades: StateFlow<Map<Long, Int>> = repo.cartQuantitiesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Ids de favoritos para el corazón de la tarjeta. */
    val favoritos: StateFlow<Set<Long>> = repo.favoriteIdsFlow()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val productos: StateFlow<List<ProductEntity>> = combine(
        _busqueda.debounce(220L).distinctUntilChanged(),
        _clases,
        _orden,
    ) { q, clases, ord -> Triple(q, clases, ord) }
        .flatMapLatest { (q, clases, ord) ->
            repo.searchFlow(q, clases.sorted(), ord.key)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Valor inicial del campo (la UI es la dueña después de esto). */
    fun busquedaInicial(): String = _busqueda.value

    fun onBusquedaChange(v: String) {
        _busqueda.value = v
    }

    fun toggleClase(clase: String) {
        _clases.value = if (clase in _clases.value) _clases.value - clase else _clases.value + clase
    }

    fun limpiarFiltros() {
        _clases.value = emptySet()
    }

    fun onOrdenClick(o: Orden) {
        _orden.value = o
    }

    fun setCantidad(productId: Long, cantidad: Int) = viewModelScope.launch {
        repo.setQuantity(productId, cantidad)
    }

    fun toggleFavorito(productId: Long) = viewModelScope.launch {
        repo.toggleFavorite(productId)
    }

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
