package rocks.claudiusthebot.watertracker.wear.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import androidx.wear.compose.foundation.ArcPaddingValues
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.curvedText
import kotlin.math.PI
import kotlin.math.sin

/**
 * The main glanceable hero for the watch:
 *   • Animated wavy water fill inside a circle.
 *   • Rim progress arc (matches the phone hero's language).
 *   • Big total in the middle, "of {goal} ml" underneath.
 *   • Curved percentage label along the bottom rim of the card.
 *   • Gentle pulse when today's goal has been met.
 */
@Composable
fun HeroTile(
    totalMl: Int,
    goalMl: Int,
    modifier: Modifier = Modifier
) {
    val rawPct = if (goalMl > 0) (totalMl.toFloat() / goalMl).coerceIn(0f, 1.5f) else 0f
    val pct by animateFloatAsState(
        targetValue = rawPct,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "pct"
    )

    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) withFrameMillis { tick = it }
    }

    val goalHit = rawPct >= 1f
    val breathe = rememberInfiniteTransition(label = "breathe")
    val breatheScale by breathe.animateFloat(
        initialValue = 1f,
        targetValue = if (goalHit) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    Box(
        modifier = modifier.size(150.dp).padding(3.dp).scale(breatheScale),
        contentAlignment = Alignment.Center
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val primaryDim = MaterialTheme.colorScheme.primaryDim

        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2f + 2.dp.toPx()
            val radius = minOf(size.width, size.height) / 2f - inset
            val center = Offset(size.width / 2f, size.height / 2f)

            // rim track
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // progress
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF81D4FA),
                        primary,
                        primaryDim,
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

            // inner water-fill disc
            val innerR = radius - stroke - 4.dp.toPx()
            val disc = Path().apply {
                addOval(Rect(center - Offset(innerR, innerR), Size(innerR * 2, innerR * 2)))
            }
            clipPath(disc) {
                drawCircle(color = Color(0xFF07151F), radius = innerR, center = center)

                val waterTop = center.y + innerR - innerR * 2 * pct.coerceAtMost(1f)
                val amp = 3.dp.toPx()
                val freq = 2f * PI.toFloat() / (innerR * 2f)
                val phase = (tick % 2400L) / 2400f * 2f * PI.toFloat()

                val water = Path().apply {
                    moveTo(center.x - innerR, waterTop)
                    var x = center.x - innerR
                    while (x <= center.x + innerR) {
                        val y = waterTop + amp * sin(freq * x + phase)
                        lineTo(x, y)
                        x += 2f
                    }
                    lineTo(center.x + innerR, center.y + innerR)
                    lineTo(center.x - innerR, center.y + innerR)
                    close()
                }
                drawPath(
                    water,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4FC3F7),
                            Color(0xFF0288D1),
                            Color(0xFF01579B)
                        )
                    )
                )

                // parallax second wave
                val waterTop2 = waterTop + amp * 1.2f
                val phase2 = (tick % 3600L) / 3600f * 2f * PI.toFloat()
                val water2 = Path().apply {
                    moveTo(center.x - innerR, waterTop2)
                    var x = center.x - innerR
                    while (x <= center.x + innerR) {
                        val y = waterTop2 + amp * 0.7f * sin(freq * 1.3f * x + phase2)
                        lineTo(x, y)
                        x += 2f
                    }
                    lineTo(center.x + innerR, center.y + innerR)
                    lineTo(center.x - innerR, center.y + innerR)
                    close()
                }
                drawPath(water2, color = Color(0xFF29B6F6).copy(alpha = 0.4f))
            }
        }

        // Center text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$totalMl",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "of $goalMl ml",
                fontSize = 9.sp,
                color = Color(0xFFB3E5FC)
            )
        }

        // Curved percentage rim — the expressive Wear signature
        CurvedLayout(
            modifier = Modifier.fillMaxSize(),
            anchor = 90f,             // bottom of the circle
            angularDirection = CurvedDirection.Angular.Reversed
        ) {
            curvedText(
                text = "${(pct * 100).toInt()}%",
                style = CurvedTextStyle(
                    fontSize = 10.sp,
                    color = Color(0xFF81D4FA),
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}
