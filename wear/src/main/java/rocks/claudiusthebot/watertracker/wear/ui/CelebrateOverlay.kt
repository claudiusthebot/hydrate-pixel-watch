package rocks.claudiusthebot.watertracker.wear.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Goal-reached celebration: particle droplets radiating from the center,
 * one-shot ~3 seconds. Parent toggles `visible` when today first crosses
 * goal and sets it back after the animation completes.
 */
@Composable
fun CelebrateOverlay(visible: Boolean, onDone: () -> Unit) {
    var startMs by remember { mutableStateOf(0L) }
    var tick by remember { mutableStateOf(0L) }

    LaunchedEffect(visible) {
        if (visible) {
            startMs = System.currentTimeMillis()
            // Auto-fade after 2.8s
            delay(2800)
            onDone()
        }
    }

    LaunchedEffect(visible) {
        while (visible) {
            withFrameMillis { tick = it }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = size.minDimension / 2f
                val elapsed = System.currentTimeMillis() - startMs
                val progress = (elapsed.coerceIn(0L, 2800L) / 2800f).coerceIn(0f, 1f)

                // 24 droplets radiating out, each with its own phase
                repeat(24) { i ->
                    val seed = Random(i).nextFloat()
                    val angle = (i / 24f) * 2f * PI.toFloat() + seed * 0.4f
                    val phase = (progress + seed * 0.3f).coerceIn(0f, 1.5f)
                    val dist = maxR * (0.3f + phase * 0.85f)
                    val x = cx + dist * cos(angle)
                    val y = cy + dist * sin(angle)
                    val alpha = (1f - progress).coerceIn(0f, 1f)
                    val r = (2.5f + seed * 2f) * (1f - progress * 0.4f)
                    drawCircle(
                        color = Color(0xFF81D4FA).copy(alpha = alpha * 0.9f),
                        radius = r.coerceAtLeast(0.8f).dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
