package com.dusk0382.cecosesolaprecios.ui.catalog

import android.util.Log
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

    // `asStateFlow()` en el lado lectura: exponer el MutableStateFlow dejaba que
    // cualquier composable mutara el estado de sync de la pantalla.
    private val _sincronizando = MutableStateFlow(false)
    private val _yaRefrescado = MutableStateFlow(false)

    val sincronizando: StateFlow<Boolean> = _sincronizando.asStateFlow()

    /** true cuando ya hubo al menos un intento de refresco en esta sesión. */
    val yaRefrescado: StateFlow<Boolean> = _yaRefrescado.asStateFlow()

    val total: StateFlow<Int> = repo.countFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Texto de búsqueda ya asentado: un solo temporizador de debounce para toda la
     * app, en vez de uno por consumidor.
     *
     * Antes `productos` y `conteoClases` tenían cada uno su propio
     * `debounce(220)` sobre el mismo `_busqueda`. Eso son dos temporizadores
     * vivos y **dos consultas a Room por búsqueda terminada**, sobre la misma
     * tabla, con el mismo resultado. En un Helio G25 la segunda consulta no es
     * gratis aunque la base sea de 527 filas, y el contador es justo el que se
     * lee mientras la grilla se está repintando.
     *
     * `Eagerly` a propósito: el debounce vive mientras vive el ViewModel, no
     * mientras haya suscriptores. Con `WhileSubscribed` la cadena se soltaba
     * al perder la UI y al volver reemitía el valor inicial `""`, lo que
     * disparaba una consulta de catálogo completo de más y un parpadeo de
     * resultados. Un temporizador de 220 ms en un ViewModel de Activity no
     * cuesta nada y hace el comportamiento predecible.
     */
    private val consultaAsentada: StateFlow<String> = _busqueda
        .debounce(220L)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, _busqueda.value)

    /**
     * Conteo por clase para el selector de filtros. Baymard (ver DESIGN.md §7):
     * el conteo junto a cada opción es la mejora de mayor impacto en una UI de
     * filtros, y la multi-selección evita la fricción de la selección única.
     *
     * Depende de la búsqueda: el número tiene que ser el de los resultados que
     * el usuario está viendo. Con un `GROUP BY` sobre la tabla entera, escribir
     * "leche" seguía ofreciendo "Despensa (109)" — un número que no era el
     * resultado de nada, que es justo lo que hace que una opción se descarte.
     *
     * No depende de las clases ya elegidas: si dependiera, al elegir un rubro
     * todos los demás contarían 0 y no se podría cambiar de opinión.
     */
    val conteoClases: StateFlow<List<ClaseConteo>> = consultaAsentada
        .flatMapLatest { q -> repo.conteoPorClaseFlow(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Cantidad por producto en el carrito, para el stepper de la tarjeta. */
    val cantidades: StateFlow<Map<Long, Int>> = repo.cartQuantitiesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Ids de favoritos para el corazón de la tarjeta. */
    val favoritos: StateFlow<Set<Long>> = repo.favoriteIdsFlow()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /**
     * Resultados de la grilla. El debounce va solo sobre la búsqueda: cambiar un
     * filtro o el orden tiene que consultarse ya, no 220 ms después, porque son
     * toques a propósito y no tecleo.
     */
    val productos: StateFlow<List<ProductEntity>> = combine(
        consultaAsentada,
        _clases,
        _orden,
    ) { q, clases, ord -> Triple(q, clases, ord) }
        .flatMapLatest { (q, clases, ord) ->
            repo.searchFlow(q, clases.sorted(), ord.key)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** ¿Hay algo más que la búsqueda afectando el conjunto de resultados? Lo usa
     *  el estado vacío para decir la causa correcta y ofrecer la salida. */
    val hayFiltros: StateFlow<Boolean> = _clases
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun limpiarBusqueda() {
        _busqueda.value = ""
    }

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
     *  (puede tardar 40s; no debe bloquear la interacción).
     *
     *  El `finally` no es cosmético: sin él, si `requestEnrich()` lanzaba, el flag
     *  `sincronizando` se quedaba en `true` para siempre y el pull-to-refresh
     *  quedaba colgado con el FAB escondido sin forma de recuperarlo. */
    fun refresh() {
        if (_sincronizando.value) return
        viewModelScope.launch {
            _sincronizando.value = true
            try {
                runCatching { repo.syncBase() }
                    .onFailure { Log.w("CecoSync", "refresh: syncBase fallo: ${it.message}", it) }
                repo.requestEnrich()
                _yaRefrescado.value = true
            } finally {
                _sincronizando.value = false
            }
        }
    }
}
