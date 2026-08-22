package com.sunflower.utilityproxy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Из брифа: 20-28dp скругления, "большие скругления" как сигнатурная
// черта Liquid Glass-направления. Единая шкала вместо разных чисел
// в разных экранах — так вся навигация/карточки визуально одного языка.
val ShapeExtraSmall = RoundedCornerShape(12.dp)
val ShapeSmall = RoundedCornerShape(16.dp)
val ShapeMedium = RoundedCornerShape(20.dp)
val ShapeLarge = RoundedCornerShape(28.dp)
val ShapePill = RoundedCornerShape(50)

val SunflowerShapes = Shapes(
    extraSmall = ShapeExtraSmall,
    small = ShapeSmall,
    medium = ShapeMedium,
    large = ShapeLarge,
    extraLarge = ShapeLarge,
)
