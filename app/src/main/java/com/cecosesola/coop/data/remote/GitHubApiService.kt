package com.cecosesola.coop.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.File
import java.util.concurrent.TimeUnit

data class PreciosResponse(
    @SerializedName("productos") val productos: List<ProductoRemoto>,
    @SerializedName("fecha_actualizacion") val fechaActualizacion: String
)

data class ProductoRemoto(
    @SerializedName("id") val id: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("precio") val precio: Double,
    @SerializedName("categoria") val categoria: String?,
    @SerializedName("imagen") val imagen: String?,
    @SerializedName("presentacion") val presentacion: String = ""
)

interface GitHubApiService {
    @GET("precios.json")
    suspend fun getPrecios(): PreciosResponse

    companion object {
        private const val BASE_URL =
            "https://raw.githubusercontent.com/dusk0382/cecosesola-data/main/"

        // Caché de 1 MB — el JSON de precios es pequeño, esto evita re-descargar
        // si el servidor responde con los mismos datos (ETag / Last-Modified).
        private const val CACHE_SIZE = 1L * 1024 * 1024

        fun create(cacheDir: File): GitHubApiService {
            val cache = Cache(File(cacheDir, "http_cache"), CACHE_SIZE)

            // Interceptor que fuerza revalidación en cada llamada explícita pero
            // permite usar la caché cuando el servidor confirma "304 Not Modified".
            // Esto ahorra parsear y guardar en Room si nada cambió.
            val revalidateInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .cacheControl(CacheControl.Builder()
                        .maxAge(0, TimeUnit.SECONDS)   // siempre revalidar
                        .build())
                    .build()
                chain.proceed(request)
            }

            val clientBuilder = OkHttpClient.Builder()
                .cache(cache)
                .addNetworkInterceptor(revalidateInterceptor)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                // Sin SSL personalizado — raw.githubusercontent.com tiene certificado
                // válido. El cliente de sistema ya lo maneja correctamente.

            // Logging solo en builds de depuración para no penalizar release
            if (BuildConfig.DEBUG) {
                clientBuilder.addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
                )
            }

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GitHubApiService::class.java)
        }
    }
}
