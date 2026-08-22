package com.sunflower.utilityproxy.tunnel

import android.util.Log
import com.sunflower.utilityproxy.data.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Единственный источник истины о состоянии туннеля. Специально сделан
 * строгим синглтоном с Mutex вокруг connect/disconnect: libXray хранит
 * DNS-клиент и outbound-менеджер Xray-core в состоянии на весь процесс, и
 * параллельный запуск второго инстанса (например, pingBatch) может испортить
 * уже работающий (см. ARCHITECTURE.md, находка про concurrency). Поэтому
 * "Тест всех" не должен запускаться, пока canRunBulkPingTest() == false —
 * это обязана проверить вызывающая сторона (ViewModel) перед запуском теста.
 */
@Singleton
class TunnelManager @Inject constructor(
    private val engine: TunnelEngine,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<TunnelState>(TunnelState.Disconnected)
    val state: StateFlow<TunnelState> = _state.asStateFlow()

    suspend fun connect(serverId: Long, tunFd: Int, xrayConfigJson: String) {
        mutex.withLock {
            if (_state.value is TunnelState.Connected || _state.value is TunnelState.Connecting) {
                Log.w(TAG, "connect() вызван при уже активном туннеле — игнорируем")
                return
            }
            _state.value = TunnelState.Connecting
            AppLogger.log(TAG, "Подключение к серверу id=$serverId")
            when (val result = engine.start(tunFd, xrayConfigJson)) {
                is LibXrayResult.Success -> {
                    _state.value = TunnelState.Connected(serverId, System.currentTimeMillis())
                    AppLogger.log(TAG, "Подключено, serverId=$serverId")
                }
                is LibXrayResult.Failure -> {
                    _state.value = TunnelState.Error(result.error)
                    AppLogger.log(TAG, "Ошибка подключения: ${result.error}")
                }
            }
        }
    }

    suspend fun disconnect() {
        mutex.withLock {
            AppLogger.log(TAG, "Отключение")
            when (val result = engine.stop()) {
                is LibXrayResult.Success -> _state.value = TunnelState.Disconnected
                is LibXrayResult.Failure -> _state.value = TunnelState.Error(result.error)
            }
        }
    }

    fun canRunBulkPingTest(): Boolean =
        _state.value !is TunnelState.Connected && _state.value !is TunnelState.Connecting

    private companion object {
        const val TAG = "TunnelManager"
    }
}
