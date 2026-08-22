package com.sunflower.utilityproxy.data

import com.sunflower.utilityproxy.data.local.ServerDao
import com.sunflower.utilityproxy.data.local.ServerEntity
import com.sunflower.utilityproxy.data.local.SubscriptionDao
import com.sunflower.utilityproxy.data.local.SubscriptionEntity
import com.sunflower.utilityproxy.parsing.ParseResult
import com.sunflower.utilityproxy.parsing.ServerUriParser
import com.sunflower.utilityproxy.parsing.SubscriptionDecoder
import com.sunflower.utilityproxy.parsing.toEntity
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class SubscriptionRepository @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    private val serverDao: ServerDao,
    private val decoder: SubscriptionDecoder,
    private val parsers: Set<@JvmSuppressWildcards ServerUriParser>,
    private val okHttpClient: OkHttpClient,
) {
    private companion object {
        const val TAG = "SubscriptionRepository"
    }

    // Строится лениво и используется ТОЛЬКО когда конкретная подписка явно
    // включила allowInsecure — обычный okHttpClient остаётся со строгой
    // проверкой TLS по умолчанию (пункт 49 промта).
    private val insecureClient: OkHttpClient by lazy { buildInsecureClient() }

    fun observeSubscriptions(): Flow<List<SubscriptionEntity>> = subscriptionDao.observeAll()

    fun observeServers(subscriptionId: Long): Flow<List<ServerEntity>> =
        serverDao.observeBySubscription(subscriptionId)

    /**
     * Пайплайн из пункта 6 промта: URL -> GET -> decode -> parse -> servers -> save.
     * age-расшифровка НЕ реализована — libXray её не предоставляет (см.
     * ARCHITECTURE.md), поэтому зашифрованные подписки честно проваливаются
     * с понятным сообщением, а не притворяются, что что-то расшифровали.
     */
    suspend fun importFromUrl(
        name: String,
        url: String,
        allowInsecure: Boolean,
        sendHwidCookie: Boolean,
        hwid: String?,
        encrypted: Boolean,
    ): ParseResult<Long> {
        if (encrypted) {
            return ParseResult.Failure("Функция требует настройки age.secretKey — расшифровка подписок пока не реализована.")
        }

        val body = try {
            fetchSubscription(url, allowInsecure, sendHwidCookie, hwid)
        } catch (e: IOException) {
            return ParseResult.Failure("Не удалось загрузить подписку: ${e.message}")
        }

        val parsedServers = when (val decoded = decoder.decode(body)) {
            is ParseResult.Success -> decoded.value
            is ParseResult.Failure -> return decoded
        }
        if (parsedServers.isEmpty()) {
            return ParseResult.Failure("Подписка загружена, но не удалось распознать ни одного сервера.")
        }

        val now = System.currentTimeMillis()
        val subscriptionId = subscriptionDao.upsert(
            SubscriptionEntity(
                name = name.ifBlank { url },
                url = url,
                createdAt = now,
                updatedAt = now,
                allowInsecure = allowInsecure,
                sendHwidCookie = sendHwidCookie,
                encrypted = encrypted,
            ),
        )

        serverDao.replaceForSubscription(subscriptionId, parsedServers.map { it.toEntity(subscriptionId) })
        return ParseResult.Success(subscriptionId)
    }

    /**
     * Обновление подписки: pull -> parse -> validate -> atomic replace.
     * При ошибке старые серверы НЕ трогаем (пункты 13/46/47 промта) — только
     * пишем lastUpdateError, чтобы показать его в UI.
     */
    suspend fun refresh(subscription: SubscriptionEntity): ParseResult<Unit> {
        val body = try {
            fetchSubscription(subscription.url, subscription.allowInsecure, subscription.sendHwidCookie, hwid = null)
        } catch (e: IOException) {
            markUpdateError(subscription, "Не удалось обновить подписку: ${e.message}")
            return ParseResult.Failure("Не удалось обновить подписку.")
        }

        val parsedServers = when (val decoded = decoder.decode(body)) {
            is ParseResult.Success -> decoded.value
            is ParseResult.Failure -> {
                markUpdateError(subscription, decoded.reason)
                return decoded
            }
        }
        if (parsedServers.isEmpty()) {
            markUpdateError(subscription, "Обновление вернуло 0 серверов — старые серверы сохранены.")
            return ParseResult.Failure("Обновление вернуло 0 серверов.")
        }

        val entities = parsedServers.map { it.toEntity(subscription.id, readOnly = subscription.hideServerSettings) }
        serverDao.replaceForSubscription(subscription.id, entities)
        subscriptionDao.update(subscription.copy(updatedAt = System.currentTimeMillis(), lastUpdateError = null))
        return ParseResult.Success(Unit)
    }

    suspend fun delete(subscription: SubscriptionEntity) = subscriptionDao.delete(subscription)

    /**
     * Пункты 35/36 промта: deep link вида vless://... — отдельный
     * сервер, а не подписка (никакого HTTP-запроса, никакой
     * SubscriptionImporter-логики). Сохраняется как Local
     * (subscriptionId = null).
     */
    suspend fun importSingleServerUri(uri: String): ParseResult<Long> {
        val scheme = uri.substringBefore("://", missingDelimiterValue = "")
        val parser = parsers.associateBy { it.scheme }[scheme]
            ?: return ParseResult.Failure("Неподдерживаемый формат.")
        return when (val parsed = parser.parse(uri)) {
            is ParseResult.Success -> {
                val id = serverDao.insertAll(listOf(parsed.value.toEntity(subscriptionId = null))).first()
                ParseResult.Success(id)
            }
            is ParseResult.Failure -> parsed
        }
    }

    suspend fun deleteAllSubscriptions() = subscriptionDao.deleteAll()

    suspend fun deleteAllServers() = serverDao.deleteAll()

    suspend fun setFavorite(serverId: Long, isFavorite: Boolean) = serverDao.setFavorite(serverId, isFavorite)

    private suspend fun markUpdateError(subscription: SubscriptionEntity, reason: String) {
        AppLogger.log(TAG, "Ошибка обновления подписки ${subscription.name}: $reason")
        subscriptionDao.update(subscription.copy(lastUpdateError = reason))
    }

    private fun fetchSubscription(url: String, allowInsecure: Boolean, sendHwidCookie: Boolean, hwid: String?): String {
        val client = if (allowInsecure) insecureClient else okHttpClient
        val requestBuilder = Request.Builder().url(url)
        if (sendHwidCookie && hwid != null) {
            requestBuilder.addHeader("Cookie", "hwid=$hwid")
        }
        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            return response.body?.string() ?: throw IOException("Пустой ответ")
        }
    }

    private fun buildInsecureClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) = Unit
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            },
        )
        val sslContext = SSLContext.getInstance("TLS").apply { init(null, trustAllCerts, SecureRandom()) }
        return okHttpClient.newBuilder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }
}
