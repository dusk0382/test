package com.cecosesola.coop.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
        private const val BASE_URL = "https://raw.githubusercontent.com/dusk0382/cecosesola-data/main/"
        
        private fun getUnsafeOkHttpClient(): OkHttpClient.Builder {
            return try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())
                
                OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                    .hostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                OkHttpClient.Builder()
            }
        }
        
        fun create(): GitHubApiService {
            val client = getUnsafeOkHttpClient()
                .addInterceptor(HttpLoggingInterceptor().apply { 
                    level = HttpLoggingInterceptor.Level.BASIC 
                })
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
                
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GitHubApiService::class.java)
        }
    }
}
