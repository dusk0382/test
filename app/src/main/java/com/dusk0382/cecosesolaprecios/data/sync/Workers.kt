package com.dusk0382.cecosesolaprecios.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dusk0382.cecosesolaprecios.data.repository.ProductRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Sync rápida: mirror del usuario en GitHub (CDN, ~100KB). */
@HiltWorker
class BaseSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: ProductRepository,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result =
        runCatching { repo.syncBase() }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
}

/** Enriquecimiento: GraphQL oficial (lento, ~1MB, 7–40s). Fallar es normal —
 *  retry con backoff lo reintenta mañana y la app sigue con lo que hay. */
@HiltWorker
class EnrichSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: ProductRepository,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result =
        runCatching { repo.syncEnrich() }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
}
