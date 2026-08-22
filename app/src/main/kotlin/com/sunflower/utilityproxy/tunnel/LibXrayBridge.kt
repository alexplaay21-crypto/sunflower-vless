package com.sunflower.utilityproxy.tunnel

import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Обёртка над сгенерированным gomobile-биндингом libXray.
 *
 * ПОДТВЕРЖДЕНО (github.com/xtls/libxray, invoke.go / invoke_model.go,
 * версия v1.260728.0 / v26.7.28 — см. ARCHITECTURE.md): весь API — это
 * ОДИН вызов `Invoke(requestJSON string) string`, обёрнутый в JSON-конверт:
 *
 *   запрос:  {"apiVersion":1,"method":"runXray","payload":{...}}
 *   ответ:   {"success":true,"data":{},"error":""}
 *
 * Ранее в архитектуре предполагались отдельные функции на каждый метод
 * (runXray/stopXray/...) — это было ошибочное предположение по одним
 * только именам Go-функций из README; реальный протокол оказался другим,
 * поэтому этот файл переписан после проверки полной Go-документации пакета.
 *
 * НЕ ПОДТВЕРЖДЕНО: точное имя Kotlin/Java-класса, которое получится после
 * `gomobile bind` для Android (в README пример — `LibXray.invoke(...)`,
 * но точный package/класс зависит от сборки AAR, которой в проекте пока
 * нет — см. ARCHITECTURE.md, раздел Tunnel). Как только AAR добавлен,
 * единственное, что нужно поправить — вызов внутри callGeneratedBinding().
 */
@Singleton
class LibXrayBridge @Inject constructor() {

    fun invoke(method: String, payload: JSONObject): LibXrayResult {
        val request = JSONObject().apply {
            put("apiVersion", 1)
            put("method", method)
            put("payload", payload)
        }
        val raw = callGeneratedBinding(request.toString())
        return parseResponse(raw)
    }

    private fun callGeneratedBinding(requestJson: String): String {
        // TODO: заменить на реальный вызов после подключения AAR, например:
        //   return LibXray.invoke(requestJson)
        throw IllegalStateException(
            "libXray AAR ещё не подключён к проекту (нужна сборка через Go/gomobile в CI, " +
                "см. ARCHITECTURE.md). JSON-протокол Invoke() уже реализован и готов к работе, " +
                "как только появится сам байндинг.",
        )
    }

    private fun parseResponse(raw: String): LibXrayResult {
        val json = JSONObject(raw)
        return if (json.optBoolean("success", false)) {
            LibXrayResult.Success(json.optJSONObject("data") ?: JSONObject())
        } else {
            LibXrayResult.Failure(json.optString("error", "Неизвестная ошибка libXray"))
        }
    }
}

sealed class LibXrayResult {
    data class Success(val data: JSONObject) : LibXrayResult()
    data class Failure(val error: String) : LibXrayResult()
}
