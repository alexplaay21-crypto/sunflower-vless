package com.sunflower.utilityproxy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SunflowerDarkColors = darkColorScheme(
    primary = SunflowerGold,
    onPrimary = DeepSpace,
    secondary = SunflowerLeaf,
    onSecondary = DeepSpace,
    background = DeepSpace,
    onBackground = TextPrimary,
    surface = SurfaceRaised,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaisedHigh,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
)

private val SunflowerLightColors = lightColorScheme(
    primary = SunflowerGoldDim, // на светлом фоне тёмный вариант золота читается лучше
    onPrimary = LightBackground,
    secondary = SunflowerLeaf,
    onSecondary = LightBackground,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurfaceRaised,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceRaisedHigh,
    onSurfaceVariant = LightTextSecondary,
    error = ErrorRed,
)

/**
 * "system" | "light" | "dark" — значение приходит из SettingsRepository.theme
 * через MainActivity (см. его doc-комментарий). Раньше тема была жёстко
 * зашита на тёмную вне зависимости от того, что хранилось в DataStore —
 * настройка существовала, но ничего реально не переключала. Dynamic Color
 * (Material You) сюда сознательно не добавлен — он бы конфликтовал с
 * сигнатурной золотой палитрой Sunflower, а промт просит именно свой
 * акцент поверх системного (пункт 39: "Также оставить Sunflower accent").
 */
@Composable
fun SunflowerTheme(theme: String = "system", content: @Composable () -> Unit) {
    val useDark = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) SunflowerDarkColors else SunflowerLightColors,
        typography = SunflowerTypography,
        shapes = SunflowerShapes,
        content = content,
    )
}
