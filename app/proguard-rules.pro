# Правила ProGuard/R8 для Sunflower Utility Proxy (release build).
#
# == Что покрывается автоматически, без ручных правил ниже ==
#
# 1. Activity/Service/BroadcastReceiver/Application, объявленные в
#    AndroidManifest.xml (MainActivity, SunflowerVpnService,
#    BootCompletedReceiver, SunflowerApp) — их держит проживающий keep
#    из getDefaultProguardFile("proguard-android-optimize.txt") (уже
#    подключён в app/build.gradle.kts) плюс manifest-aware keep-правила,
#    которые генерирует сам AGP по содержимому манифеста. Явных -keep
#    под них здесь нет — они были бы дублирующими.
# 2. Hilt/Dagger — весь граф DI собирается через KSP-кодген на этапе
#    компиляции, не через рантайм-рефлексию; консьюмер-правила лежат
#    внутри hilt-android AAR.
# 3. Room — DAO/Database реализации тоже генерируются KSP-кодгеном,
#    не рефлексией; консьюмер-правила — внутри androidx.room AAR.
# 4. OkHttp, WorkManager (кроме пункта про Worker-классы ниже),
#    Navigation-Compose, DataStore — несут собственные консьюмер-правила.
# 5. org.json.JSONObject/JSONArray (парсинг подписок, XrayConfigBuilder,
#    LibXrayBridge) — обычные вызовы put/get по строковым ключам, НЕ
#    reflection-based сериализация (не Gson/Moshi/kotlinx.serialization).
#    R8 тут ломать нечего — нет полей, которые сериализатор находил бы
#    по имени через рефлексию.

# == Что реально нужно держать вручную ==

# WorkManager резолвит Worker-класс (SubscriptionUpdateWorker) по строке
# с именем класса при выполнении задачи — даже с HiltWorkerFactory сам
# класс должен пережить переименование, иначе резолвинг по имени в
# рантайме упадёт. androidx.hilt:hilt-work, скорее всего, уже покрывает
# это своими консьюмер-правилами, но держим явно и defensively —
# дублирование здесь не вредит.
-keep class * extends androidx.work.ListenableWorker

# libXray / JNI-мост (см. tunnel/LibXrayBridge.kt): когда появится
# реальный .aar (см. build-libxray-aar.yml), сгенерированный gomobile-
# класс почти наверняка обращается к нему через JNI-регистрацию имён
# методов — R8 переименует/удалит их при обфускации, если не keep-нуть.
# Раскомментировать и поправить точное имя класса ПОСЛЕ подключения
# настоящего AAR, когда оно станет известно, а не сейчас вслепую:
# -keep class libxray.** { *; }

# == Логи ==
#
# android.util.Log.e/w/i сознательно ОСТАВЛЕНЫ работать в release —
# это полезная диагностика (ошибки подключения, TUN, autostart), а не
# debug-шум, и в текущем коде Log.d/Log.v не используется вовсе (нечего
# было вырезать). Ниже — правило на будущее: если кто-то добавит
# Log.d/Log.v, R8 вырежет сами вызовы как no-op в release, не удаляя сам
# класс android.util.Log.
#
# ВАЖНО: это НЕ относится к data.AppLogger — это отдельный in-memory
# буфер для экрана "Логи" в самом приложении (пункт 33 промта), реальная
# фича продукта, а не debug-код; его вызовы этим правилом не затрагиваются.
-assumenosideeffects class android.util.Log {
    public static int v(java.lang.String, java.lang.String);
    public static int v(java.lang.String, java.lang.String, java.lang.Throwable);
    public static int d(java.lang.String, java.lang.String);
    public static int d(java.lang.String, java.lang.String, java.lang.Throwable);
}
