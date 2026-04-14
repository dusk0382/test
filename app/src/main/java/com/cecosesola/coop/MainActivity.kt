package com.cecosesola.coop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cecosesola.coop.presentation.ui.MainScreen
import com.cecosesola.coop.presentation.ui.theme.CecosesolaTheme
import com.cecosesola.coop.presentation.viewmodel.MainViewModel
import com.cecosesola.coop.workers.SyncWorker

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // enableEdgeToEdge debe llamarse ANTES de super.onCreate y de setContent.
        // Sin esto en Android 15+ el sistema fuerza edge-to-edge de todas formas
        // pero sin que la app gestione los insets, causando contenido tapado por
        // la barra de estado o la barra de navegación.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // SyncWorker.schedule solo necesita correr una vez, no en cada rotación.
        // savedInstanceState == null significa que es la primera creación de la Activity,
        // no una reconfiguración (rotación, cambio de idioma, etc.).
        if (savedInstanceState == null) {
            SyncWorker.schedule(this)
        }

        val repository = (application as CecosesolaApp).appContainer.repository

        setContent {
            CecosesolaTheme {
                // ViewModelProvider.Factory con lambda — más conciso y equivalente
                val viewModel: MainViewModel = viewModel(
                    factory = androidx.lifecycle.ViewModelProvider.Factory.from(
                        // initializer disponible desde lifecycle-viewmodel 2.6+
                        androidx.lifecycle.viewmodel.initializer { MainViewModel(repository) }
                    )
                )
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
