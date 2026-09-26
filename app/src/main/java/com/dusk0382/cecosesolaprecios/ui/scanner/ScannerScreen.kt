package com.dusk0382.cecosesolaprecios.ui.scanner

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.dusk0382.cecosesolaprecios.ui.theme.Espacio
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dusk0382.cecosesolaprecios.data.local.ProductEntity
import com.dusk0382.cecosesolaprecios.data.repository.ProductRepository
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "CecoScanner"

/**
 * Escáner EAN-13 / EAN-8 / UPC-A con CameraX + ML Kit bundled (offline, sin Play
 * Services). Decisiones de rendimiento para el Helio G25:
 *  - Análisis a 720p: sobra para leer un código y es lo que más CPU consume.
 *  - STRATEGY_KEEP_ONLY_LATEST: si no alcanza el ritmo, los frames se descartan.
 *  - Este scanner se usa en la feria: si el código no está catalogado, la UI
 *    ofrece buscar por el número en vez de dejar al usuario en un callejón.
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repo: ProductRepository,
) : ViewModel() {

    data class ScanResult(val producto: ProductEntity?, val codigo: String)

    private val _resultado = MutableStateFlow<ScanResult?>(null)
    val resultado: StateFlow<ScanResult?> = _resultado.asStateFlow()

    /**
     * true entre que se detecta un código y se resuelve la consulta en Room.
     *
     * Antes era un `@Volatile private var` que solo se reseteaba en
     * `reiniciar()`, un método que **no se llamaba desde ningún lado**. El escáner
     * quedaba de un solo uso por sesión: después del primer barcode, todos los
     * frames siguientes se descartaban en silencio y no había ni error ni
     * movimiento en pantalla que explicara por qué. Ahora es estado observable
     * (para poder mostrar "Buscando el producto…") y se libera en [consumido],
     * que es donde la UI se hace cargo del resultado.
     */
    private val _buscando = MutableStateFlow(false)
    val buscando: StateFlow<Boolean> = _buscando.asStateFlow()

    fun onBarcode(texto: String?) {
        val codigo = texto
            ?.filter(Char::isDigit)
            ?.takeIf { it.length in 8..14 }
            ?: return
        if (_buscando.value) return
        _buscando.value = true
        viewModelScope.launch {
            try {
                _resultado.value = ScanResult(repo.byBarcode(codigo), codigo)
            } catch (e: Exception) {
                // Sin este try el error subía al handler por defecto: proceso
                // muerto, y `buscando` quedaba en true para siempre.
                Log.e(TAG, "byBarcode($codigo) falló: ${e.message}", e)
            } finally {
                // Si la consulta falla, el cerrojo se libera igual para que el
                // usuario pueda intentar con otro código en vez de quedarse con
                // un escáner que ya no responde.
                _buscando.value = false
            }
        }
    }

    /** Lo llama la UI cuando ya hizo cargo del resultado. */
    fun consumido() {
        _resultado.value = null
    }
}

@Composable
fun ScannerScreen(
    onFound: (Long) -> Unit,
    onNotFound: (String) -> Unit,
    onBack: () -> Unit,
    vm: ScannerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun permisoConcedido() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    var permiso by remember { mutableStateOf(permisoConcedido()) }
    // Si ya se pidió una vez y el sistema ya no muestra el diálogo, el permiso
    // quedó denegado de forma permanente: volver a pedirlo no hace nada y hay que
    // mandar a Ajustes del sistema.
    var yaPedido by rememberSaveable { mutableStateOf(false) }
    varNegadoPermanente by remember { mutableStateOf(false) }

    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        permiso = concedido
        yaPedido = true
        if (!concedido) {
            denegadoPermanente = !ActivityCompat.shouldShowRequestPermissionRationale(
                context,
                Manifest.permission.CAMERA,
            )
        }
    }

    // El permiso se re-chequea al volver al primer plano. Antes se calculaba una
    // sola vez en un remember{}, así que si el usuario lo daba en Ajustes del
    // sistema y volvía, la pantalla seguía diciendo que faltaba: la única forma de
    // recuperar era matar el proceso.
    DisposableEffect(lifecycleOwner) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) permiso = permisoConcedido()
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
    }

    LaunchedEffect(Unit) {
        if (!permiso && !yaPedido) lanzadorPermiso.launch(Manifest.permission.CAMERA)
    }

    val resultado by vm.resultado.collectAsStateWithLifecycle()
    val buscando by vm.buscando.collectAsStateWithLifecycle()
    LaunchedEffect(resultado) {
        val r = resultado ?: return@LaunchedEffect
        // El cerrojo del ViewModel se libera acá, que es donde se consume el
        // resultado. Antes solo se reseteaba en `reiniciar()`, que no se llamaba
        // desde ningún lado: el escáner quedaba de un solo uso por sesión.
        vm.consumido()
        val producto = r.producto
        if (producto != null) onFound(producto.localId) else onNotFound(r.codigo)
    }

    var estadoCamera by remember { mutableStateOf<EstadoCamera>(EstadoCamera.Vinculando) }

    Box(Modifier.fillMaxSize()) {
        if (permiso) {
            CameraAnalyzer(
                onCode = vm::onBarcode,
                onEstado = { estadoCamera = it },
            )
            // Velo para que el texto y el botón de salir se lean sobre cualquier
            // foto: el caso de uso de esta app es fotografiar productos en una
            // góndola, y contra una caja blanca o una ventana el texto blanco
            // desaparece. El botón de cerrar es justamente el que no puede
            // volverse invisible.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(vertical = Espacio.l),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        buscando -> "Buscando el producto…"
                        estadoCamera is EstadoCamera.Error -> (estadoCamera as EstadoCamera.Error).mensaje
                        else -> "Apunta al código de barras"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Espacio.l),
                )
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(Espacio.s)
                    .background(Color.Black.copy(alpha = 0.55f), MaterialTheme.shapes.extraLarge),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.Close, "Cerrar", tint = Color.White)
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(Espacio.xxl),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Se necesita permiso de cámara para escanear.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Espacio.l))
                Button(onClick = {
                    if (denegadoPermanente) {
                        // El sistema no va a mostrar otro diálogo: al ajuste del SO.
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    } else {
                        lanzadorPermiso.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Text(if (denegadoPermanente) "Abrir ajustes del sistema" else "Dar permiso")
                }
            }
        }
    }
}

@Composable
private fun CameraAnalyzer(onCode: (String?) -> Unit) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    val scanner: BarcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                )
                .build(),
        )
    }

    // setAnalyzer exige un Executor; el main executor mantiene el orden con la UI
    // y el trabajo pesado real (ML Kit) corre en su propio executor interno.
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }

    DisposableEffect(Unit) {
        onDispose { scanner.close() }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize(),
        update = { view -> bindCamera(view, scanner, executor, onCode) },
    )
}

/**
 * Estado de la cámara. Se reporta a la UI en vez de tragarse los errores: antes,
 * si el bind fallaba, `runCatching` descartaba la excepción y `previewView.tag`
 * ya estaba puesto, así que la pantalla quedaba en negro para siempre con el
 * texto "Apunta al código de barras" y sin explicación ni reintento. Con
 * `required="false"` en el manifest la app se instala en equipos sin cámara, y
 * ahí `DEFAULT_BACK_CAMERA` no se puede satisfacer.
 */
sealed interface EstadoCamera {
    data object Vinculando : EstadoCamera
    data object Lista : EstadoCamera

    /** `mensaje` va a la UI: "no se encontró cámara" no es un detalle de log. */
    data class Error(val mensaje: String) : EstadoCamera
}

/**
 * Vincula Preview + ImageAnalysis al ciclo de vida del PreviewView.
 *
 * Se hace en `update` (no en `LaunchedEffect`) para que el UseCase siempre apunte
 * al PreviewView ya adjunto a la composición y no se fugue tras un recompose.
 *
 * El marcador de "ya vinculado" es un campo del companion, **no** `tag` de la
 * vista: el `tag` se escribía antes de `future.get()`, así que la guarda
 * significaba "se intentó" y no "funcionó", y cualquier fallo posterior dejaba la
 * vista en un estado donde ya no se podía reintentar.
 */
private fun bindCamera(
    previewView: PreviewView,
    scanner: BarcodeScanner,
    executor: java.util.concurrent.Executor,
    onCode: (String?) -> Unit,
    onEstado: (EstadoCamera) -> Unit,
) {
    if (previewView.isVinculado) return
    previewView.isVinculado = true
    onEstado(EstadoCamera.Vinculando)

    val future = ProcessCameraProvider.getInstance(previewView.context)
    future.addListener({
        try {
            val provider = future.get()
            val lifecycleOwner = previewView.findViewTreeLifecycleOwner()
            if (lifecycleOwner == null) {
                // Sin owner no hay contra qué registrar el caso de uso: se marca
                // como no vinculado para que un recompose pueda reintentar.
                previewView.isVinculado = false
                onEstado(EstadoCamera.Error("La vista de cámara todavía no está asociada a una pantalla."))
                return@addListener
            }
            provider.unbindAll()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                            ),
                        )
                        .build(),
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { useCase ->
                    useCase.setAnalyzer(executor) { proxy ->
                        // `proxy.close()` tiene que estar en un finally. Antes solo
                        // se cerraba desde el callback de `addOnCompleteListener`, así
                        // que si `InputImage.fromMediaImage` o `scanner.process`
                        // lanzaban, el frame no se liberaba nunca. ImageAnalysis
                        // tiene un presupuesto chico de frames en vuelo: uno
                        // fugado y el análisis deja de entregar frames para siempre.
                        try {
                            val mediaImage = proxy.image ?: return@setAnalyzer
                            val input = InputImage.fromMediaImage(
                                mediaImage,
                                proxy.imageInfo.rotationDegrees,
                            )
                            scanner.process(input)
                                .addOnSuccessListener { codes ->
                                    onCode(codes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue)
                                }
                        } catch (e: Exception) {
                            Log.w(TAG, "fallo analizando un frame: ${e.message}", e)
                        } finally {
                            proxy.close()
                        }
                    }
                }

            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                UseCaseGroup.Builder().addUseCase(preview).addUseCase(analysis).build(),
            )
            onEstado(EstadoCamera.Lista)
        } catch (e: Exception) {
            previewView.isVinculado = false
            val mensaje = when (e) {
                is IllegalArgumentException ->
                    "Este equipo no tiene una cámara trasera disponible."
                else -> "No se pudo abrir la cámara: ${e.message ?: e::class.simpleName}"
            }
            Log.e(TAG, "bindCamera falló: $mensaje", e)
            onEstado(EstadoCamera.Error(mensaje))
        }
    }, executor)
}

/**
 * Marcador de "la cámara quedó vinculada".
 *
 * Sigue en el `tag` de la vista —es el lugar idiomático para estado por vista en
 * Android, y `PreviewView` no lo usa— pero lo que cambió **no es el mecanismo sino
 * el valor y el momento**: antes guardaba el `ListenableFuture`, puesto *antes*
 * de `future.get()`, así que la guarda de "ya vinculado" significaba "se intentó"
 * y cualquier fallo posterior dejaba la vista sin forma de reintentar. Ahora es un
 * booleano que solo se pone en `true` cuando el bind terminó bien, y vuelve a
 * `false` en cada fallo para que un recompose reintente.
 */
private var PreviewView.isVinculado: Boolean
    get() = this.tag as? Boolean ?: false
    set(value) {
        this.tag = value
    }

