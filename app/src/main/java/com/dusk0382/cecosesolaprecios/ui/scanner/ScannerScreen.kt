package com.dusk0382.cecosesolaprecios.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

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
    val resultado: StateFlow<ScanResult?> = _resultado

    /** Evita procesar el mismo frame múltiples veces mientras se busca en Room. */
    @Volatile
    private var procesando = false

    fun onBarcode(texto: String?) {
        val codigo = texto
            ?.filter(Char::isDigit)
            ?.takeIf { it.length in 8..14 }
            ?: return
        if (procesando) return
        procesando = true
        viewModelScope.launch {
            _resultado.value = ScanResult(repo.byBarcode(codigo), codigo)
        }
    }

    fun reiniciar() {
        procesando = false
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
    var permiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido -> permiso = concedido }

    LaunchedEffect(Unit) {
        if (!permiso) lanzadorPermiso.launch(Manifest.permission.CAMERA)
    }

    val resultado by vm.resultado.collectAsStateWithLifecycle()
    LaunchedEffect(resultado) {
        val r = resultado ?: return@LaunchedEffect
        val producto = r.producto
        if (producto != null) onFound(producto.localId) else onNotFound(r.codigo)
    }

    Box(Modifier.fillMaxSize()) {
        if (permiso) {
            CameraAnalyzer(onCode = vm::onBarcode)
            Text(
                text = "Apunta al código de barras",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp),
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Se necesita permiso de cámara para escanear.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(32.dp),
                )
            }
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            Icon(Icons.Filled.Close, "Cerrar", tint = Color.White)
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
 * Vincula Preview + ImageAnalysis al ciclo de vida del PreviewView.
 * Se hace en `update` (no en LaunchedEffect) para que el UseCase siempre apunte
 * al PreviewView ya adjunto a la composición y no se fugue tras un recompose.
 */
private fun bindCamera(
    previewView: PreviewView,
    scanner: BarcodeScanner,
    executor: java.util.concurrent.Executor,
    onCode: (String?) -> Unit,
) {
    previewView.tag?.let { return } // ya vinculado a esta vista
    val future = ProcessCameraProvider.getInstance(previewView.context)
    previewView.tag = future
    future.addListener({
        runCatching {
            val provider = future.get()
            val lifecycleOwner = previewView.findViewTreeLifecycleOwner() ?: return@runCatching
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
                        val mediaImage = proxy.image
                        if (mediaImage == null) {
                            proxy.close()
                            return@setAnalyzer
                        }
                        val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                        scanner.process(input)
                            .addOnSuccessListener { codes ->
                                onCode(codes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue)
                            }
                            .addOnCompleteListener { proxy.close() }
                    }
                }

            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                UseCaseGroup.Builder().addUseCase(preview).addUseCase(analysis).build(),
            )
        }
    }, executor)
}

