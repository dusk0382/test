package com.cecosesola.coop

import android.app.Application
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
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
                    .maxSizePercent(0.04)   // 4% de RAM — correcto para gama baja
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("ceco_imagenes"))
                    .maxSizeBytes(5L * 1024 * 1024)  // 5 MB
                    .build()
            }
            // RGB_565: 2 bytes/pixel en lugar de 4 (ARGB_8888).
            // Las imágenes de productos no tienen transparencia, así que no se pierde nada.
            .bitmapConfig(Bitmap.Config.RGB_565)
            // Sin crossfade en gama baja — evita una capa de compositing extra
            .crossfade(false)
            // Respetar Cache-Control del servidor para imágenes.
            // Sin esto Coil revalida cada imagen en cada arranque aunque no haya cambiado.
            .respectCacheHeaders(true)
            // Política de red: primero caché, luego red solo si no hay hit.
            // Para una app de precios donde las imágenes cambian raramente esto
            // reduce drásticamente las peticiones de red al abrir la app.
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
