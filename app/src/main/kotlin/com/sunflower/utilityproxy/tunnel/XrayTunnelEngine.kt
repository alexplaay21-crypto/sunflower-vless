package com.sunflower.utilityproxy.tunnel

import android.system.ErrnoException
import android.system.Os
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ПОДТВЕРЖДЕНО по официальному источнику — github.com/XTLS/Xray-core,
 * proxy/tun/README.md (не пересказ через libXray, а сам инбаунд):
 *
 *   "Android uses the VpnService API which provides a TUN file descriptor
 *   to the application. ... Set the environment variable xray.tun.fd
 *   (or XRAY_TUN_FD) to the fd number before starting Xray. This can be
 *   done from Kotlin/Java or by exposing a Go function via gomobile
 *   bindings."
 *
 * Это НАСТОЯЩАЯ переменная окружения процесса (оба варианта имени —
 * алиасы одного и того же), а НЕ поле JSON-конфига — более ранняя версия
 * этого файла ошибочно писала fd в JSON как env.xray.tun.fd на основе
 * менее точного пересказа через документацию libXray. android.system.Os —
 * стандартный, давно стабильный Android API (с API 21), тонкая обёртка
 * над POSIX setenv(3); т.к. нативная Go-библиотека грузится в тот же
 * процесс приложения (не отдельный процесс), общее для процесса окружение
 * ей видно.
 *
 * Xray-инбаунд "tun" сам не парсит TCP/IP — пакеты проходят через
 * встроенный userspace network stack и дальше через ОБЫЧНЫЙ Xray routing,
 * так же, как с любым другим инбаундом (core-tutorial.argsment.com/xray/tun).
 * При одном-единственном outbound и без явных routing-правил весь трафик
 * идёт на него по умолчанию — второй outbound/явные правила не нужны для
 * MVP с одним сервером.
 */
@Singleton
class XrayTunnelEngine @Inject constructor(
    private val bridge: LibXrayBridge,
) : TunnelEngine {

    override suspend fun start(tunFd: Int, xrayConfigJson: String): LibXrayResult = withContext(Dispatchers.IO) {
        try {
            Os.setenv("XRAY_TUN_FD", tunFd.toString(), true)
        } catch (e: ErrnoException) {
            Log.e(TAG, "Не удалось установить XRAY_TUN_FD: ${e.message}")
            return@withContext LibXrayResult.Failure("Не удалось передать TUN fd в окружение процесса: ${e.message}")
        }
        val payload = JSONObject().put("configJSON", xrayConfigJson)
        bridge.invoke("runXrayFromJson", payload)
    }

    override suspend fun stop(): LibXrayResult = withContext(Dispatchers.IO) {
        bridge.invoke("stopXray", JSONObject())
    }

    override suspend fun isRunning(): Boolean = withContext(Dispatchers.IO) {
        when (val result = bridge.invoke("getXrayState", JSONObject())) {
            is LibXrayResult.Success -> result.data.optBoolean("running", false)
            is LibXrayResult.Failure -> false
        }
    }

    override suspend fun version(): String? = withContext(Dispatchers.IO) {
        when (val result = bridge.invoke("xrayVersion", JSONObject())) {
            is LibXrayResult.Success -> result.data.optString("version").ifBlank { null }
            is LibXrayResult.Failure -> null
        }
    }

    private companion object {
        const val TAG = "XrayTunnelEngine"
    }
}
