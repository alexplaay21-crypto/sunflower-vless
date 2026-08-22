package com.sunflower.utilityproxy.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Пункт 14 промта: Never/Every 6 hours/Every 12 hours/Daily. Если
 * пользователь выключил автообновление — фоновых запросов быть не
 * должно (пункт 14: "Если пользователь отключил автообновление: никаких
 * фоновых запросов"), поэтому "never" явно отменяет работу, а не просто
 * не планирует новую.
 */
@Singleton
class SubscriptionUpdateScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun apply(interval: String) {
        val workManager = WorkManager.getInstance(context)

        val (amount, unit) = when (interval) {
            "6h" -> 6L to TimeUnit.HOURS
            "12h" -> 12L to TimeUnit.HOURS
            "daily" -> 24L to TimeUnit.HOURS
            else -> {
                workManager.cancelUniqueWork(SubscriptionUpdateWorker.WORK_NAME)
                return
            }
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<SubscriptionUpdateWorker>(amount, unit)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SubscriptionUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request,
        )
    }
}
