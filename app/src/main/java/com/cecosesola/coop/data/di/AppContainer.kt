package com.cecosesola.coop.data.di

import android.content.Context
import com.cecosesola.coop.data.local.PreciosDatabase
import com.cecosesola.coop.data.remote.GitHubApiService
import com.cecosesola.coop.data.repository.PreciosRepository
import com.cecosesola.coop.presentation.utils.NetworkMonitor

class AppContainer(context: Context) {

    // applicationContext evita memory leaks si AppContainer vive más que una Activity
    private val appContext = context.applicationContext

    private val database by lazy { PreciosDatabase.getInstance(appContext) }

    // NetworkMonitor como singleton — un solo registro de callbacks de red
    val networkMonitor by lazy { NetworkMonitor(appContext) }

    private val apiService by lazy {
        // Pasamos cacheDir para que OkHttp pueda cachear respuestas HTTP
        GitHubApiService.create(appContext.cacheDir)
    }

    val repository by lazy {
        PreciosRepository(
            preciosDao    = database.preciosDao(),
            metadataDao   = database.metadataDao(),
            favoritosDao  = database.favoritosDao(),
            busquedasDao  = database.busquedasDao(),
            apiService    = apiService,
            networkMonitor = networkMonitor
        )
    }
}
