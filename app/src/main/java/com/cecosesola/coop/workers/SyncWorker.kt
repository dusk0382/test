package com.cecosesola.coop.workers

import android.content.Context
import androidx.work.*
import com.cecosesola.coop.CecosesolaApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repo = (applicationContext as CecosesolaApp).appContainer.repository
            if (repo.refrescarSiEsNecesario().isSuccess) Result.success() else Result.retry()
        } catch (e: Exception) { Result.retry() }
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "sync_cecosesola", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.DAYS)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).setRequiresCharging(true).build())
                    .setInitialDelay(calculateDelayTo5AM(), TimeUnit.MILLISECONDS).build()
            )
        }
        private fun calculateDelayTo5AM(): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 5); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            if (now.after(target)) target.add(Calendar.DAY_OF_MONTH, 1)
            return target.timeInMillis - now.timeInMillis
        }
    }
}
