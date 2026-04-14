package com.cecosesola.coop

import android.app.Application
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.cecosesola.coop.data.di.AppContainer
import timber.log.Timber

class CecosesolaApp : Application(), ImageLoaderFactory {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        appContainer = AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.04) // 4% de RAM (antes 8%)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("ceco_imagenes"))
                    .maxSizeBytes(5 * 1024 * 1024) // 5 MB máximo (antes 15 MB)
                    .build()
            }
            .bitmapConfig(Bitmap.Config.RGB_565) // 2 bytes/pixel
            .crossfade(0) // Sin animación
            .build()
    }
}
