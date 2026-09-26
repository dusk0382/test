package com.dusk0382.cecosesolaprecios.ui.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
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
import com.dusk0382.cecosesolaprecios.domain.Rubro
import com.dusk0382.cecosesolaprecios.domain.etiquetaVisible
import com.dusk0382.cecosesolaprecios.ui.common.RenglonProducto
import com.dusk0382.cecosesolaprecios.ui.theme.Espacio

/**
 * Catálogo según DESIGN.md §7: buscador + botón de filtros con badge + FAB de
 * escáner. Sin filas de chips de categoría: los ~100 tags de la API no son
 * navegables (los reemplaza la clasificación medida de `domain/Rubros.kt`).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CatalogScreen(
    vm: CatalogViewModel,
    onOpenDetail: (Long) -> Unit,
    onScan: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val productos by vm.productos.collectAsStateWithLifecycle()
    val clasesSel by vm.clasesSel.collectAsStateWithLifecycle()
    val conteoClases by vm.conteoClases.collectAsStateWithLifecycle()
    val orden by vm.orden.collectAsStateWithLifecycle()
    val sincronizando by vm.sincronizando.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val yaRefrescado by vm.yaRefrescado.collectAsStateWithLifecycle()
    val cantidades by vm.cantidades.collectAsStateWithLifecycle()
    val favoritos by vm.favoritos.collectAsStateWithLifecycle()
    val hayFiltros by vm.hayFiltros.collectAsStateWithLifecycle()

    // El campo de búsqueda es dueño de su estado: cada tecleo sólo recompone el
    // campo, no los chips ni la grilla (DESIGN.md §8.2). Al VM llega el valor,
    // y el debounce decide cuándo reconsultar.
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

    // Un cambio de búsqueda, de filtros o de orden cambia **el conjunto de
    // resultados**, así que la grilla tiene que volver al principio.
    //
    // Lo que pasaba antes: la grilla sin `state` explícito conserva un
    // `rememberLazyGridState()` interno que sobrevive a los cambios de contenido.
    // Al filtrar a 3 resultados el índice del ancla se mantenía, y al limpiar el
    // campo la lista se re-expandía a 518 con el usuario en la mitad del
    // catálogo, justo donde estaba antes de filtrar. "Como si se hubiera
    // scrolleado" era el síntoma, y era el estado, no la animación.
    //
    // `key(...) { rememberLazyGridState() }` en vez de un
    // `LaunchedEffect { scrollToItem(0) }`: cambiar las claves descarta el slot del
    // `remember` y la grilla arranca en 0 siempre. Con `LaunchedEffect` había que
    // llamar `scrollToItem` sobre un estado que puede no estar montado —si la
    // búsqueda no deja resultados la grilla no está compuesta— y depender de que
    // el scroll pendiente se aplique al volver a adjuntarse. `key` no depende de
    // nada: es 0 por construcción.
    val gridState = key(query, clasesSel, orden) { rememberLazyGridState() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // — Cabecera: título + filtros (con badge de activos) + ajustes —
            Row(
                Modifier.fillMaxWidth().padding(start = Espacio.l, end = Espacio.xs, top = Espacio.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Cecosesola",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { filtrosAbiertos = true }) {
                    BadgedBox(
                        badge = {
                            if (clasesSel.isNotEmpty()) {
                                Badge { Text("${clasesSel.size}") }
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
                query = query,
                onChange = { query = it; vm.onBusquedaChange(it) },
                modifier = Modifier.padding(horizontal = Espacio.l, vertical = Espacio.s),
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
                Crossfade(
                    targetState = productos.isEmpty(),
                    label = "catalogo-estado",
                    modifier = Modifier.fillMaxSize(),
                ) { vacio ->
                    if (vacio) {
                        EstadoVacio(
                            total = total,
                            yaRefrescado = yaRefrescado,
                            query = query,
                            conFiltros = hayFiltros,
                            onQuitarBusqueda = { query = ""; vm.onBusquedaChange("") },
                            onLimpiarFiltros = vm::limpiarFiltros,
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = gridState,
                            contentPadding = PaddingValues(Espacio.m),
                            horizontalArrangement = Arrangement.spacedBy(Espacio.m),
                            verticalArrangement = Arrangement.spacedBy(Espacio.m),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(productos, key = { it.localId }) { p ->
                                RenglonProducto(
                                    producto = p,
                                    cantidad = cantidades[p.localId] ?: 0,
                                    esFavorito = p.localId in favoritos,
                                    onCantidad = { vm.setCantidad(p.localId, it) },
                                    onFavorito = { vm.toggleFavorito(p.localId) },
                                    onClick = { onOpenDetail(p.localId) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }

        // — FAB de escáner: entra con spring, se va al navegar (no teletransportación) —
        AnimatedVisibility(
            visible = !sincronizando,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Espacio.l),
        ) {
            FloatingActionButton(
                onClick = onScan,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(painterResource(R.drawable.ic_qr_scanner), "Escanear código de barras")
            }
        }
    }

    if (filtrosAbiertos) {
        HojaFiltros(
            clasesSel = clasesSel,
            conteoClases = conteoClases,
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
 * El placeholder usa el parámetro `placeholder` de `BasicTextField` y no un
 * `Text` apilado en un `Box`. La versión anterior apilaba un `Text` encima del
 * campo dentro del mismo `Box`, pero el campo llevaba
 * `padding(horizontal = 8.dp)` y el `Text` no: el placeholder se dibujaba 8 dp a
 * la izquierda de donde aparecía el texto real, y al teclear la primera letra
 * todo el campo saltaba a la derecha. El `placeholder` de `BasicTextField` se
 * compone en el origen del texto por construcción, así que no se puede
 * desalinear.
 *
 * También se cablea `ImeAction.Search` con su `KeyboardActions`: declarar la
 * acción del teclado sin implementar su `onSearch` deja una tecla visible que
 * no hace nada, que es peor que no tenerla.
 */
@Composable
private fun CampoBusqueda(
    query: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hayTexto = query.isNotEmpty()
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
            value = query,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            placeholder = {
                Text(
                    text = "Buscar producto o código de barras",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // El campo ya se anuncia con `contentDescription`. Si el
                    // placeholder dejara su semántica, TalkBack leería lo mismo dos
                    // veces seguidas: "Buscar producto o código de barras, campo de
                    // texto, Buscar producto o código de barras". El texto se ve,
                    // pero no se oye: para eso está la etiqueta del campo.
                    modifier = Modifier.clearAndSetSemantics { },
                )
            },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    // Bajar el teclado y devolver el foco a la grilla: la búsqueda
                    // ya se consultó, seguir con el teclado abierto solo tapa los
                    // resultados que el usuario vino a ver.
                    foco.clearFocus()
                },
            ),
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
 * Estado vacío del catálogo. La causa importa y el mensaje la solía confundir:
 * "no hay datos" pide sincronizar, y "tus filtros no dejan nada" pide deshacer un
 * filtro — ofrecer la salida es lo que hace que un estado vacío sirva de algo en
 * vez de ser decorativo.
 */
@Composable
private fun EstadoVacio(
    total: Int,
    yaRefrescado: Boolean,
    query: String,
    conFiltros: Boolean,
    onQuitarBusqueda: () -> Unit,
    onLimpiarFiltros: () -> Unit,
) {
    val buscando = query.isNotBlank()
    val (mensaje, accion) = when {
        total == 0 -> "Aún no hay precios cargados.\nJala hacia abajo para sincronizar." to null
        !yaRefrescado -> "Cargando precios…" to null
        buscando && conFiltros ->
            "Ningún producto coincide con «$query» en los rubros que elegiste." to
                ("Quitar la búsqueda" to onQuitarBusqueda)
        buscando -> "Sin resultados para «$query»." to ("Quitar la búsqueda" to onQuitarBusqueda)
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
    clasesSel: Set<String>,
    conteoClases: List<com.dusk0382.cecosesolaprecios.data.local.ClaseConteo>,
    orden: Orden,
    onOrden: (Orden) -> Unit,
    onToggleClase: (String) -> Unit,
    onLimpiar: () -> Unit,
    onCerrar: () -> Unit,
) {
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
                conteoClases.forEach { cc ->
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
