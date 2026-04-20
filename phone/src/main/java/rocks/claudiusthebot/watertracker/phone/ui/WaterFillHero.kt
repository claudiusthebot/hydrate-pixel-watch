package rocks.claudiusthebot.watertracker.phone.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Phone-side hero card. Rendered with:
 *   • a dark gradient background,
 *   • a progress ring,
 *   • an animated wavy water fill inside the ring.
 * The whole thing animates smoothly via [animateFloatAsState] when the
 * daily total changes.
 */
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

    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) withFrameMillis { tick = it }
    }

    Surface(
        modifier = modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(36.dp)),
        color = Color.Transparent
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            // Soft gradient background
            Canvas(Modifier.fillMaxWidth().height(300.dp)) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to Color(0xFF0B2034),
                        0.55f to Color(0xFF0F3954),
                        1f to Color(0xFF104D72)
                    ),
                    cornerRadius = CornerRadius(36.dp.toPx())
                )
            }

            // Progress ring + water-fill disc
            Box(
                Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val radius = (minOf(w, h) / 2f) - 8.dp.toPx()
                    val center = Offset(w / 2f, h / 2f)
                    val stroke = 12.dp.toPx()

                    // Track
                    drawArc(
                        color = Color.White.copy(alpha = 0.12f),
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    // Progress
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFF81D4FA),
                                Color(0xFF29B6F6),
                                Color(0xFF0288D1),
                                Color(0xFF81D4FA)
                            ),
                            center = center
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * pct.coerceAtMost(1f),
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    // Inner water-fill disc
                    val inner = radius - stroke - 4.dp.toPx()
                    val discClip = Path().apply {
                        addOval(Rect(center - Offset(inner, inner), Size(inner * 2, inner * 2)))
                    }
                    clipPath(discClip) {
                        drawCircle(
                            color = Color(0xFF072030),
                            radius = inner,
                            center = center
                        )

                        val waterTop = center.y + inner - inner * 2 * pct.coerceAtMost(1f)
                        val amplitude = 6.dp.toPx()
                        val freq = 2f * PI.toFloat() / (inner * 2f)
                        val phase = (tick % 2400L) / 2400f * 2f * PI.toFloat()

                        val water = Path().apply {
                            moveTo(center.x - inner, waterTop)
                            var x = center.x - inner
                            while (x <= center.x + inner) {
                                val y = waterTop + amplitude * sin(freq * x + phase)
                                lineTo(x, y)
                                x += 3f
                            }
                            lineTo(center.x + inner, center.y + inner)
                            lineTo(center.x - inner, center.y + inner)
                            close()
                        }
                        drawPath(
                            water,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF4FC3F7),
                                    Color(0xFF0288D1),
                                    Color(0xFF01579B)
                                ),
                                startY = waterTop - amplitude,
                                endY = center.y + inner
                            )
                        )

                        // second, slower wave for parallax
                        val waterTop2 = waterTop + amplitude * 1.2f
                        val phase2 = (tick % 4000L) / 4000f * 2f * PI.toFloat()
                        val water2 = Path().apply {
                            moveTo(center.x - inner, waterTop2)
                            var x = center.x - inner
                            while (x <= center.x + inner) {
                                val y = waterTop2 + amplitude * 0.8f * sin(freq * 1.3f * x + phase2)
                                lineTo(x, y)
                                x += 3f
                            }
                            lineTo(center.x + inner, center.y + inner)
                            lineTo(center.x - inner, center.y + inner)
                            close()
                        }
                        drawPath(
                            water2,
                            color = Color(0xFF29B6F6).copy(alpha = 0.45f)
                        )

                        // bubbles
                        val bubbles = 5
                        for (i in 0 until bubbles) {
                            val seed = i * 91.7f
                            val phaseB = ((tick / 20L + (seed * 7L).toLong()) % 4000L) / 4000f
                            val bx = center.x - inner + ((seed * 53f) % (inner * 2f))
                            val by = center.y + inner - phaseB * inner * 2f
                            if (by > waterTop) {
                                drawCircle(
                                    color = Color.White.copy(
                                        alpha = (1f - phaseB).coerceIn(0f, 0.55f)
                                    ),
                                    radius = (2.5f + seed % 1.5f).dp.toPx(),
                                    center = Offset(bx, by)
                                )
                            }
                        }
                    }

                    // specular highlight on outer ring
                    drawArc(
                        color = Color.White.copy(alpha = 0.25f),
                        startAngle = 215f,
                        sweepAngle = 40f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = stroke / 2.2f, cap = StrokeCap.Round)
                    )
                }

                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Today",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFB3E5FC)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$currentMl",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "of $goalMl ml",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFB3E5FC)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${(pct * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}
