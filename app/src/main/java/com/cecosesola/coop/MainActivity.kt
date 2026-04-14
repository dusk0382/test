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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            SyncWorker.schedule(this)
        }

        val repository = (application as CecosesolaApp).appContainer.repository

        setContent {
            CecosesolaTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return MainViewModel(repository) as T
                        }
                    }
                )
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
