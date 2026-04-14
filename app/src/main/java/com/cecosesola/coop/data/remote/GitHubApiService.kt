package com.cecosesola.coop.data.remote

import com.cecosesola.coop.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.io.File
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class PreciosResponse(
    @Json(name = "productos") val productos: List<ProductoRemoto>,
    @Json(name = "fecha_actualizacion") val fechaActualizacion: String
)

@JsonClass(generateAdapter = true)
data class ProductoRemoto(
    @Json(name = "id") val id: String,
    @Json(name = "nombre") val nombre: String,
    @Json(name = "precio") val precio: Double,
    @Json(name = "categoria") val categoria: String?,
    @Json(name = "imagen") val imagen: String?,
    @Json(name = "presentacion") val presentacion: String = ""
)

interface GitHubApiService {
    @GET("precios.json")
    suspend fun getPrecios(): PreciosResponse

    companion object {
        private const val BASE_URL = "https://raw.githubusercontent.com/dusk0382/cecosesola-data/main/"
        private const val CACHE_SIZE = 1L * 1024 * 1024

        fun create(cacheDir: File): GitHubApiService {
            val cache = Cache(File(cacheDir, "http_cache"), CACHE_SIZE)
            val revalidateInterceptor = Interceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .cacheControl(CacheControl.Builder().maxAge(0, TimeUnit.SECONDS).build())
                        .build()
                )
            }
            val clientBuilder = OkHttpClient.Builder()
                .cache(cache)
                .addNetworkInterceptor(revalidateInterceptor)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
            if (BuildConfig.DEBUG) {
                clientBuilder.addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
                )
            }
            val moshi = Moshi.Builder().build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(clientBuilder.build())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GitHubApiService::class.java)
        }
    }
}
