package com.dusk0382.cecosesolaprecios.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dusk0382.cecosesolaprecios.data.prefs.ThemeMode
import com.dusk0382.cecosesolaprecios.data.repository.ProductRepository
import com.dusk0382.cecosesolaprecios.ui.MainViewModel
import com.dusk0382.cecosesolaprecios.ui.common.formatBs
import com.dusk0382.cecosesolaprecios.ui.common.formatFechaHora
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstadoDatos(
    val fechaRepo: String? = null,
    val milisApi: Long? = null,
    val tasaVed: Double? = null,
    val ferias: List<String> = emptyList(),
    val verificando: Boolean = false,
    val mensaje: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: ProductRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoDatos())
    val estado: StateFlow<EstadoDatos> = _estado.asStateFlow()

    init { recargar() }

    private fun recargar() = viewModelScope.launch {
        _estado.update {
            it.copy(
                fechaRepo = repo.ultimaFechaRepo(),
                milisApi = repo.ultimaFechaApi()?.toLongOrNull(),
                tasaVed = repo.tasaOficial(),
                ferias = repo.ferias(),
            )
        }
    }

    /**
     * Refresco manual: el mirror primero (rápido, sí o sí) y el enriquecimiento
     * se encola como worker — la API oficial tarda 7–40s y no se hace esperar al
     * usuario con la pantalla bloqueada. El mensaje es honesto sobre eso.
     */
    fun verificar() {
        if (_estado.value.verificando) return
        _estado.update { it.copy(verificando = true, mensaje = null) }
        viewModelScope.launch {
            val ok = runCatching { repo.syncBase() }.isSuccess
            repo.requestEnrich()
            _estado.update {
                it.copy(
                    verificando = false,
                    mensaje = if (ok) {
                        "Lista base actualizada. Enriqueciendo en segundo plano…"
                    } else {
                        "No se pudo contactar la lista. Revisa tu conexión."
                    },
                )
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
                },
            )
        },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Seccion("Datos")
            Dato("Lista base (mirror)", estado.fechaRepo ?: "nunca")
            Dato(
                "Enriquecido (oficial)",
                estado.milisApi?.let(::formatFechaHora) ?: "nunca",
            )
            estado.tasaVed?.let { tasa ->
                Dato("Tasa oficial", "1 USD = Bs ${formatBs(tasa)}")
            }
            if (estado.ferias.isNotEmpty()) {
                Dato("Ferias", estado.ferias.joinToString(", "))
            }

            Spacer(Modifier.height(12.dp))
            Button(onClick = vm::verificar, enabled = !estado.verificando) {
                if (estado.verificando) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (estado.verificando) "Verificando…" else "Verificar datos")
            }
            estado.mensaje?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Seccion("Apariencia")

            Text("Tema", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Mostrar precios en USD", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Usa el precio solidario (CEC) de la lista oficial.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = usd, onCheckedChange = mainVm::setUsd)
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Seccion("Fuente")
            Text(
                "Precios tomados de precios.cecosesola.coop y de la lista oficial de Cecosesola. " +
                    "Los precios pueden variar por feria; verifica en la feria más cercana.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Seccion(titulo: String) {
    Text(
        titulo,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 10.dp),
    )
}

@Composable
private fun Dato(label: String, valor: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
