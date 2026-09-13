package com.dusk0382.cecosesolaprecios.data.remote

import com.dusk0382.cecosesolaprecios.data.remote.dto.GraphqlEnvelope
import com.dusk0382.cecosesolaprecios.data.remote.dto.PreciosRepoDto
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Clientes HTTP con kotlinx-serialization puro (sin Retrofit):
 * dos endpoints no justifican la dependencia, y el plugin de serialization
 * genera los adapters (amigable con R8 — nada de reflexión).
 */
class HttpClients(val jsonFormat: Json) {

    /** Rápido (CDN de GitHub). */
    val fast = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /** La API oficial tarda 7–40s en responder; timeouts largos y sin reintentos
     *  agresivos para no martillearla. */
    val slow: OkHttpClient = fast.newBuilder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(150, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()
}

class RepoApi(private val client: HttpClients) {

    fun getPrecios(): PreciosRepoDto {
        val request = Request.Builder().url(URL).get().build()
        return client.fast.newCall(request).execute().use { resp ->
            check(resp.isSuccessful) { "HTTP ${resp.code}" }
            client.jsonFormat.decodeFromString(PreciosRepoDto.serializer(), resp.body!!.string())
        }
    }

    companion object {
        const val URL = "https://raw.githubusercontent.com/dusk0382/cecosesola-data/main/precios.json"
    }
}

class OfficialApi(private val client: HttpClients) {

    /** Devuelve el JSON string interno (`data.downloadFile`) o null si algo falla.
     *  Lento por naturaleza: el llamador lo espera en un worker de background. */
    fun downloadFile(): String? {
        val body = GraphQL_BODY.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(URL).post(body).build()
        return client.slow.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val text = resp.body!!.string()
            runCatching {
                client.jsonFormat.decodeFromString(GraphqlEnvelope.serializer(), text)
                    .data?.downloadFile
            }.getOrNull()
        }
    }

    companion object {
        const val URL = "https://kana.imk.cecosesola.coop/graphql"
        const val GraphQL_BODY = """{"query":"query { downloadFile }"}"""
    }
}
