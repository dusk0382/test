package com.dusk0382.cecosesolaprecios.ui.catalog

import android.util.Log
import androidx.lifecycle.SavedStateHandle
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

/**
 * Clave estable de un conjunto de resultados. La usan el ViewModel y la pantalla
 * para saber si la lista que se está mostrando corresponde a la búsqueda, los
 * filtros y el orden actuales.
 *
 * Sin ella, restaurar el scroll era adivinar: se aplicaba el índice contra la
 * lista anterior (la que todavía no había sido reemplazada por Room) y caía en
 * otro producto, o se recortaba al último.
 */
internal fun claveCatalogo(consulta: String, clases: Set<String>, orden: Orden): String =
    listOf(
        consulta.trim().lowercase(),
        clases.sorted().joinToString(","),
        orden.key,
    ).joinToString("|")

/** La lista de la grilla junto con la clave del estado que la produjo. */
data class ResultadosCatalogo(
    val clave: String,
    val productos: List<ProductEntity>,
    /**
     * true solo en el valor inicial del `stateIn`: la primera consulta a Room
     * todavía no respondió. Sin esta señal, el primer frame de la app (y el de
     * una vuelta después de que el proceso muera) mostraba "Aún no hay precios
     * cargados" —una afirmación falsa— y ofrecía sincronizar datos que sí
     * estaban en la base.
     */
    val inicial: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repo: ProductRepository,
    private val estadoGuardado: SavedStateHandle,
) : ViewModel() {

    /**
     * Texto de búsqueda. La UI mantiene el campo y manda cada tecla; acá el
     * debounce decide cuándo se reconsulta. Es la fuente única para la grilla y
     * para los conteos de rubros.
     *
     * Vive también en el `SavedStateHandle`: el campo era `rememberSaveable` y
     * el ViewModel no, así que al morir el proceso el texto volvía y la consulta
     * arrancaba en "". La pantalla quedaba mostrando un listado que no
     * correspondía a lo escrito hasta que un `LaunchedEffect` lo corregía, y los
     * filtros y el orden se perdían del todo. Con el estado completo acá, la
     * primera consulta ya se hace con lo que el usuario dejó.
     */
    private val _busqueda = MutableStateFlow(
        estadoGuardado.get<String>(CLAVE_BUSQUEDA).orEmpty(),
    )

    /** Clases (rubros) seleccionadas: multi-selección, OR entre sí. */
    private val _clases = MutableStateFlow(
        estadoGuardado.get<ArrayList<String>>(CLAVE_CLASES).orEmpty().toSet(),
    )
    val clasesSel: StateFlow<Set<String>> = _clases.asStateFlow()

    private val _orden = MutableStateFlow(
        Orden.entries.firstOrNull { it.key == estadoGuardado.get<String>(CLAVE_ORDEN) }
            ?: Orden.NOMBRE,
    )
    val orden: StateFlow<Orden> = _orden.asStateFlow()

    // `asStateFlow()` en el lado lectura: exponer el MutableStateFlow dejaba que
    // cualquier composable mutara el estado de sync de la pantalla.
    private val _sincronizando = MutableStateFlow(false)

    val sincronizando: StateFlow<Boolean> = _sincronizando.asStateFlow()

    /**
     * Mensaje de un refresh que falló. La pantalla lo muestra en un snackbar y
     * lo consume; sin esto, `refresh()` fallaba en silencio y el usuario no
     * tenía forma de saber que la lista seguía siendo la vieja.
     */
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    /**
     * Total de productos en la base. `null` hasta que Room responde: con un 0
     * inicial, una lista vacía que llega antes que el conteo hacía que la
     * pantalla afirmara "Aún no hay precios cargados" —una mentira— en cada
     * búsqueda sin resultados.
     */
    val total: StateFlow<Int?> = repo.countFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
     * La búsqueda **ya asentada**, expuesta para que la UI pueda decidir cuándo
     * cambió de verdad el conjunto de resultados.
     *
     * La UI escribe la búsqueda al instante en cada tecla, pero la consulta a Room
     * va con debounce. Con la búsqueda cruda como señal, el catálogo "?ve un cambio
     * de conjunto" 220 ms antes de que la lista sea la nueva, y cualquier
     * posicionamiento de scroll se aplica contra la lista equivocada. Como el
     * debounce ya está hecho acá y es el mismo que usa [productos], esta es la
     * única señal con la que la clave y la lista cambian juntas.
     */
    val consultaSettled: StateFlow<String> = consultaAsentada

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
     * true si el último enrich exitoso tiene más de 12 h (o nunca hubo uno).
     * El catálogo lo usa para avisar que los precios solidarios en USD pueden
     * estar viejos; al terminar un enrich, `API_SYNC_AT` cambia y el aviso se va.
     */
    val enrichViejo: StateFlow<Boolean> = repo.apiSyncAtFlow()
        .map { ultimo -> ultimo == null || System.currentTimeMillis() - ultimo > 12L * 60 * 60 * 1000 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Resultados de la grilla **y la clave que los produjo**. El debounce va solo
     * sobre la búsqueda: cambiar un filtro o el orden tiene que consultarse ya, no
     * 220 ms después, porque son toques a propósito y no tecleo.
     *
     * La clave permite saber cuándo la lista que está en pantalla es la del
     * estado actual. Es lo que hace que el scroll se restaure contra la lista
     * nueva y no contra la anterior, que todavía puede estar mostrándose
     * mientras Room responde.
     */
    val resultados: StateFlow<ResultadosCatalogo> = combine(
        consultaAsentada,
        _clases,
        _orden,
    ) { q, clases, ord -> Triple(q, clases, ord) }
        .flatMapLatest { (q, clases, ord) ->
            val clave = claveCatalogo(q, clases, ord)
            repo.searchFlow(q, clases.sorted(), ord.key)
                .map { ResultadosCatalogo(clave, it) }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            // La clave vacía no puede ser la de ningún estado real (`claveCatalogo`
            // siempre arma algo con los separadores), así que la pantalla sabe que
            // esta lista todavía no corresponde a la consulta actual.
            ResultadosCatalogo(clave = "", productos = emptyList(), inicial = true),
        )

    /** Valor inicial del campo (la UI es la dueña después de esto). */
    fun busquedaInicial(): String = _busqueda.value

    fun onBusquedaChange(v: String) {
        _busqueda.value = v
        estadoGuardado[CLAVE_BUSQUEDA] = v
    }

    fun toggleClase(clase: String) {
        val nuevas = if (clase in _clases.value) _clases.value - clase else _clases.value + clase
        _clases.value = nuevas
        estadoGuardado[CLAVE_CLASES] = ArrayList(nuevas)
    }

    fun limpiarFiltros() {
        _clases.value = emptySet()
        estadoGuardado[CLAVE_CLASES] = ArrayList<String>()
    }

    fun onOrdenClick(o: Orden) {
        _orden.value = o
        estadoGuardado[CLAVE_ORDEN] = o.key
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
     *  quedaba colgado sin forma de recuperarlo.
     *
     *  Cada paso se captura por separado para poder decir qué falló: sin red no
     *  se actualizó nada; con la base actualizada pero el worker sin encolar, la
     *  lista vieja ya sirve y solo se perdió el enriquecimiento. */
    fun refresh() {
        if (_sincronizando.value) return
        viewModelScope.launch {
            _sincronizando.value = true
            try {
                val sync = runCatching { repo.syncBase() }
                sync.exceptionOrNull()?.let {
                    Log.w("CecoSync", "refresh: syncBase fallo: ${it.message}", it)
                }
                val enrich = if (sync.isSuccess) runCatching { repo.requestEnrich() } else null
                enrich?.exceptionOrNull()?.let {
                    Log.w("CecoSync", "refresh: requestEnrich fallo: ${it.message}", it)
                }
                _mensaje.value = when {
                    sync.isFailure -> "No se pudo actualizar la lista. Revisá tu conexión."
                    enrich?.isFailure == true ->
                        "La lista se actualizó, pero no se pudo iniciar el enriquecimiento."
                    else -> null
                }
            } finally {
                _sincronizando.value = false
            }
        }
    }

    /** La pantalla ya mostró el mensaje: no debe reaparecer al recomponer. */
    fun consumirMensaje() {
        _mensaje.value = null
    }

    private companion object {
        const val CLAVE_BUSQUEDA = "busqueda"
        const val CLAVE_CLASES = "clases"
        const val CLAVE_ORDEN = "orden"
    }
}
