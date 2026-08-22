plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.sunflower.utilityproxy"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sunflower.utilityproxy"
        // minSdk 26 (Android 8.0): упрощает адаптивные иконки (не нужен fallback
        // для до-26 плотностей) и покрывает подавляющее большинство активных
        // устройств на 2026 год. libXray собирается вплоть до API 21, так что
        // при необходимости порог можно снизить — тогда потребуются дополнительные
        // ресурсы иконки под mipmap-*dpi.
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.8.0"
    }

    signingConfigs {
        create("release") {
            // Читается из переменных окружения (CI secrets), никогда не хардкодится
            // в репозитории — см. .github/workflows/release.yml и README.md,
            // раздел "Release-сборка". Если переменные не заданы (локальный debug-
            // прогон build.yml), signingConfig на release просто не назначается
            // ниже — сборка соберётся debug-подписанной, без падения.
            val keystorePath = System.getenv("SUNFLOWER_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("SUNFLOWER_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SUNFLOWER_KEY_ALIAS")
                keyPassword = System.getenv("SUNFLOWER_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false // явно, не полагаемся на дефолт AGP
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (!System.getenv("SUNFLOWER_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
            // Без секретов signingConfig не назначен вовсе — Gradle соберёт
            // release build с debug-подписью по умолчанию, а не упадёт. Это
            // ожидаемо для локальной проверки; для установки на реальное
            // устройство или публикации нужен подписанный вариант из
            // release.yml с настоящими secrets.
        }
    }

    // BuildConfig не включён (buildFeatures.buildConfig не выставлен в true) —
    // BuildConfig.DEBUG в проекте нигде не использовался, поэтому убирать
    // было нечего; поле не генерируется вовсе, а не "вырезается" R8.

    packaging {
        // Стандартный набор конфликтов META-INF/* при большом числе
        // зависимостей (OkHttp, Kotlin stdlib, coroutines, AndroidX) —
        // без этого сборка может упасть на "More than one file was found
        // with OS independent path". Список — общепринятый, не подобран
        // под конкретную ошибку (её ещё не было, реальной сборки не было).
        resources {
            excludes += setOf(
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/DEPENDENCIES",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = false
        }
    }

    // Room-схема пока не экспортируется (version = 1, миграций ещё нет) —
    // room.schemaLocation добавим вместе с первой реальной миграцией.
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
