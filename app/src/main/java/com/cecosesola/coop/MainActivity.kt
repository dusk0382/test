package com.cecosesola.coop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cecosesola.coop.data.di.AppContainer
import com.cecosesola.coop.presentation.ui.MainScreen
import com.cecosesola.coop.presentation.ui.theme.CecosesolaTheme
import com.cecosesola.coop.presentation.viewmodel.MainViewModel
import com.cecosesola.coop.workers.SyncWorker

class MainActivity : ComponentActivity() {
    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as CecosesolaApp
        appContainer = app.appContainer
        SyncWorker.schedule(this)

        setContent {
            CecosesolaTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return MainViewModel(appContainer.repository) as T
                        }
                    }
                )
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
