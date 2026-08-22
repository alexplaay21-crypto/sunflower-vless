package com.sunflower.utilityproxy.tunnel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sunflower.utilityproxy.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * VpnService поверх TunnelManager. Поднимает TUN через стандартный
 * VpnService.Builder и передаёт fd + Xray-конфиг (уже содержащий inbound
 * protocol="tun", см. XrayConfigBuilder) в TunnelManager.connect().
 *
 * Сам fd передаётся В Xray НЕ через JSON, а как реальная переменная
 * окружения процесса XRAY_TUN_FD — это делает XrayTunnelEngine прямо
 * перед вызовом runXrayFromJson (см. его doc-комментарий и
 * github.com/XTLS/Xray-core/blob/main/proxy/tun/README.md).
 *
 * Работает как foreground-сервис с постоянным уведомлением — без этого
 * Android агрессивно убивает долгоживущие VPN-сервисы в фоне. Канал
 * IMPORTANCE_LOW — без звука, чтобы не раздражать статусом "подключено".
 *
 * ВАЖНО: даже при полностью верном коде ниже, реального туннелирования
 * трафика не будет, пока LibXrayBridge не подключён к настоящему AAR
 * (см. build-libxray-aar.yml) — establish() поднимет TUN-интерфейс, но
 * TunnelManager.connect() получит Failure.
 *
 * НЕ РЕАЛИЗОВАНО: allowLan, killSwitch, per-app routing — Builder ниже
 * нарочно самый простой (full-tunnel, без исключений), пункты 26-29
 * промта пока не реализованы в UI.
 */
@AndroidEntryPoint
class SunflowerVpnService : VpnService() {

    @Inject
    lateinit var tunnelManager: TunnelManager

    private var tunInterface: ParcelFileDescriptor? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val serverId = intent?.getLongExtra(EXTRA_SERVER_ID, -1L) ?: -1L
        val configJson = intent?.getStringExtra(EXTRA_XRAY_CONFIG_JSON)

        if (serverId == -1L || configJson.isNullOrBlank()) {
            Log.e(TAG, "onStartCommand без serverId/конфига — останавливаемся")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())

        serviceScope.launch {
            val fd = establishTunnel()
            if (fd == null) {
                Log.e(TAG, "Не удалось поднять TUN-интерфейс")
                stopSelf()
                return@launch
            }
            tunnelManager.connect(serverId, fd.fd, configJson)
        }

        return START_STICKY
    }

    private fun establishTunnel(): ParcelFileDescriptor? {
        tunInterface?.close()
        val builder = Builder()
            .setSession("Sunflower")
            .addAddress(TUN_ADDRESS, TUN_PREFIX_LENGTH)
            .addDnsServer(DEFAULT_DNS)
            .addRoute("0.0.0.0", 0)
            .setMtu(TUN_MTU)
        tunInterface = builder.establish()
        return tunInterface
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(CHANNEL_ID, "Sunflower VPN", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Статус VPN-подключения"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sunflower")
            .setContentText("VPN активен")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        serviceScope.launch { tunnelManager.disconnect() }
        tunInterface?.close()
        tunInterface = null
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        onDestroy()
        super.onRevoke()
    }

    companion object {
        private const val TAG = "SunflowerVpnService"
        private const val CHANNEL_ID = "sunflower_vpn_status"
        private const val NOTIFICATION_ID = 1
        const val EXTRA_SERVER_ID = "extra_server_id"
        const val EXTRA_XRAY_CONFIG_JSON = "extra_xray_config_json"
        private const val TUN_ADDRESS = "10.10.10.1"
        private const val TUN_PREFIX_LENGTH = 24
        private const val DEFAULT_DNS = "1.1.1.1"
        private const val TUN_MTU = 1500
    }
}
