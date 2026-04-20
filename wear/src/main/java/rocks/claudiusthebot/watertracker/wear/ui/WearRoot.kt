package rocks.claudiusthebot.watertracker.wear.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch
import rocks.claudiusthebot.watertracker.wear.WaterStore
import kotlin.math.PI
import kotlin.math.sin

/**
 * Watch UI. Always renders the intake UI — no Health Connect gate on the
 * watch side, because Health Connect lives on the phone in our architecture.
 */
@Composable
fun WearRoot(store: WaterStore) {
    MaterialTheme {
        val today by store.today.collectAsState()
        val conn by store.connection.collectAsState()
        val scope = rememberCoroutineScope()

        var quicks by remember { mutableStateOf(listOf(200, 300, 500)) }
        LaunchedEffect(Unit) { quicks = store.currentQuicks() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF0B2034), Color(0xFF050A10))
                    )
                )
        ) {
            MainWearScreen(
                total = today.totalMl,
                goal = today.goalMl,
                entries = today.entries,
                connection = conn,
                quicks = quicks,
                onAdd = { ml -> scope.launch { store.addIntake(ml) } },
                onGoalStep = { delta ->
                    scope.launch {
                        store.setGoal((today.goalMl + delta).coerceIn(500, 6000))
                    }
                }
            )
        }
    }
}

@Composable
private fun MainWearScreen(
    total: Int,
    goal: Int,
    entries: List<rocks.claudiusthebot.watertracker.shared.WaterEntry>,
    connection: WaterStore.ConnectionState,
    quicks: List<Int>,
    onAdd: (Int) -> Unit,
    onGoalStep: (Int) -> Unit
) {
    val pct = if (goal > 0) (total.toFloat() / goal).coerceIn(0f, 1.5f) else 0f
    val animPct by animateFloatAsState(
        targetValue = pct,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pct"
    )
    val listState = rememberScalingLazyListState()

    Box(Modifier.fillMaxSize()) {
        EdgeProgressArc(fraction = animPct.coerceAtMost(1f))

        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 42.dp, bottom = 34.dp,
                start = 10.dp, end = 10.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { SyncChip(connection) }
            item { HeroWaterFill(total = total, goal = goal, pct = animPct) }
            item { QuickAddRow(quicks = quicks, onPick = onAdd) }
            item { MiniAddRow(onPick = onAdd) }
            item { GoalStepperRow(goalMl = goal, onStep = onGoalStep) }

            if (entries.isNotEmpty()) {
                item {
                    Text(
                        "Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6FB3E0)
                    )
                }
                items(entries.take(5)) { e -> EntryRow(e) }
            }
        }
        RimVignette()
    }
}

@Composable
private fun SyncChip(conn: WaterStore.ConnectionState) {
    val (label, color) = when {
        conn.pendingSync > 0 ->
            "${conn.pendingSync} pending…" to Color(0xFFFFB74D)
        conn.phoneReachable ->
            "Synced with phone" to Color(0xFF81C784)
        else ->
            "Waiting for phone" to Color(0xFF90A4AE)
    }
    Box(
        modifier = Modifier
            .background(
                color = Color.White.copy(alpha = 0.08f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, shape = androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(Modifier.size(6.dp))
            Text(
                label,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun EdgeProgressArc(fraction: Float) {
    val grad = listOf(
        Color(0xFF4FC3F7),
        Color(0xFF29B6F6),
        Color(0xFF0288D1),
        Color(0xFF01579B)
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = 10.dp.toPx()
        val inset = 6.dp.toPx() + stroke / 2f
        val sz = Size(size.width - inset * 2, size.height - inset * 2)
        val origin = Offset(inset, inset)
        drawArc(
            color = Color.White.copy(alpha = 0.08f),
            startAngle = -90f, sweepAngle = 360f, useCenter = false,
            topLeft = origin, size = sz,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            brush = Brush.sweepGradient(
                colors = grad,
                center = Offset(origin.x + sz.width / 2, origin.y + sz.height / 2)
            ),
            startAngle = -90f,
            sweepAngle = 360f * fraction,
            useCenter = false,
            topLeft = origin, size = sz,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun HeroWaterFill(total: Int, goal: Int, pct: Float) {
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) withFrameMillis { tick = it }
    }

    Box(
        modifier = Modifier.size(112.dp).padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f - 4.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = Color(0xFF0A1A2A),
                radius = radius,
                center = center
            )

            val waterTop = size.height * (1f - pct.coerceAtMost(1f))
            val amplitude = 3.dp.toPx()
            val frequency = 2.5f * PI.toFloat() / size.width
            val phase = (tick % 1800L) / 1800f * 2f * PI.toFloat()

            val water = Path().apply {
                moveTo(0f, waterTop)
                var x = 0f
                while (x <= size.width) {
                    val y = waterTop + amplitude * sin(frequency * x + phase)
                    lineTo(x, y)
                    x += 2f
                }
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }

            val clip = Path().apply {
                addOval(
                    Rect(center - Offset(radius, radius), Size(radius * 2, radius * 2))
                )
            }

            clipPath(clip) {
                drawPath(
                    path = water,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4FC3F7),
                            Color(0xFF0288D1),
                            Color(0xFF01579B)
                        )
                    )
                )
            }

            drawArc(
                color = Color.White.copy(alpha = 0.1f),
                startAngle = 200f, sweepAngle = 60f, useCenter = false,
                topLeft = center - Offset(radius - 3.dp.toPx(), radius - 3.dp.toPx()),
                size = Size((radius - 3.dp.toPx()) * 2, (radius - 3.dp.toPx()) * 2),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$total",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "of $goal ml",
                fontSize = 10.sp,
                color = Color(0xFFB3E5FC)
            )
        }
    }
}

@Composable
private fun QuickAddRow(quicks: List<Int>, onPick: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        quicks.take(3).forEach { ml ->
            FilledTonalButton(
                onClick = { onPick(ml) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+$ml", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MiniAddRow(onPick: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilledTonalButton(onClick = { onPick(50) }, modifier = Modifier.weight(1f)) {
            Text("+50", fontSize = 11.sp)
        }
        FilledTonalButton(onClick = { onPick(100) }, modifier = Modifier.weight(1f)) {
            Text("+100", fontSize = 11.sp)
        }
        FilledTonalButton(onClick = { onPick(150) }, modifier = Modifier.weight(1f)) {
            Text("+150", fontSize = 11.sp)
        }
    }
}

@Composable
private fun GoalStepperRow(goalMl: Int, onStep: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(onClick = { onStep(-250) }, modifier = Modifier.weight(1f)) {
            Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier.weight(1.5f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("goal", fontSize = 8.sp, color = Color(0xFF6FB3E0))
            Text(
                "$goalMl",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        FilledTonalButton(onClick = { onStep(250) }, modifier = Modifier.weight(1f)) {
            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EntryRow(entry: rocks.claudiusthebot.watertracker.shared.WaterEntry) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.06f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${entry.volumeMl} ml",
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (entry.source.contains("wear")) "⌚" else "📱",
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun RimVignette() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.75f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}
