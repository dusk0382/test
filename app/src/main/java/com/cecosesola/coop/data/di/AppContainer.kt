package com.cecosesola.coop.data.di

import android.content.Context
import com.cecosesola.coop.data.local.PreciosDatabase
import com.cecosesola.coop.data.remote.GitHubApiService
import com.cecosesola.coop.data.repository.PreciosRepository

class AppContainer(context: Context) {
    private val database by lazy { PreciosDatabase.getInstance(context) }
    private val apiService by lazy { GitHubApiService.create() }
    
    val repository by lazy { 
        PreciosRepository(
            preciosDao = database.preciosDao(),
            metadataDao = database.metadataDao(),
            favoritosDao = database.favoritosDao(),
            busquedasDao = database.busquedasDao(),
            apiService = apiService,
            networkMonitor = com.cecosesola.coop.presentation.utils.NetworkMonitor(context)
        )
    }
}
