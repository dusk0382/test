package com.dusk0382.cecosesolaprecios.ui.settings

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dusk0382.cecosesolaprecios.data.prefs.ThemeMode
import com.dusk0382.cecosesolaprecios.data.repository.ProductRepository
import com.dusk0382.cecosesolaprecios.ui.MainViewModel
import com.dusk0382.cecosesolaprecios.ui.common.FilaDato
import com.dusk0382.cecosesolaprecios.ui.common.FilaDatoBloque
import com.dusk0382.cecosesolaprecios.ui.common.formatBs
import com.dusk0382.cecosesolaprecios.ui.common.formatearFechaMirror
import com.dusk0382.cecosesolaprecios.ui.common.formatFechaHora
import com.dusk0382.cecosesolaprecios.ui.theme.Espacio
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstadoDatos(
    val fechaRepo: String? = null,
    val milisApi: Long? = null,
    val tasaVed: Double? = null,
    val ferias: List<String> = emptyList(),
    val productos: Int? = null,
    val verificando: Boolean = false,
    val reconstruyendo: Boolean = false,
    val mensaje: String? = null,
    /**
     * false mientras `recargar()` no terminó su primera vuelta.
     *
     * Antes el valor inicial de los otros campos era `null`, y `null` se pintaba
     * como "nunca": en la **primera composición** —antes de que las cuatro
     * lecturas asíncronas de Room terminen— la pantalla decía "Lista base
     * (mirror) — nunca" y "Enriquecido (oficial) — nunca" para datos que sí
     * existían, y ocultaba el interruptor de USD por `tasaVed == null`. Un
     * usuario que tocara "Verificar datos" en ese frame recibía un mensaje
     * engañoso. "Todavía no sé" no es "nunca": son dos estados distintos.
     */
    val cargado: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: ProductRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoDatos())
    val estado: StateFlow<EstadoDatos> = _estado.asStateFlow()

    init { recargar() }

    private fun recargar() = viewModelScope.launch {
        val fechaRepo = repo.ultimaFechaRepo()
        val milisApi = repo.ultimaFechaApi()?.toLongOrNull()
        val tasaVed = repo.tasaOficial()
        val ferias = repo.ferias()
        val productos = repo.countFlow().first()
        _estado.update {
            it.copy(
                fechaRepo = fechaRepo,
                milisApi = milisApi,
                tasaVed = tasaVed,
                ferias = ferias,
                productos = productos,
                cargado = true,
            )
        }
    }

    /**
     * Refresco manual: el mirror primero (rápido, sí o sí) y el enriquecimiento
     * se encola como worker — la API oficial tarda 7–40s y no se hace esperar al
     * usuario con la pantalla bloqueada. El mensaje es honesto sobre eso.
     *
     * El `finally` importa: `requestEnrich()` estaba fuera del `runCatching`, así
     * que si lanzaba, `verificando` se quedaba en `true` y el botón quedaba
     * deshabilitado para siempre, sin forma de reintentar a mano.
     */
    fun verificar() {
        if (_estado.value.verificando) return
        _estado.update { it.copy(verificando = true, mensaje = null) }
        viewModelScope.launch {
            var mensaje: String? = null
            try {
                val ok = runCatching { repo.syncBase() }
                    .onFailure { Log.w("CecoSync", "verificar: syncBase fallo: ${it.message}", it) }
                    .isSuccess
                repo.requestEnrich()
                mensaje = if (ok) {
                    "Lista base actualizada. Enriqueciendo en segundo plano…"
                } else {
                    "No se pudo contactar la lista. Revisa tu conexión."
                }
            } catch (e: Exception) {
                Log.e("CecoSync", "verificar: ${e.message}", e)
                mensaje = "No se pudo verificar. Revisa tu conexión."
            } finally {
                _estado.update {
                    it.copy(verificando = false, mensaje = mensaje)
                }
            }
            recargar()
        }
    }

    /**
     * Reparación de una base instalada vieja con apiIds/CEC mal asignados: borra
     * el enriquecimiento y lo vuelve a encolar. El guard evita dos borrados
     * simultáneos si se toca el botón dos veces antes de recomponer.
     */
    fun reconstruir() {
        if (_estado.value.reconstruyendo) return
        _estado.update { it.copy(reconstruyendo = true, mensaje = null) }
        viewModelScope.launch {
            var mensaje: String? = null
            try {
                repo.resetEnrichment()
                mensaje = "Enriquecimiento borrado. Descargando la lista oficial en segundo plano…"
            } catch (e: Exception) {
                Log.e("CecoSync", "reconstruir: ${e.message}", e)
                mensaje = "No se pudo reconstruir. Revisá tu conexión."
            } finally {
                _estado.update { it.copy(reconstruyendo = false, mensaje = mensaje) }
            }
            recargar()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    mainVm: MainViewModel = hiltViewModel(),
    vm: SettingsViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val themeMode by mainVm.themeMode.collectAsStateWithLifecycle()
    val usd by mainVm.usd.collectAsStateWithLifecycle()
    var confirmarReconstruccion by remember { mutableStateOf(false) }

    // Pasadas 12 h, el precio solidario es un dato viejo, no un fallo: se avisa
    // sin color de error. Sin fecha de enriquecimiento también se avisa.
    val enrichViejo = estado.cargado &&
        (estado.milisApi?.let { System.currentTimeMillis() - it > 12L * 60 * 60 * 1000 } ?: true)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
                },
            )
        },
    ) { insets ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(Espacio.l),
        ) {
            Seccion("Datos")
            val pendiente = if (estado.cargado) "nunca" else "leyendo…"
            // El mirror guarda "2026-09-26 19:00:24" crudo y la API oficial guarda
            // epoch millis. Mostrar uno en ISO y el otro con "26/09/2026 a las
            // 01:43" en la misma columna era directamente comparable en el
            // screenshot y no encajaba. Los dos pasan por el mismo formateador.
            FilaDato("Lista base (mirror)", estado.fechaRepo?.let(::formatearFechaMirror) ?: pendiente)
            FilaDato(
                "Enriquecido (oficial)",
                estado.milisApi?.let(::formatFechaHora) ?: pendiente,
            )
            if (enrichViejo) {
                Text(
                    if (estado.milisApi == null) {
                        "Sin precio solidario actualizado. Verificá datos antes de usarlo."
                    } else {
                        "Precio solidario desactualizado (más de 12 h). Verificá datos antes de usarlo."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Espacio.xs),
                )
            }
            FilaDato("Productos", estado.productos?.toString() ?: "leyendo…")
            estado.tasaVed?.let { tasa ->
                FilaDato("Tasa del precio solidario", "1 USD = Bs ${formatBs(tasa)}")
            }
            if (estado.ferias.isNotEmpty()) {
                FilaDatoBloque("Ferias", estado.ferias.joinToString(", "))
            }

            Spacer(Modifier.height(Espacio.s))
            Button(
                onClick = vm::verificar,
                enabled = !estado.verificando && !estado.reconstruyendo,
            ) {
                if (estado.verificando) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(Espacio.s))
                }
                Text(if (estado.verificando) "Verificando…" else "Verificar datos")
            }
            Spacer(Modifier.height(Espacio.s))
            OutlinedButton(
                onClick = { confirmarReconstruccion = true },
                enabled = !estado.verificando && !estado.reconstruyendo,
            ) {
                if (estado.reconstruyendo) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(Espacio.s))
                }
                Text(if (estado.reconstruyendo) "Reconstruyendo…" else "Reconstruir enriquecimiento")
            }
            estado.mensaje?.let {
                Spacer(Modifier.height(Espacio.s))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(Espacio.xl))
            HorizontalDivider()
            Seccion("Apariencia")

            Text("Tema", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(Espacio.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                ThemeMode.entries.forEach { modo ->
                    FilterChip(
                        selected = themeMode == modo,
                        onClick = { mainVm.setTheme(modo) },
                        label = {
                            Text(
                                when (modo) {
                                    ThemeMode.SYSTEM -> "Sistema"
                                    ThemeMode.LIGHT -> "Claro"
                                    ThemeMode.DARK -> "Oscuro"
                                },
                            )
                        },
                    )
                }
            }

            if (estado.tasaVed != null) {
                Spacer(Modifier.height(Espacio.l))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = usd,
                            role = Role.Switch,
                            onValueChange = mainVm::setUsd,
                        )
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        "Mostrar precio solidario (USD)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = usd, onCheckedChange = null)
                }
            }

            Spacer(Modifier.height(Espacio.xl))
            HorizontalDivider()
            Seccion("Fuente")
            Text(
                "Precios tomados de precios.cecosesola.coop y de la lista oficial de Cecosesola. " +
                    "Los precios pueden variar por feria; verifica en la feria más cercana.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Espacio.xl))
        }
    }

    if (confirmarReconstruccion) {
        AlertDialog(
            onDismissRequest = { confirmarReconstruccion = false },
            title = { Text("¿Reconstruir el precio solidario?") },
            text = {
                Text(
                    "Se borrarán los precios solidarios y se volverán a descargar de la lista " +
                        "oficial. Favoritos y carrito se conservan.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarReconstruccion = false
                        vm.reconstruir()
                    },
                ) { Text("Reconstruir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarReconstruccion = false }) { Text("Cancelar") }
            },
        )
    }
}

/** Encabezado de sección: peso tipográfico, sin MAYÚSCULAS ni color (§3.3/§3.5). */
@Composable
private fun Seccion(titulo: String) {
    Text(
        titulo,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = Espacio.s + Espacio.xs),
    )
}
