package com.sunflower.utilityproxy.tunnel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sunflower.utilityproxy.data.AppLogger
import com.sunflower.utilityproxy.data.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Пункт 30/31 промта: автозапуск при загрузке устройства — но ТОЛЬКО если
 * пользователь явно включил это в настройках (SettingsRepository.autostartOnBoot).
 * BOOT_COMPLETED сам по себе не означает автоматический запуск чего-либо —
 * промт прямо просит не запускать соединение без сохранённого выбранного
 * сервера (пункт 31).
 *
 * НЕ РЕАЛИЗОВАНО: реально запомнить "последний выбранный сервер" и
 * подключиться к нему при загрузке — сейчас только читает флаг и
 * логирует. Появится, когда на экране "Серверы" будет понятие "последний
 * использованный/сервер по умолчанию".
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val enabled = settingsRepository.autostartOnBoot.first()
            if (enabled) {
                AppLogger.log(TAG, "BOOT_COMPLETED: автозапуск включён, но выбор последнего сервера пока не реализован")
                Log.i(TAG, "Автозапуск включён, но соединение не запущено: нет сохранённого выбора сервера")
            } else {
                Log.i(TAG, "BOOT_COMPLETED получен, автозапуск выключен в настройках — ничего не делаем")
            }
        }
    }

    private companion object {
        const val TAG = "BootCompletedReceiver"
    }
}
