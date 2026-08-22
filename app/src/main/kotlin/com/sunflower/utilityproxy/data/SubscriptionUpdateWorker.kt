package com.sunflower.utilityproxy.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sunflower.utilityproxy.parsing.ParseResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Пункт 14 промта: автообновление подписок через WorkManager. Не трогает
 * старые серверы при ошибке одной подписки (см. SubscriptionRepository.refresh) —
 * одна упавшая подписка не должна ронять весь Worker.
 *
 * ВАЖНО: планирование (Never/6h/12h/Daily) и запрет фоновых запросов при
 * выключенном автообновлении — на стороне UI/SettingsRepository, здесь
 * только сама работа. Настройки для выбора интервала пока нет в Settings-
 * экране (см. ARCHITECTURE.md), поэтому WorkManager.enqueue с нужным
 * PeriodicWorkRequest тоже пока не подключён нигде — только сам Worker.
 */
@HiltWorker
class SubscriptionUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: SubscriptionRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val subscriptions = repository.observeSubscriptions().first()
            var anyFailure = false
            subscriptions.forEach { subscription ->
                if (repository.refresh(subscription) is ParseResult.Failure) {
                    anyFailure = true
                }
            }
            if (anyFailure) Result.retry() else Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "subscription_auto_update"
    }
}
