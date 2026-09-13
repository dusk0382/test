package com.dusk0382.cecosesolaprecios

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.dusk0382.cecosesolaprecios.data.remote.HttpClients
import com.dusk0382.cecosesolaprecios.data.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CecosesolaApp : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var http: HttpClients

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /** Coil global con el OkHttp compartido: reusa el connection pool y el
     *  dispatcher (evita un segundo pool de threads — clave en gama baja). */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { http.fast }))
            }
            .memoryCache {
                coil3.memory.MemoryCache.Builder()
                    .maxSizeBytes(12L * 1024 * 1024) // ~2–3GB de RAM: 12MB de bitmaps sobran
                    .build()
            }
            .crossfade(false) // sin animación por imagen: Mali-G52 lo agradece
            .build()

    override fun onCreate() {
        super.onCreate()
        SyncScheduler.schedule(this)
    }
}
