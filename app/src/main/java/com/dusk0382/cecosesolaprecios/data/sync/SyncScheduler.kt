package com.dusk0382.cecosesolaprecios.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {

    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)
        val connected = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // El scraper del usuario corre Jue–Dom 3×/día; 6h es más que suficiente
        // y cuida la batería de un gama baja.
        wm.enqueueUniquePeriodicWork(
            "sync_base",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BaseSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(connected)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build(),
        )

        wm.enqueueUniquePeriodicWork(
            "sync_enrich",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<EnrichSyncWorker>(1, TimeUnit.DAYS)
                .setConstraints(connected)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.HOURS)
                .build(),
        )
    }
}
