package com.sunflower.utilityproxy

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Точка входа Hilt. Также поставляет HiltWorkerFactory для WorkManager,
 * чтобы SubscriptionUpdateWorker мог получать зависимости через @Inject.
 *
 * НЕ ПРОВЕРЕНО живой сборкой: форма Configuration.Provider (val-свойство
 * ниже, а не getWorkManagerConfiguration()) соответствует недавним версиям
 * WorkManager, но конкретно для 2.10.4 не сверялась построчно с исходником —
 * если сборка упадёт на этом файле, это первое место для проверки.
 */
@HiltAndroidApp
class SunflowerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
