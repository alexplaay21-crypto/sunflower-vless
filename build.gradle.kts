// Плагины объявлены здесь с apply false и применяются в app/build.gradle.kts.
// Начиная с AGP 9.0 отдельный плагин org.jetbrains.kotlin.android НЕ применяется —
// поддержка Kotlin встроена в AGP по умолчанию (built-in Kotlin).
// Источник: https://developer.android.com/build/migrate-to-built-in-kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
