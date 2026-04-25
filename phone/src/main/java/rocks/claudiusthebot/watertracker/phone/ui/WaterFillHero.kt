package rocks.claudiusthebot.watertracker.phone.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rocks.claudiusthebot.watertracker.phone.ui.shapes.RoundedStarShape
import kotlin.math.PI
import kotlin.math.sin

/**
 * Hero card for the Today screen.
 *
 * Layout: a horizontal card with bold typography on the left (today's
 * total + a M3 Expressive `LinearWavyProgressIndicator`) and a fluid
 * water-fill blob on the right wrapped in a `RoundedStarShape` cookie.
 *
 * All colors come from the M3 theme (`primaryContainer` / `primary` /
 * `onPrimaryContainer`) so it adapts to dynamic-color wallpapers and
 * dark mode without the hardcoded blues the previous version used.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WaterFillHero(
    currentMl: Int,
    goalMl: Int,
    modifier: Modifier = Modifier
) {
    val rawPct = if (goalMl > 0) (currentMl.toFloat() / goalMl).coerceIn(0f, 1.5f) else 0f
    val pct by animateFloatAsState(
        targetValue = rawPct,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pct"
    )
    val displayedMl by animateIntAsState(
        targetValue = currentMl,
        animationSpec = tween(durationMillis = 480),
        label = "displayedMl"
    )

    val pctInt = (pct * 100).toInt()
    val remaining = (goalMl - currentMl).coerceAtLeast(0)
    val hit = currentMl >= goalMl && goalMl > 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Left column: copy + wavy progress ─────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (hit) "GOAL HIT" else "TODAY",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$displayedMl",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "ml",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Text(
                    text = "of $goalMl ml goal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )

                Spacer(Modifier.height(2.dp))

                LinearWavyProgressIndicator(
                    progress = { pct.coerceAtMost(1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)
                )

                Text(
                    text = if (hit) "$pctInt% · keep going!"
                        else "$pctInt% · $remaining ml to go",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }

            Spacer(Modifier.width(18.dp))

            // ── Right side: fluid-fill cookie ────────────────────────
            FluidFillCookie(
                pct = pct,
                modifier = Modifier.size(132.dp)
            )
        }
    }
}

/**
 * The fluid water disc, themed and shaped like an M3-Expressive cookie.
 * Two phase-offset sine waves rise from the bottom, with the occasional
 * bubble on top.
 */
@Composable
private fun FluidFillCookie(
    pct: Float,
    modifier: Modifier = Modifier
) {
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { while (true) withFrameMillis { tick = it } }

    val shape = remember { RoundedStarShape(sides = 8, curve = 0.10) }
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val waterTopColor = MaterialTheme.colorScheme.primary
    val waterBottomColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    val waveTrim = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val highlightColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val capped = pct.coerceIn(0f, 1f)

            val waterTop = h - h * capped
            val amplitude = 5.dp.toPx()
            val freq = 2f * PI.toFloat() / w
            val phase1 = (tick % 2400L) / 2400f * 2f * PI.toFloat()
            val phase2 = (tick % 4000L) / 4000f * 2f * PI.toFloat()

            val clip = Path().apply { addRect(Rect(Offset.Zero, Size(w, h))) }
            clipPath(clip) {
                if (capped > 0f) {
                    val frontWave = Path().apply {
                        moveTo(0f, waterTop)
                        var x = 0f
                        while (x <= w) {
                            val y = waterTop + amplitude * sin(freq * x + phase1)
                            lineTo(x, y); x += 3f
                        }
                        lineTo(w, h); lineTo(0f, h); close()
                    }
                    drawPath(
                        frontWave,
                        brush = Brush.verticalGradient(
                            colors = listOf(waterTopColor, waterBottomColor),
                            startY = waterTop - amplitude,
                            endY = h
                        )
                    )

                    // Slower secondary wave for parallax / depth.
                    val backWave = Path().apply {
                        val backTop = waterTop + amplitude * 1.1f
                        moveTo(0f, backTop)
                        var x = 0f
                        while (x <= w) {
                            val y = backTop + amplitude * 0.75f * sin(freq * 1.4f * x + phase2)
                            lineTo(x, y); x += 3f
                        }
                        lineTo(w, h); lineTo(0f, h); close()
                    }
                    drawPath(backWave, color = waveTrim)

                    // Rising bubbles
                    val bubbles = 5
                    for (i in 0 until bubbles) {
                        val seed = i * 91.7f
                        val phaseB = ((tick / 20L + (seed * 7L).toLong()) % 4000L) / 4000f
                        val bx = ((seed * 53f) % w)
                        val by = h - phaseB * h
                        if (by > waterTop) {
                            drawCircle(
                                color = highlightColor.copy(
                                    alpha = (1f - phaseB).coerceIn(0f, 0.55f)
                                ),
                                radius = (1.6f + (seed % 1.4f)).dp.toPx(),
                                center = Offset(bx, by)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("unused")
@Composable
private fun PercentBadge(pct: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Text(
            text = "$pct%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
