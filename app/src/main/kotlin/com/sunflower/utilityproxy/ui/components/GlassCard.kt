package com.sunflower.utilityproxy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sunflower.utilityproxy.ui.theme.GlassBorder
import com.sunflower.utilityproxy.ui.theme.ShapeLarge

/**
 * "Glass"-поверхность из брифа (пункт 37/70 промта): полупрозрачный
 * градиент + тонкая обводка + мягкая тень — сознательно БЕЗ настоящего
 * backdrop-блюра. Modifier.blur() в Compose блюрит сам composable, а не
 * то, что ЗА ним (нужно для настоящего glassmorphism); реальный
 * backdrop-blur в Compose требует либо API 31+ RenderEffect-трюков,
 * либо стороннюю библиотеку (напр. Haze) — ещё одна непроверенная
 * версия-зависимость. Промт сам называет этот вариант допустимой
 * заменой, если полный blur слишком дорог по производительности:
 * "используй оптимизированную glass-имитацию: semi-transparent surface +
 * border + shadow + gradient". Более честно и предсказуемо на реальных
 * устройствах, чем гнаться за точной имитацией iOS-блюра.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = ShapeLarge,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = shape, ambientColor = GlassBorder.copy(alpha = 0.08f))
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                    ),
                ),
            )
            .border(1.dp, GlassBorder.copy(alpha = 0.08f), shape),
        content = content,
    )
}
