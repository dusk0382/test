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
        // Sin callTimeout una conexión medio abierta se queda colgada hasta el
        // tope de 10 min de WorkManager, sin cancelación posible.
        .callTimeout(30, TimeUnit.SECONDS)
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

/**
 * Error HTTP con el código a la vista, para que el worker pueda distinguir un
 * fallo reintentable (5xx, red) de uno permanente (404: el repo del mirror se
 * renombró o se borró, y reintentar cada 6 h no lo va a arreglar nunca).
 */
class HttpStatusException(val code: Int, val url: String) :
    java.io.IOException("HTTP $code en $url")

class RepoApi(private val client: HttpClients) {

    fun getPrecios(): PreciosRepoDto {
        val request = Request.Builder().url(URL).get().build()
        return client.fast.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw HttpStatusException(resp.code, URL)
            val body = resp.body?.string()
                ?: throw java.io.IOException("HTTP ${resp.code} con cuerpo vacío en $URL")
            client.jsonFormat.decodeFromString(PreciosRepoDto.serializer(), body)
        }
    }

    companion object {
        const val URL = "https://raw.githubusercontent.com/dusk0382/cecosesola-data/main/precios.json"
    }
}

class OfficialApi(private val client: HttpClients) {

    /** Devuelve el JSON string interno (`data.downloadFile`).
     *
     *  Antes devolvía `null` tanto para un 500 como para un payload roto, y el
     *  worker reportaba `Result.success()` en los dos casos: un cambio de schema
     *  quedaba registrado como una sincronización exitosa. Ahora lanza, y quien
     *  decide si reintentar es el worker. */
    fun downloadFile(): String {
        val body = GraphQL_BODY.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(URL).post(body).build()
        return client.slow.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw HttpStatusException(resp.code, URL)
            val text = resp.body?.string()
                ?: throw java.io.IOException("HTTP ${resp.code} con cuerpo vacío en $URL")
            client.jsonFormat.decodeFromString(GraphqlEnvelope.serializer(), text)
                .data?.downloadFile
                ?: throw java.io.IOException("el envelope vino sin data.downloadFile")
        }
    }

    companion object {
        const val URL = "https://kana.imk.cecosesola.coop/graphql"
        const val GraphQL_BODY = """{"query":"query { downloadFile }"}"""
    }
}
