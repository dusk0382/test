package com.dusk0382.cecosesolaprecios.ui.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dusk0382.cecosesolaprecios.R
import com.dusk0382.cecosesolaprecios.data.local.ClaseConteo
import com.dusk0382.cecosesolaprecios.domain.Rubro
import com.dusk0382.cecosesolaprecios.domain.etiquetaVisible
import com.dusk0382.cecosesolaprecios.ui.common.LocalUsdPrecio
import com.dusk0382.cecosesolaprecios.ui.common.RenglonProducto
import com.dusk0382.cecosesolaprecios.ui.theme.Espacio
import kotlinx.coroutines.flow.StateFlow

/**
 * Catálogo según DESIGN.md §7: buscador + escáner, filtros con badge y ajustes
 * en la cabecera. Sin filas de chips de categoría: los ~100 tags de la API no
 * son navegables (los reemplaza la clasificación medida de `domain/Rubros.kt`).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CatalogScreen(
    vm: CatalogViewModel,
    onOpenDetail: (Long) -> Unit,
    onScan: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val resultados by vm.resultados.collectAsStateWithLifecycle()
    val productos = resultados.productos
    val clasesSel by vm.clasesSel.collectAsStateWithLifecycle()
    val orden by vm.orden.collectAsStateWithLifecycle()
    val sincronizando by vm.sincronizando.collectAsStateWithLifecycle()
    val cantidades by vm.cantidades.collectAsStateWithLifecycle()
    val favoritos by vm.favoritos.collectAsStateWithLifecycle()
    // Derivado del estado que ya se lee: evita otro StateFlow y otra emisión
    // por cada toggle de rubro.
    val hayFiltros = clasesSel.isNotEmpty()
    val mensaje by vm.mensaje.collectAsStateWithLifecycle()

    // El callback de navegación puede cambiar sin que cambie el producto. Se
    // guarda el último valor y las lambdas de cada tarjeta se memorizan por
    // `localId`: sin esto, cada recomposición de la pantalla creaba tres lambdas
    // nuevas por ítem y forzaba a recomponer tarjetas que no cambiaron.
    val abrirDetalle by rememberUpdatedState(onOpenDetail)

    // Un refresh que falla no puede quedar en silencio: el snackbar dice qué se
    // actualizó y qué no. Se consume al mostrarlo para no repetirlo al volver.
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbar.showSnackbar(it)
            vm.consumirMensaje()
        }
    }

    // El texto del campo vive acá y se guarda para sobrevivir a la muerte de
    // proceso; al ViewModel llega cada tecla y su debounce decide cuándo se
    // reconsulta. `query` es la verdad inmediata; `consultaSettled` es la que
    // produjo la lista que se está viendo.
    var query by rememberSaveable { mutableStateOf(vm.busquedaInicial()) }
    var filtrosAbiertos by rememberSaveable { mutableStateOf(false) }

    // `rememberSaveable` sobrevive a la muerte de proceso, el ViewModel no: al
    // volver el campo mostraba la búsqueda restaurada mientras la grilla
    // consultaba con `_busqueda = ""`. El usuario veía un listado que no
    // correspondía a lo que había escrito, y no había forma de saber por qué.
    // En un Helio G25 de 2–3 GB el recorte de memoria es rutinario, no una
    // excepción, así que esto no es un caso de borde.
    LaunchedEffect(Unit) {
        if (query != vm.busquedaInicial()) vm.onBusquedaChange(query)
    }

    // El scroll depende de dos cosas distintas: al empezar a buscar, los
    // resultados arrancan arriba; al borrar la búsqueda, el usuario vuelve al
    // punto donde estaba antes de escribir. La posición previa es `saveable`
    // porque la pantalla se descompone al abrir una ficha: con un `remember` se
    // perdía y al volver del detalle limpiar la búsqueda mandaba al principio.
    val gridState = rememberLazyGridState()
    val consultaSettled by vm.consultaSettled.collectAsStateWithLifecycle()

    // `resultados.clave` dice qué lista está en pantalla; `claveDeseada`, la que
    // corresponde a la consulta/filtros/orden actuales. Solo se restaura el
    // scroll cuando coinciden: aplicar un índice contra la lista anterior cae en
    // otro producto, que era el otro síntoma.
    val claveDeseada = remember(consultaSettled, clasesSel, orden) {
        claveCatalogo(consultaSettled, clasesSel, orden)
    }
    val claveFiltros = remember(clasesSel, orden) {
        claveCatalogo("", clasesSel, orden)
    }

    var posicionAntesDeBuscar by rememberSaveable { mutableStateOf<Int?>(null) }
    var ultimaConsulta by rememberSaveable { mutableStateOf(consultaSettled) }
    var claveFiltrosAnterior by rememberSaveable { mutableStateOf(claveFiltros) }
    var objetivoPendiente by rememberSaveable { mutableStateOf<Int?>(null) }

    // La primera evaluación no decide nada. Si el proceso murió con una búsqueda
    // activa, `consultaSettled` arranca en "" (el ViewModel es nuevo) mientras
    // `ultimaConsulta` se restaura con la búsqueda: sin esta guarda, ese salto se
    // leería como "el usuario borró" y consumiría la posición previa.
    var primeraEvaluacion by remember { mutableStateOf(true) }

    LaunchedEffect(consultaSettled) {
        if (primeraEvaluacion) {
            primeraEvaluacion = false
            ultimaConsulta = consultaSettled
            return@LaunchedEffect
        }
        if (ultimaConsulta == consultaSettled) return@LaunchedEffect
        val decision = decidirScrollBusqueda(
            consultaAnterior = ultimaConsulta,
            consultaNueva = consultaSettled,
            indiceActual = gridState.firstVisibleItemIndex,
            posicionPrevia = posicionAntesDeBuscar,
        )
        ultimaConsulta = consultaSettled
        posicionAntesDeBuscar = decision.posicionPrevia
        decision.destino?.let { objetivoPendiente = it }
    }

    // Filtros y orden también cambian el conjunto: se empieza arriba. No se
    // guarda una posición por combinación de filtros: eso hacía que tocar un
    // chip devolviera al usuario a un índice de otra visita.
    LaunchedEffect(claveFiltros) {
        if (claveFiltrosAnterior != claveFiltros) {
            claveFiltrosAnterior = claveFiltros
            objetivoPendiente = 0
        }
    }

    // El destino se aplica recién cuando la lista es la del estado actual. Antes
    // se aplicaba contra `productos.size` de la lista vieja: al limpiar una
    // búsqueda con muchos resultados, el índice previo quedaba pendiente y
    // terminaba aplicado sobre otro conjunto.
    LaunchedEffect(objetivoPendiente, resultados.clave, productos.size) {
        val objetivo = objetivoPendiente ?: return@LaunchedEffect
        if (resultados.clave != claveDeseada) return@LaunchedEffect
        if (productos.isEmpty()) {
            objetivoPendiente = null
            return@LaunchedEffect
        }
        gridState.scrollToItem(objetivo.coerceIn(0, productos.lastIndex))
        objetivoPendiente = null
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // — Cabecera: título + escáner + filtros (con badge) + ajustes —
            Row(
                Modifier.fillMaxWidth().padding(start = Espacio.l, end = Espacio.xs, top = Espacio.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Cecosesola",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                // El escáner vive en la cabecera y no en un FAB: abajo a la
                // derecha tapaba el stepper de la última tarjeta.
                IconButton(onScan) {
                    Icon(painterResource(R.drawable.ic_qr_scanner), "Escanear código de barras")
                }
                IconButton(onClick = { filtrosAbiertos = true }) {
                    BadgedBox(
                        badge = {
                            if (clasesSel.isNotEmpty()) {
                                // `primary` y no el `error` por defecto del Badge:
                                // un conteo de filtros no es un error, y el rojo
                                // metía un segundo acento en la pantalla.
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("${clasesSel.size}")
                                }
                            }
                        },
                    ) {
                        Icon(painterResource(R.drawable.ic_filter), "Filtros")
                    }
                }
                IconButton(onSettings) {
                    Icon(painterResource(R.drawable.ic_settings), "Ajustes")
                }
            }

            // — Buscador: icono de búsqueda, texto, limpiar —
            CampoBusqueda(
                // Se pasa un lector, no el String: así la lectura del estado
                // ocurre dentro del campo y cada tecla no recompone la pantalla
                // entera (cabecera, hoja, grilla y scroll).
                query = { query },
                onChange = { query = it; vm.onBusquedaChange(it) },
                modifier = Modifier.padding(horizontal = Espacio.l, vertical = Espacio.s),
            )

            // Los precios solidarios (USD) pueden ser viejos aunque la lista en Bs
            // esté al día. El aviso lee `LocalUsdPrecio` y el flujo dentro del
            // hijo: un cambio de moneda no recompone el catálogo entero.
            AvisoPrecioSolidarioViejo(
                enrichViejoFlow = vm.enrichViejo,
                onActualizar = vm::refresh,
            )

            // — Filtros activos como chips descartables, sólo mientras existan —
            if (clasesSel.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Espacio.l),
                    horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                ) {
                    lazyItems(clasesSel.sorted()) { clase ->
                        FilterChip(
                            selected = true,
                            onClick = { vm.toggleClase(clase) },
                            label = { Text(etiquetaClase(clase)) },
                        )
                    }
                }
            }

            // — Contenido: estado vacío ↔ grilla, con crossfade (no corte seco) —
            PullToRefreshBox(
                isRefreshing = sincronizando,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                // `resultados.inicial` significa que Room todavía no respondió:
                // en ese frame no se puede afirmar "no hay precios". Al no montar
                // el Crossfade hasta tener la lista real, la primera carga no
                // anima ni muestra un estado vacío falso.
                if (resultados.inicial) {
                    Box(Modifier.fillMaxSize())
                } else {
                    Crossfade(
                        targetState = productos.isEmpty(),
                        animationSpec = tween(150),
                        label = "catalogo-estado",
                        modifier = Modifier.fillMaxSize(),
                    ) { vacio ->
                        if (vacio) {
                            // El total se recolecta solo en esta rama: con la
                            // grilla llena no hace falta el COUNT y su emisión
                            // no recompone la pantalla. Mientras sea null, una
                            // lista vacía no prueba que la base esté vacía.
                            val total by vm.total.collectAsStateWithLifecycle()
                            if (total == null) {
                                Box(Modifier.fillMaxSize())
                            } else {
                                EstadoVacio(
                                    total = total!!,
                                    query = consultaSettled,
                                    esperando = query.trim() != consultaSettled.trim(),
                                    conFiltros = hayFiltros,
                                    onQuitarBusqueda = { query = ""; vm.onBusquedaChange("") },
                                    onLimpiarFiltros = vm::limpiarFiltros,
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                state = gridState,
                                contentPadding = PaddingValues(
                                    start = Espacio.m,
                                    top = Espacio.m,
                                    end = Espacio.m,
                                    bottom = Espacio.xl,
                                ),
                                horizontalArrangement = Arrangement.spacedBy(Espacio.m),
                                verticalArrangement = Arrangement.spacedBy(Espacio.m),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(productos, key = { it.localId }) { p ->
                                    val onCantidad: (Int) -> Unit = remember(p.localId) {
                                        { q -> vm.setCantidad(p.localId, q) }
                                    }
                                    val onFavorito: () -> Unit = remember(p.localId) {
                                        { vm.toggleFavorito(p.localId) }
                                    }
                                    val onClick: () -> Unit = remember(p.localId) {
                                        { abrirDetalle(p.localId) }
                                    }
                                    // Sin fades: DESIGN §3.8 prohíbe animar la
                                    // entrada de listas y en un G25 los ítems que
                                    // aparecen/desaparecen con alpha son trabajo
                                    // extra. La colocación sigue animando.
                                    val itemModifier = remember(p.localId) {
                                        Modifier.animateItem(
                                            fadeInSpec = null,
                                            fadeOutSpec = null,
                                        )
                                    }
                                    RenglonProducto(
                                        producto = p,
                                        cantidad = cantidades[p.localId] ?: 0,
                                        esFavorito = p.localId in favoritos,
                                        onCantidad = onCantidad,
                                        onFavorito = onFavorito,
                                        onClick = onClick,
                                        modifier = itemModifier,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).padding(Espacio.l),
        )
    }

    if (filtrosAbiertos) {
        HojaFiltros(
            conteoClasesFlow = vm.conteoClases,
            clasesSel = clasesSel,
            orden = orden,
            onOrden = vm::onOrdenClick,
            onToggleClase = vm::toggleClase,
            onLimpiar = vm::limpiarFiltros,
            onCerrar = { filtrosAbiertos = false },
        )
    }
}

/** "DESPENSA" → "Despensa" con fallback seguro si el nombre no es un rubro. */
private fun etiquetaClase(nombre: String): String =
    runCatching { Rubro.valueOf(nombre).etiquetaVisible() }.getOrDefault(nombre)

/**
 * Buscador.
 *
 * El placeholder va por `decorationBox` y no como un `Text` apilado en un `Box`
 * al lado del campo. La versión anterior lo apilaba así, pero el campo llevaba
 * `padding(horizontal = 8.dp)` y el `Text` no: el placeholder se dibujaba 8 dp a
 * la izquierda de donde aparecía el texto real, y al teclear la primera letra
 * todo el campo saltaba a la derecha.
 *
 * Por qué `decorationBox` y no el parámetro `placeholder` de `BasicTextField`:
 * ese parámetro solo existe en la sobrecarga que recibe un `TextFieldValue`, que
 * es `ExperimentalFoundationApi`. La sobrecarga estable con `String` se resuelve
 * con `decorationBox`, y ahí el placeholder y el campo se componen **dentro del
 * mismo layout y del mismo padding**, así que no se pueden desalinear.
 *
 * También se cablea `ImeAction.Search` con su `KeyboardActions`: declarar la
 * acción del teclado sin implementar `onSearch` deja una tecla visible que no
 * hace nada, que es peor que no tenerla.
 */
@Composable
private fun CampoBusqueda(
    query: () -> String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Lectura diferida a propósito: el estado vive en CatalogScreen, pero se
    // lee acá. Si se pasara el String, cada tecla invalidaría toda la pantalla.
    val texto = query()
    val hayTexto = texto.isNotEmpty()
    val foco = LocalFocusManager.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Espacio.toqueMinimo)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.extraLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Espacio.l).size(20.dp),
        )
        BasicTextField(
            value = texto,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            // `onSurface` y no `primary`: el naranja sobre el buscador da
            // 2.75:1 y el cursor es interfaz (3:1).
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    // Bajar el teclado: la búsqueda ya se consultó, y tenerlo
                    // abierto solo tapa los resultados que el usuario vino a ver.
                    foco.clearFocus()
                },
            ),
            decorationBox = { campo ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (!hayTexto) {
                        Text(
                            text = "Buscar producto o código de barras",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            // El campo ya se anuncia con `contentDescription`. Si el
                            // placeholder dejara su semántica, TalkBack leería lo
                            // mismo dos veces: "Buscar producto…, campo de texto,
                            // Buscar producto…". Se ve, pero no se oye.
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    }
                    campo()
                }
            },
            // `BasicTextField` no expone `label`: sin esto TalkBack anuncia
            // "campo de texto" y nada más, sin decir de qué.
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Espacio.s)
                .semantics { contentDescription = "Buscar producto o código de barras" },
        )
        AnimatedVisibility(
            visible = hayTexto,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            IconButton(onClick = { onChange("") }) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Limpiar búsqueda",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.width(Espacio.s))
    }
}

/**
 * Aviso de CEC viejo. Es un componente chico a propósito: lee `LocalUsdPrecio`
 * y el flujo de frescura acá, no en `CatalogScreen`, para que cambiar la moneda
 * no invalide la cabecera, la grilla ni el scroll.
 */
@Composable
private fun AvisoPrecioSolidarioViejo(
    enrichViejoFlow: StateFlow<Boolean>,
    onActualizar: () -> Unit,
) {
    if (!LocalUsdPrecio.current) return
    val enrichViejo by enrichViejoFlow.collectAsStateWithLifecycle()
    if (!enrichViejo) return
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Espacio.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Precio solidario desactualizado.",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onActualizar) { Text("Actualizar") }
    }
}

/**
 * Estado vacío del catálogo. La causa importa y el mensaje la solía confundir:
 * "no hay datos" pide sincronizar, y "tus filtros no dejan nada" pide deshacer un
 * filtro — ofrecer la salida es lo que hace que un estado vacío sirva de algo en
 * vez de ser decorativo.
 */
@Composable
private fun EstadoVacio(
    total: Int,
    query: String,
    esperando: Boolean,
    conFiltros: Boolean,
    onQuitarBusqueda: () -> Unit,
    onLimpiarFiltros: () -> Unit,
) {
    val buscando = query.isNotBlank()
    val mostrada = query.trim()
    // El "Cargando precios…" de antes no tenía señal: `yaRefrescado` solo pasaba a
    // true tras un refresh manual, así que una búsqueda sin resultados en una
    // sesión normal se quedaba en "Cargando precios…" para siempre. El total es
    // la señal correcta: si hay datos, una lista vacía es un resultado, no una
    // carga. `esperando` cubre el debounce: mientras la consulta nueva no asentó,
    // el mensaje no puede afirmar que no hay resultados.
    val (mensaje, accion) = when {
        total == 0 -> "Aún no hay precios cargados.\nJala hacia abajo para sincronizar." to null
        esperando -> "Buscando…" to null
        buscando && conFiltros ->
            "Ningún producto coincide con «$mostrada» en los rubros que elegiste." to
                ("Quitar la búsqueda" to onQuitarBusqueda)
        buscando -> "Sin resultados para «$mostrada»." to ("Quitar la búsqueda" to onQuitarBusqueda)
        conFiltros -> "Ningún producto en los rubros que elegiste." to
            ("Limpiar los filtros" to onLimpiarFiltros)
        else -> "Sin productos para mostrar." to null
    }
    Column(
        Modifier.fillMaxSize().padding(Espacio.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (accion != null) {
            Spacer(Modifier.height(Espacio.m))
            TextButton(onClick = accion.second) { Text(accion.first) }
        }
    }
}

/**
 * Hoja de filtros: orden (segmentado), rubros con conteo por opción
 * (Baymard: la mejora de mayor impacto) y multi-selección. Ver DESIGN.md §7.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HojaFiltros(
    conteoClasesFlow: StateFlow<List<ClaseConteo>>,
    clasesSel: Set<String>,
    orden: Orden,
    onOrden: (Orden) -> Unit,
    onToggleClase: (String) -> Unit,
    onLimpiar: () -> Unit,
    onCerrar: () -> Unit,
) {
    // El componente recibe el flujo estrecho que usa, no el ViewModel entero.
    // Solo se recolecta con la hoja abierta: antes el `conteoClases` se
    // recolectaba en CatalogScreen y hacía una consulta GROUP BY en cada
    // búsqueda aunque el usuario nunca abriera los filtros.
    val conteoClases by conteoClasesFlow.collectAsStateWithLifecycle()
    // Un rubro elegido que no tiene resultados con la búsqueda actual no aparece
    // en `conteoClases`; sin agregarlo acá desaparecía de la hoja y no había
    // forma de destildarlo. Se agrega con 0 y se ordena por total descendente.
    val opciones = (
        conteoClases +
            clasesSel
                .filter { clase -> conteoClases.none { it.clase == clase } }
                .map { ClaseConteo(clase = it, total = 0) }
        ).sortedByDescending { it.total }
    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Espacio.l)
                .padding(bottom = Espacio.xxl),
        ) {
            Text("Orden", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Espacio.s))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Orden.entries.forEachIndexed { i, o ->
                    SegmentedButton(
                        selected = orden == o,
                        onClick = { onOrden(o) },
                        shape = SegmentedButtonDefaults.itemShape(i, Orden.entries.size),
                    ) {
                        Text(o.label)
                    }
                }
            }

            Spacer(Modifier.height(Espacio.xl))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Rubros", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onLimpiar, enabled = clasesSel.isNotEmpty()) {
                    Text("Limpiar todo")
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                verticalArrangement = Arrangement.spacedBy(Espacio.s),
            ) {
                opciones.forEach { cc ->
                    FilterChip(
                        selected = cc.clase in clasesSel,
                        onClick = { onToggleClase(cc.clase) },
                        label = { Text("${etiquetaClase(cc.clase)} (${cc.total})") },
                    )
                }
            }
        }
    }
}
