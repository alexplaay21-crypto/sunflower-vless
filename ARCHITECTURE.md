# Архитектура — Sunflower Utility Proxy

## Модуль

Один Gradle-модуль `:app`, разделение по пакетам (`ui`, `domain`, `data`,
`tunnel`, `network`, `parsing`). Мульти-модульность даёт выигрыш на
большой команде/кэшах CI; для соло-разработки с телефона она только
усложнит и без того нетривиальный GitHub Actions. Разбить на модули
можно позже, если сборка станет медленной.

## Слои

- **UI** — Compose + Compose Navigation (добавится вместе с первым
  вторым экраном), дизайн-система Liquid Glass отдельным пакетом
  `ui/theme`. Сейчас в `ui/theme` — переходная тёмная палитра, не
  финальный Liquid Glass (см. PART 37-40 промта).
- **ViewModel** — Main/Servers/Subscriptions/Settings, состояние через
  `StateFlow<UiState>`. `MainViewModel` уже есть и работает end-to-end.
- **Domain** — тонкие use-case'ы (ImportSubscription, UpdateSubscription,
  TestServer, ConnectTunnel), чтобы бизнес-логика не жила в ViewModel и
  была тестируемой. Появятся вместе с первым реальным источником данных.
- **Data** — Repository поверх:
  - **Room** — `Subscription (1) → (N) Server`: это реляционные данные
    (фильтры, избранное, атомарная замена серверов при обновлении
    подписки). Версия 2.8.0 подтверждена, зависимость пока не подключена.
  - **DataStore (Preferences)** — скаляры: тема, killSwitch, allowLan,
    автоподключение и т.п.
  - **Android Keystore** — секреты, в первую очередь под `age.secretKey`,
    если эта фича вообще будет реализуема (см. «Неподтверждено» ниже).
- **Network** — OkHttp, `SubscriptionRemoteDataSource` (GET, опциональный
  `Cookie: hwid=`, `allowInsecure` только на конкретный запрос).
- **Tunnel** — `SunflowerVpnService : VpnService` + интерфейс
  `TunnelEngine` + `XrayTunnelEngine` (реализация поверх libXray) +
  `TunnelManager` как единственный источник истины о состоянии
  (`StateFlow<TunnelState>`). **Обязан быть строгим синглтоном** — см.
  находку про libXray ниже.
- **Background** — WorkManager (2.10.4) для автообновления подписок,
  `BootCompletedReceiver` под флагом настройки.
- **Widget** — Glance (Compose-based, не классический RemoteViews).
- **DI** — Hilt 2.59.2 + KSP 2.3.11 (не kapt — с AGP 9 kapt несовместим
  со встроенным Kotlin, см. ниже).

## Ключевая находка: AGP 9 меняет сборку Kotlin-проектов

Начиная с AGP 9.0 поддержка Kotlin **встроена в AGP** и включена по
умолчанию — плагин `org.jetbrains.kotlin.android` для Android-приложения
больше не применяется вообще. Это не мелочь: любой туториал или ответ
другой модели, обученной до этого изменения, покажет старую (уже
несовместимую) схему `plugins { id("org.jetbrains.kotlin.android") }`.
Источник: https://developer.android.com/build/migrate-to-built-in-kotlin

Что из этого следует для наших файлов:
- В `app/build.gradle.kts` **нет** строки про `kotlin-android` — Kotlin
  компилируется сам по себе через AGP.
- Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`) — это
  **отдельный** плагин, built-in Kotlin его не заменяет, поэтому он
  по-прежнему явно подключён.
- Для аннотаций (Hilt) используется KSP, а не kapt: kapt несовместим со
  встроенным Kotlin, официальная рекомендация — миграция на KSP.
- Явный блок `kotlin { compilerOptions { ... } }` не нужен, пока не
  требуется что-то нестандартное — jvmTarget по умолчанию берётся из
  `android.compileOptions.targetCompatibility`.

## TUN: подтверждено по исходнику tun-инбаунда (не по пересказу через libXray)

Предыдущая версия этого файла ошибочно предполагала, что fd пишется в
JSON-конфиг как `env.xray.tun.fd`. Проверка именно
`github.com/XTLS/Xray-core/blob/main/proxy/tun/README.md` и
`xtls.github.io/en/config/inbounds/tun.html` (официальные источники,
не пересказ) показала другое:

1. **`inbounds` обязан содержать объект с `"protocol": "tun"`.** Это
   настоящий, задокументированный inbound Xray-core, не выдумка.
2. **Сам TUN fd передаётся НЕ через JSON**, а как настоящая переменная
   окружения процесса — `XRAY_TUN_FD` (или её алиас `xray.tun.fd`, оба
   варианта равнозначны). Источник — дословно: *"Set the environment
   variable xray.tun.fd (or XRAY_TUN_FD) to the fd number before
   starting Xray. This can be done from Kotlin/Java..."*. На Android это
   делается через `android.system.Os.setenv(...)` — стандартный,
   стабильный с API 21 Android API, тонкая обёртка над POSIX `setenv(3)`;
   т.к. Go-библиотека грузится в тот же процесс приложения, окружение ей
   видно. Реализовано в `XrayTunnelEngine.start()`, прямо перед вызовом
   `runXrayFromJson`.
3. **Routing:** tun-inbound — низкоуровневый пакетный ридер, пакеты идут
   через встроенный userspace network stack Xray-core и дальше через
   обычную систему routing, как с любым другим инбаундом
   (core-tutorial.argsment.com/xray/tun: *"packets are decoded by a
   minimal in-process network stack and dispatched to outbounds"*). При
   одном outbound и без явного блока `"routing"` весь трафик по умолчанию
   идёт на единственный outbound — доп. правила не нужны для MVP с одним
   сервером.
4. `gateway`/`dns`/`autoSystemRoutingTable` в settings tun-инбаунда — это
   для Linux/Windows/macOS, где Xray сам поднимает интерфейс. На Android
   адресацию и маршруты уже делает `VpnService.Builder`
   (`SunflowerVpnService.establishTunnel`), поэтому в JSON их нет.

### Итоговый рабочий конфиг (пример для VLESS+Reality)

Ровно то, что генерирует `XrayConfigBuilder.build()`. Все четыре
протокола (vless/vmess/trojan/shadowsocks) используют плоскую форму
`settings` — проверено по официальной странице каждого протокола
отдельно (см. doc-комментарий в файле), а не по аналогии друг с другом:

```json
{
  "log": { "loglevel": "warning" },
  "inbounds": [
    {
      "tag": "tun-in",
      "port": 0,
      "protocol": "tun",
      "settings": { "name": "sunflower-tun", "mtu": 1500 }
    }
  ],
  "outbounds": [
    {
      "protocol": "vless",
      "settings": {
        "address": "example.com",
        "port": 443,
        "id": "UUID",
        "encryption": "none",
        "flow": "xtls-rprx-vision"
      },
      "streamSettings": {
        "network": "tcp",
        "security": "reality",
        "realitySettings": {
          "show": false,
          "fingerprint": "chrome",
          "serverName": "example.com",
          "publicKey": "PUBLIC_KEY",
          "shortId": "SHORT_ID"
        }
      }
    }
  ]
}
```

А переменная окружения `XRAY_TUN_FD` ставится отдельно, кодом, до
вызова `runXrayFromJson` — она не часть этого JSON.

### Что из этого всё ещё не проверено

- Сам `.aar` не собран (см. build-libxray-aar.yml) — без него всё
  вышеописанное нельзя проверить живым запуском.
- `android.system.Os.setenv` — стандартный Android API, но именно эта
  связка (Os.setenv → видимость в загруженной через JNI Go-библиотеке)
  не проверена живым тестом, только логически обоснована.
- Trojan/VMess outbound (см. выше) — теперь тоже проверены по
  официальным страницам (не по устоявшемуся паттерну, как раньше).

## Fragment / Noise: подтверждено, но НЕ подключено — не там, где ожидалось

Проверил `xtls.github.io/en/config/outbounds/freedom.html` и исходник
(`infra/conf`, `FreedomConfig` в pkg.go.dev) — `fragment`/`noises`
оказались полями **отдельного `"freedom"`-outbound**, а не свойством
самого VLESS/Trojan/VMess outbound:

```json
{
  "protocol": "freedom",
  "settings": {
    "fragment": { "packets": "tlshello", "length": "100-200", "interval": "10-20" },
    "noises": [ { "type": "base64", "packet": "...", "delay": "10-16" } ]
  }
}
```

Схема самого поля подтверждена официально. Но чтобы это реально
фрагментировало трафик именно VLESS/Trojan-соединения, нужен второй,
непроверенный шаг: связать этот `freedom`-outbound с основным через
`streamSettings.sockopt.dialerProxy`, чтобы TLS-хендшейк уходил через
фрагментирующий outbound. Эту цепочку в данной сессии не проверял —
поэтому переключателя "Использовать фрагментирование" в Settings
сознательно нет: показать его, не проверив dialerProxy-связку, означало
бы ровно то самое "красивый переключатель, который не работает", от
которого прямо предостерегает промт (пункт 85).

## Что подтвердилось по libXray

Первая проверка (по README и списку Go-функций) привела к неверному
предположению, что каждый метод — отдельная функция вроде
`LibXray.runXray(config)`. Полная документация пакета
(pkg.go.dev/github.com/xtls/libxray, invoke.go / invoke_model.go)
показала другую картину, и `LibXrayBridge`/`XrayTunnelEngine` уже
переписаны под реальный протокол:

- Единственная точка входа — `Invoke(requestJSON string) string`,
  завёрнутая в JSON-конверт:
  `{"apiVersion":1,"method":"runXray","payload":{...}}` →
  `{"success":true,"data":{},"error":""}`.
- Реальные `method` и форма `payload` для каждого — из `invoke_model.go`:
  `runXrayFromJson` → `{"configJSON":"..."}`, `stopXray` → без payload,
  `getXrayState` → `{"running":bool}`, `xrayVersion` → `{"version":"..."}`,
  `ping`/`pingBatch`/`convertShareLinksToXrayJson`/`getFreePorts` —
  аналогично по своим Request-типам.
- **SetTunFd убран из API.** TUN-дескриptor теперь передаётся не отдельным
  вызовом, а полем `xray.tun.fd` в корневом объекте `env` самого Xray
  JSON-конфига — это должен будет сделать `SunflowerVpnService`, прежде
  чем передавать конфиг в `XrayTunnelEngine.start()`.
- Последний релиз — v26.3.27 (Go-модуль зафиксирован на Xray-core
  v26.7.28 через pseudo-version), под Android собирается в AAR через
  `python3 build/main.py android`, минимальный API — 21.
- **Важно для TunnelManager:** Xray-core держит DNS-клиент и
  outbound-менеджер в общем для процесса состоянии. Запуск ещё одного
  инстанса через `ping`/`pingBatch`/`testXray`, пока `runXray`/
  `runXrayFromJson` уже активен, может испортить это состояние — libXray
  не изолирует параллельные инстансы. `TunnelManager` поэтому сделан
  строгим синглтоном с `Mutex` вокруг connect/disconnect, а
  `canRunBulkPingTest()` явно блокирует «Тест всех» при активном туннеле.
- Для Android есть встроенная помощь: `SetDNS`/`ResetDNS` (не зациклить
  DNS-трафик обратно в tun) и `ProcessFinder` — интерфейс для per-app
  routing (пункт 27 промта), который приложение реализует и регистрирует
  через `LibXray.registerProcessFinder(finder, Build.VERSION.SDK_INT)`.
- **Находка, упрощающая парсеры:** есть готовая
  `convertShareLinksToXrayJson`. Собственные парсеры (Vless/Vmess/
  Trojan/Shadowsocks) всё равно нужны — они кладут в БД структурированные
  поля (host/port/uuid и т.д.) для UI и редактирования, а не просто
  Xray JSON — но при генерации конфига для реального подключения стоит
  свериться, не проще ли часть случаев отдать libXray.

### Что ещё не сделано в Tunnel-слое

`LibXrayBridge`, `TunnelEngine`, `XrayTunnelEngine`, `TunnelManager` уже
написаны и реализуют протокол выше корректно, но `LibXrayBridge` намеренно
бросает понятное исключение вместо реального вызова — потому что
**сам AAR ещё не собран и не подключён к проекту**. Сборка требует
отдельного шага в CI: Go + gomobile + Android NDK, клонирование
`xtls/libxray`, `python3 build/main.py android`, публикация полученного
`.aar` туда, откуда его увидит `app/build.gradle.kts`. Это отдельная,
нетривиальная задача (кросс-компиляция Go, версии NDK), которую
осознанно не стали делать наспех в этом же проходе — SunflowerVpnService
тоже отложен до готовности AAR, потому что писать VpnService поверх
недоступного байндинга бессмысленно.

## Неподтверждено — не реализуется вслепую

- **age.secretKey** — в экспортируемом API libXray функции расшифровки
  age нет. Скорее всего, потребуется отдельная Kotlin/JVM-библиотека age
  поверх Keystore, а не возможность самого libXray. Не найдётся такой
  библиотеки — фича не реализуется, вместо неё останется явное сообщение
  пользователю, как и просил промт.
- Точная JSON-схема fragment/mux/noise под закреплённую версию Xray-core
  — сверяется с официальной config-документацией отдельно, не по памяти.
- Реальное покрытие протоколов у `convertShareLinksToXrayJson`.
- Wear OS / Android TV — вне базовой архитектуры, отдельные опциональные
  модули, если до них дойдёт очередь.
- Core Dagger Hilt зафиксирован на 2.59.2 (подтверждено через GitHub
  Releases, помечен там как Latest); KSP 2.3.11 подтверждён через Maven
  Central. Остальные версии в `libs.versions.toml` (core-ktx, lifecycle,
  activity-compose) взяты из актуальной ленты релизов AndroidX, но не
  перепроверялись так же тщательно — если сборка ругнётся на
  несовместимость, это первое место для проверки.

## Статус по слоям (актуально на этот zip)

| Слой | Статус |
|---|---|
| Gradle/CI (AGP9, Hilt, Compose) | Готово, должно собираться |
| Data (Room: Subscription/Server) | Готово |
| Settings (DataStore) | killSwitch/allowLan/autoConnect/theme/deviceId/autostartOnBoot/updateInterval — готово |
| Settings UI | Настройки VPN + Автозапуск + интервал автообновления — готово; Настройки туннеля/Маршрутизация/Прокси по приложениям/Пинг/Inbounds — нет (нужна ещё не проверенная Xray-схема или экран выбора приложений) |
| Logs / Reset / About | Готово: in-memory логи с редактированием чувствительных данных, сброс с подтверждением по каждому действию, статичный About |
| Автозапуск на загрузке (BootCompletedReceiver) | Готово частично: читает флаг из настроек и логирует, но реально не переподключается — нет понятия "сервер по умолчанию" |
| Parsing (Vless/Vmess/Trojan/SS) | Готово для основных/актуальных форматов каждого протокола |
| Subscriptions (импорт по URL, список, обновление) | Готово; QR и JSON-вкладки — нет |
| Auto-update (WorkManager) | Готово целиком: Worker + реальное планирование (enqueueUniquePeriodicWork), ставится/снимается при смене интервала в Settings |
| Избранное на серверах | Готово (звезда + сортировка по ней в списке) |
| Deep links (vless/vmess/trojan/ss) | Готово: subscription-URL уходит в Add Subscription с предзаполненным полем, серверные ссылки — прямой импорт как Local |
| Foreground-уведомление VPN-сервиса | Готово (NotificationChannel IMPORTANCE_LOW); foregroundServiceType для Android 14+ не проверен — см. ниже |
| Tunnel-протокол (LibXrayBridge/TunnelEngine/TunnelManager) | Код корректен по документации, но не может реально подключиться — ждёт AAR |
| XrayConfigBuilder (outbound из ServerEntity) | Готово: все 4 протокола (vless/vmess/trojan/shadowsocks) сверены с официальными страницами по отдельности; VMess без alterId — актуальная документация его не содержит |
| Fragment/Noise | Схема freedom-outbound подтверждена, НЕ подключена — нужна ещё непроверенная dialerProxy-связка outbound'ов |
| libXray AAR | Есть экспериментальный CI-джоб (build-libxray-aar.yml), не подтверждено, что пройдёт с первого раза |
| SunflowerVpnService | Готово: Builder, TUN fd через переменную окружения, foreground-уведомление, вызывается из реального UI |
| Экран «Серверы» + VpnService.prepare() | Готово — стартовый экран, кнопка «Подключиться» реально работает до границы LibXrayBridge |
| Widget / Wear OS / Android TV | Не начато |
| QR-сканер / вкладка JSON | Не начато |
| age-шифрование подписок | Не реализуемо через libXray (подтверждено) — нужна отдельная библиотека |
| Юнит-тесты | Не начато |
| Release-сборка / подпись | Готово: signingConfig из env-переменных, ProGuard/R8 включён, 2 workflow (генерация keystore + release build) — не проверены живым запуском |
| Дизайн: нижний таб-бар, GlassCard, иконки | Готово, применено на ВСЕХ экранах (Settings/Logs/Reset/About догнаны в v0.10) |
| Светлая/тёмная/системная тема | Готово по-настоящему: пикер в Settings + реальное переключение в MainActivity (раньше настройка существовала, но не использовалась нигде) |
| Юнит-тесты (парсеры + decoder) | Готово: 19 тестов, JUnit4+Robolectric, base64-фикстуры проверены через Python перед вставкой |
| Локализация UI (values/values-ru strings.xml) | Готово для всего пользовательского текста экранов (61 строка); сообщения об ошибках бизнес-логики — нет, отдельная архитектурная задача |

## Защита release APK (R8/ProGuard)

### Изменённые файлы
- `app/build.gradle.kts` — `isDebuggable = false` явно (был дефолт AGP,
  теперь прописан), добавлен блок `packaging.resources.excludes` под
  типовые META-INF-конфликты.
- `app/proguard-rules.pro` — переписан: keep для Worker-классов
  (WorkManager резолвит по имени класса в рантайме), `-assumenosideeffects`
  для `Log.v`/`Log.d` (в коде их и так не было — правило на будущее),
  подробно расписано, что уже покрывается автоматически (Hilt/Room —
  KSP-кодген, не рефлексия; org.json — прямые вызовы, не reflection-
  сериализация) и что специально НЕ добавлено (JNI-keep под libXray —
  до того, как появится реальное имя класса из AAR).

### Что уже было готово до этого прохода
`isMinifyEnabled`/`isShrinkResources` включены с версии v0.8. Мэппинг-файл
(`mapping.txt`) в APK и так никогда не попадает — это отдельный output
рядом с APK, а не часть архива; исходники `.kt` в APK попасть не могут
в принципе — APK содержит скомпилированный DEX, не source (это не
настраивается, так устроена сборка).

### Реальная проверка на секреты (не по описанию — прогнал grep по всему
дереву исходников прямо в песочнице)
Хардкод password/secret/token/key, тестовые/staging/localhost URL,
случайно закоммиченные `.jks`/`.keystore`/`.pem`/`google-services.json` —
0 совпадений по всем пяти проверкам. Единственный "похожий" на секрет
литерал — `10.10.10.1`, это IP-адрес TUN-интерфейса, не URL и не пароль.

### Что НЕ проверено (требует живой сборки — как и всё остальное в этом
проекте, ни я, ни ты без CI это не подтвердим)
1. `assembleRelease` реально проходит.
2. APK ставится на устройство.
3. VPN/TUN поднимается (структурно да — establish() не зависит от R8;
   реального туннелирования всё равно не будет без AAR, см. выше).
4. Xray/JNI работает — не проверить, пока AAR не подключён физически.
5. Подключение/маршрутизация — то же самое.
6. R8 реально обфусцировал код — нужен mapping.txt/декомпиляция готового
   APK, которых у меня нет.
7. Отсутствие секретов в готовом APK — по исходникам проверил (см. выше);
   по итоговому бинарнику — нет доступа его собрать.

Размер APK до/после тоже не могу — у меня нет собранного APK ни в одной,
ни в другой версии, только исходники.

## Другие открытые вопросы, требующие живой проверки (не только AAR)

- `android:foregroundServiceType` для VPN-сервиса на Android 14+ (API 34):
  не нашёл прямого подтверждения, освобождён ли VpnService от общего
  требования объявлять тип foreground-сервиса, или нужен
  `specialUse`/другой конкретный тип. Сейчас атрибут не указан — если
  сборка/запуск на API 34+ упадёт именно на этом, здесь первое место
  для проверки.

## Почему не все 12 разделов Settings

Раздел «Настройки туннеля» (fragment/mux/noise/inbounds) требует
генерировать реальный Xray JSON-конфиг с полями, чью точную схему для
закреплённой версии Xray-core (v26.7.28) я не сверял с официальной
config-документацией построчно — показывать переключатели, которые не
подключены ни к чему реальному, прямо запрещено пунктом 85 промта
("лучше меньше функций, но чтобы они реально работали"). Как только
конфиг-генератор будет сверен и написан, этот раздел добавится тем же
образом, что и «Настройки VPN» сейчас — по одному реально работающему
полю за раз, не по 15 сразу.

## Порядок работы дальше

1. Прогнать `build-libxray-aar.yml` живым запуском; поправить
   NDK_VERSION/env, если упадёт.
2. Подключить полученный `.aar` в `app/build.gradle.kts` и
   раскомментировать реальный вызов в `LibXrayBridge`.
3. Живым тестом проверить связку `Os.setenv("XRAY_TUN_FD", ...)` →
   видимость в загруженной через JNI Go-библиотеке — единственное, что
   осталось логическим выводом, а не подтверждённым фактом, в цепочке
   TUN → routing → outbound.
4. Сверить реальную Xray-core config-схему для fragment/mux/routing и
   только тогда добавлять оставшиеся разделы Settings.
