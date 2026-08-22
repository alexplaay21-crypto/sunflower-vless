package com.sunflower.utilityproxy.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sunflower_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val KILL_SWITCH = booleanPreferencesKey("kill_switch")
        val ALLOW_LAN = booleanPreferencesKey("allow_lan")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val THEME = stringPreferencesKey("theme") // "system" | "light" | "dark"
        val DEVICE_ID = stringPreferencesKey("device_id") // пункт 4: НЕ IMEI/IMSI, случайный UUID
        val AUTOSTART_ON_BOOT = booleanPreferencesKey("autostart_on_boot")
        val UPDATE_INTERVAL = stringPreferencesKey("update_interval") // "never"|"6h"|"12h"|"daily", пункт 14
    }

    val killSwitch: Flow<Boolean> = context.dataStore.data.map { it[Keys.KILL_SWITCH] ?: false }
    val allowLan: Flow<Boolean> = context.dataStore.data.map { it[Keys.ALLOW_LAN] ?: false }
    val autoConnect: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_CONNECT] ?: false }
    val theme: Flow<String> = context.dataStore.data.map { it[Keys.THEME] ?: "system" }
    val autostartOnBoot: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTOSTART_ON_BOOT] ?: false }
    val updateInterval: Flow<String> = context.dataStore.data.map { it[Keys.UPDATE_INTERVAL] ?: "never" }

    suspend fun setKillSwitch(enabled: Boolean) = context.dataStore.edit { it[Keys.KILL_SWITCH] = enabled }
    suspend fun setAllowLan(enabled: Boolean) = context.dataStore.edit { it[Keys.ALLOW_LAN] = enabled }
    suspend fun setAutoConnect(enabled: Boolean) = context.dataStore.edit { it[Keys.AUTO_CONNECT] = enabled }
    suspend fun setTheme(value: String) = context.dataStore.edit { it[Keys.THEME] = value }
    suspend fun setAutostartOnBoot(enabled: Boolean) = context.dataStore.edit { it[Keys.AUTOSTART_ON_BOOT] = enabled }
    suspend fun setUpdateInterval(value: String) = context.dataStore.edit { it[Keys.UPDATE_INTERVAL] = value }

    /** Пункт 34: "Сбросить настройки" — очищает переключатели, deviceId намеренно сохраняется (это не "настройка"). */
    suspend fun resetSettings() {
        context.dataStore.edit { prefs ->
            val deviceId = prefs[Keys.DEVICE_ID]
            prefs.clear()
            deviceId?.let { prefs[Keys.DEVICE_ID] = it }
        }
    }

    /**
     * Стабильный случайный application-level device id для HWID-cookie
     * (пункт 4 промта). Генерируется один раз внутри одной DataStore-
     * транзакции и переиспользуется — НЕ IMEI/IMSI, НЕ меняется при
     * каждом запросе.
     */
    suspend fun getOrCreateDeviceId(): String {
        var result = ""
        context.dataStore.edit { prefs ->
            val existing = prefs[Keys.DEVICE_ID]
            result = existing ?: UUID.randomUUID().toString().also { prefs[Keys.DEVICE_ID] = it }
        }
        return result
    }
}
