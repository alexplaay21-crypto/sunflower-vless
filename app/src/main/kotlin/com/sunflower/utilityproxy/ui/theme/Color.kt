package com.sunflower.utilityproxy.ui.theme

import androidx.compose.ui.graphics.Color

// Базовая палитра v0.1 — переходная, до полноценной Liquid Glass системы
// (PART 37-40: blur, layered surfaces, Material You). Сознательный выбор:
// тёплое золото подсолнуха на глубоком синевато-чёрном фоне — НЕ кремовый
// с терракотой и НЕ чёрный с кислотным зелёным (частые дефолтные AI-палитры).

val SunflowerGold = Color(0xFFF5B93F)
val SunflowerGoldDim = Color(0xFFC98F1F)
val SunflowerLeaf = Color(0xFF4C9A7C)

val DeepSpace = Color(0xFF0E0F13)
val SurfaceRaised = Color(0xFF181A22)
val SurfaceRaisedHigh = Color(0xFF20232D)
val GlassBorder = Color(0xFFFFFFFF)

val TextPrimary = Color(0xFFEDEEF2)
val TextSecondary = Color(0xFFA0A4B2)

val ErrorRed = Color(0xFFE5484D)

// Светлая тема — та же сигнатурная пара (золото подсолнуха + акцент-лист),
// но на светлом фоне вместо DeepSpace. Не белый в ноль — тёплый почти-белый,
// чтобы карточки (GlassCard) на нём тоже читались как "стекло", а не
// сливались в один плоский белый прямоугольник.
val LightBackground = Color(0xFFFAF8F4)
val LightSurfaceRaised = Color(0xFFFFFFFF)
val LightSurfaceRaisedHigh = Color(0xFFF0EDE6)
val LightTextPrimary = Color(0xFF1C1B18)
val LightTextSecondary = Color(0xFF5C5850)
