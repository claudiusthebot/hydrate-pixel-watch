package rocks.claudiusthebot.watertracker.phone.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Pill-shaped tab cell with three motion accents:
 *  - selected pulse (1.0 → 1.05 → 1.0)
 *  - direct neighbours nudge sideways toward the new selection
 *  - background/content colors crossfade
 *
 * Adapted from theovilardo/PixelPlayer (MIT) — TabAnimation.
 */
@Composable
fun AnimatedTabPill(
    modifier: Modifier = Modifier,
    index: Int,
    selectedIndex: Int,
    onClick: () -> Unit,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    onSelectedColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    onUnselectedColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
    transformOrigin: TransformOrigin = TransformOrigin.Center,
    content: @Composable () -> Unit
) {
    val isSelected = index == selectedIndex
    val scale = remember { Animatable(1f) }
    val offsetX = remember { Animatable(0f) }
    var initialised by remember { mutableStateOf(false) }
    val tap = rememberLightTick()

    val animSpec = tween<Float>(durationMillis = 250, easing = FastOutSlowInEasing)
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else unselectedColor,
        animationSpec = tween(200),
        label = "tabBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) onSelectedColor else onUnselectedColor,
        animationSpec = tween(200),
        label = "tabFg"
    )

    LaunchedEffect(selectedIndex) {
        if (!initialised) {
            initialised = true
            scale.snapTo(1f); offsetX.snapTo(0f)
            return@LaunchedEffect
        }
        if (isSelected) {
            launch {
                scale.animateTo(1.05f, animSpec)
                scale.animateTo(1f, animSpec)
            }
            offsetX.snapTo(0f)
        } else {
            scale.snapTo(1f)
            val distance = index - selectedIndex
            if (abs(distance) == 1) {
                val direction = if (distance > 0) 1f else -1f
                launch {
                    offsetX.animateTo(12f * direction, animSpec)
                    offsetX.animateTo(0f, animSpec)
                }
            } else offsetX.snapTo(0f)
        }
    }

    Tab(
        modifier = modifier
            .padding(5.dp)
            .graphicsLayer {
                scaleX = scale.value
                translationX = offsetX.value
                this.transformOrigin = transformOrigin
            }
            .clip(CircleShape)
            .background(color = backgroundColor, shape = RoundedCornerShape(50)),
        selected = isSelected,
        text = content,
        onClick = { tap(); onClick() },
        selectedContentColor = contentColor,
        unselectedContentColor = contentColor
    )
}
