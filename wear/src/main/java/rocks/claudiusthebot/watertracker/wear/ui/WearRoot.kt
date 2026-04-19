package rocks.claudiusthebot.watertracker.wear.ui

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.wear.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch
import rocks.claudiusthebot.watertracker.wear.WaterStore
import rocks.claudiusthebot.watertracker.wear.health.WearHealthConnect

@Composable
fun WearRoot(store: WaterStore, hc: WearHealthConnect) {
    MaterialTheme {
        val today by store.today.collectAsState()
        val ready by store.ready.collectAsState()

        val scope = rememberCoroutineScope()
        val quicks = remember { mutableStateOf(listOf(200, 300, 500)) }
        LaunchedEffect(Unit) {
            quicks.value = store.currentQuicks()
        }

        val permLauncher = rememberLauncherForActivityResult(
            contract = hc.permissionContract()
        ) { _ ->
            scope.launch { store.refresh() }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (ready.availability != WearHealthConnect.Availability.INSTALLED) {
                UnsupportedScreen()
                return@Box
            }
            if (!ready.hasPermissions) {
                PermissionScreen { permLauncher.launch(WearHealthConnect.PERMISSIONS) }
                return@Box
            }

            val listState = rememberScalingLazyListState()
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 28.dp, bottom = 32.dp,
                    start = 6.dp, end = 6.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { ProgressHero(total = today.totalMl, goal = today.goalMl) }
                item {
                    QuickRow(quicks = quicks.value) { ml ->
                        scope.launch { store.addIntake(ml) }
                    }
                }
                item {
                    CustomRow { ml ->
                        scope.launch { store.addIntake(ml) }
                    }
                }
                item { Spacer(Modifier.height(2.dp)) }
                item {
                    GoalRow(goalMl = today.goalMl) { delta ->
                        scope.launch { store.setGoal((today.goalMl + delta).coerceIn(500, 6000)) }
                    }
                }
                if (today.entries.isNotEmpty()) {
                    item {
                        Text(
                            "Recent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(today.entries.take(5)) { e ->
                        Card(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${e.volumeMl} ml",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (e.source.contains("wear")) "⌚" else "📱",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressHero(total: Int, goal: Int) {
    val pct = if (goal > 0) (total.toFloat() / goal).coerceIn(0f, 1.4f) else 0f
    Box(
        modifier = Modifier
            .size(130.dp)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val track = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val pad = stroke
            val sz = Size(size.width - pad * 2, size.height - pad * 2)
            val origin = Offset(pad, pad)
            drawArc(
                color = track,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = origin, size = sz,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = primary,
                startAngle = -90f, sweepAngle = 360f * pct.coerceAtMost(1f),
                useCenter = false, topLeft = origin, size = sz,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$total",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "/ $goal ml",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickRow(quicks: List<Int>, onPick: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        quicks.take(3).forEach { ml ->
            FilledTonalButton(
                onClick = { onPick(ml) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+$ml", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CustomRow(onPick: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Button(
            onClick = { onPick(100) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledVariantButtonColors()
        ) {
            Text("+100", fontSize = 11.sp)
        }
        Button(
            onClick = { onPick(50) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledVariantButtonColors()
        ) {
            Text("+50", fontSize = 11.sp)
        }
    }
}

@Composable
private fun GoalRow(goalMl: Int, onStep: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(
            onClick = { onStep(-250) },
            modifier = Modifier.weight(1f)
        ) { Text("−goal", fontSize = 10.sp) }
        Text(
            "$goalMl",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp
        )
        FilledTonalButton(
            onClick = { onStep(250) },
            modifier = Modifier.weight(1f)
        ) { Text("+goal", fontSize = 10.sp) }
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Hydrate",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Grant Health Connect permission for hydration",
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Button(onClick = onGrant) {
            Text("Grant", fontSize = 12.sp)
        }
    }
}

@Composable
private fun UnsupportedScreen() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Health Connect not available on this watch",
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

