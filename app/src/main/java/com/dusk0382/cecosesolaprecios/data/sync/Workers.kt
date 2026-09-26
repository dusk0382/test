package com.dusk0382.cecosesolaprecios.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dusk0382.cecosesolaprecios.data.remote.HttpStatusException
import com.dusk0382.cecosesolaprecios.data.repository.ProductRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Sync rápida: mirror del usuario en GitHub (CDN, ~100KB).
 */
@HiltWorker
class BaseSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: ProductRepository,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result =
        ejecutarSync("sync_base", runAttemptCount) { repo.syncBase() }
}

/** Enriquecimiento: GraphQL oficial (lento, ~1MB, 7–40s). Fallar es normal —
 *  el backoff lo reintenta y la app sigue con lo que ya tiene. */
@HiltWorker
class EnrichSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: ProductRepository,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result =
        ejecutarSync("sync_enrich", runAttemptCount) { repo.syncEnrich() }
}

/**
 * Decide qué hacer con el resultado de una sincronización, y deja rastro.
 *
 * Antes: `runCatching { … }.fold(success, retry)` descartaba el throwable y
 * convertía cualquier error —incluido un cambio de schema o un 404— en un
 * `Result.retry()` mudo, para siempre. Sin log y sin dead-letter, "mis datos
 * están viejos" en el teléfono era imposible de diagnosticar.
 *
 * Ahora:
 * - Un **4xx es permanente** (el repo del mirror se renombró, el path cambió):
 *   cortar con `failure()` en vez de reintentar cada 6 h para siempre.
 * - Cualquier otro fallo reintenta hasta [MAX_INTENTOS] y después corta, para no
 *   dejar la cola envenenada (con `KEEP` una cola envenenada no se reemplaza
 *   nunca, así que el corte es lo que permite que el siguiente ciclo pruebe de
 *   nuevo).
 */
private suspend fun ejecutarSync(
    nombre: String,
    intento: Int,
    bloque: suspend () -> Boolean,
): Result = try {
    if (bloque()) Log.i(TAG, "$nombre: sincronizado")
    else Log.i(TAG, "$nombre: sin cambios (idempotencia)")
    Result.success()
} catch (e: HttpStatusException) {
    if (e.code in 400..499) {
        Log.e(TAG, "$nombre: HTTP ${e.code} en ${e.url} — permanente, no se reintenta", e)
        Result.failure()
    } else {
        Log.w(TAG, "$nombre: HTTP ${e.code} — intento ${intento + 1}/$MAX_INTENTOS", e)
        Result.retry()
    }
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e
} catch (e: Exception) {
    if (intento + 1 >= MAX_INTENTOS) {
        Log.e(TAG, "$nombre: se agotaron los $MAX_INTENTOS intentos — ${e.message}", e)
        Result.failure()
    } else {
        Log.w(TAG, "$nombre: ${e.message} — intento ${intento + 1}/$MAX_INTENTOS", e)
        Result.retry()
    }
}

private const val TAG = "CecoSync"
private const val MAX_INTENTOS = 5
