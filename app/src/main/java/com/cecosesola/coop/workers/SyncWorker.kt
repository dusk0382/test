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
            if (repo.refrescarSiEsNecesario().isSuccess) Result.success()
            else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            // Constraints mínimos: solo necesita red (cualquier tipo).
            // ELIMINADO: setRequiresCharging(true) — en dispositivos de gama baja
            // raramente están cargando mientras se usan, así que la sync nunca corría.
            // ELIMINADO: setRequiredNetworkType(UNMETERED) → ahora acepta cualquier red
            // (el JSON de precios es pequeño, <50KB, no justifica esperar solo WiFi).
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "sync_cecosesola",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.DAYS)
                    .setConstraints(constraints)
                    .setInitialDelay(calculateDelayTo5AM(), TimeUnit.MILLISECONDS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                    .build()
            )
        }

        private fun calculateDelayTo5AM(): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 5)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (now.after(target)) target.add(Calendar.DAY_OF_MONTH, 1)
            return target.timeInMillis - now.timeInMillis
        }
    }
}
