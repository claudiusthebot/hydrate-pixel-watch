package rocks.claudiusthebot.watertracker.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.MotionScheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import kotlinx.coroutines.launch
import rocks.claudiusthebot.watertracker.wear.WaterStore

/**
 * Root entry point. The Wear OS 6 / Material 3 Expressive recipe:
 *  - `MotionScheme.expressive()` so M3 components use the bouncy 1.5
 *    spring set across the app.
 *  - `AppScaffold` owns `TimeText` (single subscription).
 *  - `ScreenScaffold` owns the per-screen edge button / position
 *    indicator.
 *  - `TransformingLazyColumn` + `rememberTransformationSpec` give items
 *    the focal-edge scale/fade transformation as they leave the lens.
 */
@Composable
fun WearRoot(store: WaterStore) {
    MaterialTheme(motionScheme = MotionScheme.expressive()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AppScaffold(
                timeText = { TimeText() }
            ) {
                MainScreen(store)
            }
        }
    }
}

@Composable
private fun MainScreen(store: WaterStore) {
    val today by store.today.collectAsState()
    val conn by store.connection.collectAsState()

    val scope = rememberCoroutineScope()
    var quicks by remember { mutableStateOf(listOf(150, 250, 500)) }
    LaunchedEffect(Unit) { quicks = store.currentQuicks() }

    var customSheetOpen by remember { mutableStateOf(false) }
    var celebrating by remember { mutableStateOf(false) }
    var lastCelebrateDate by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(today.totalMl, today.goalMl, today.date) {
        val hit = today.goalMl > 0 && today.totalMl >= today.goalMl
        if (hit && lastCelebrateDate != today.date) {
            lastCelebrateDate = today.date
            celebrating = true
        }
    }

    val lastUsedMl = remember(quicks, today.entries) {
        today.entries.firstOrNull()?.volumeMl ?: quicks.getOrNull(1) ?: 250
    }

    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val celebrateHaptic = rememberWearCelebrate()
    LaunchedEffect(celebrating) {
        if (celebrating) celebrateHaptic()
    }

    ScreenScaffold(
        scrollState = listState,
        contentPadding = PaddingValues(
            top = 28.dp, bottom = 42.dp, start = 10.dp, end = 10.dp
        ),
        edgeButton = {
            QuickLogEdgeButton(
                ml = lastUsedMl,
                onClick = {
                    scope.launch { store.addIntake(lastUsedMl) }
                }
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .rotaryScrollable(
                    behavior = RotaryScrollableDefaults.behavior(listState),
                    focusRequester = focusRequester
                ),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                HeroTile(totalMl = today.totalMl, goalMl = today.goalMl)
            }
            item {
                Box(
                    modifier = Modifier
                        .transformedHeight(this, transformationSpec)
                ) {
                    SyncStatusChip(conn)
                }
            }

            items(quicks.take(3)) { ml ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this@items, transformationSpec)
                ) {
                    DrinkChip(
                        ml = ml,
                        containerColor = colorForDrink(ml),
                        contentColor = contentForDrink(ml),
                        onPick = { scope.launch { store.addIntake(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                ) {
                    CustomPill(onClick = { customSheetOpen = true })
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                ) {
                    GoalAdjuster(
                        goalMl = today.goalMl,
                        onStep = { delta ->
                            scope.launch {
                                store.setGoal((today.goalMl + delta).coerceIn(500, 6000))
                            }
                        }
                    )
                }
            }

            if (today.entries.isNotEmpty()) {
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        Text(
                            "Today",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                items(today.entries.take(5)) { entry ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this@items, transformationSpec)
                    ) {
                        EntryRow(entry)
                    }
                }
            }
        }

        CelebrateOverlay(
            visible = celebrating,
            onDone = { celebrating = false }
        )
    }

    if (customSheetOpen) {
        CustomAmountDialog(
            initialMl = lastUsedMl,
            onDismiss = { customSheetOpen = false },
            onConfirm = { ml ->
                customSheetOpen = false
                scope.launch { store.addIntake(ml) }
            }
        )
    }
}

@Composable
private fun QuickLogEdgeButton(ml: Int, onClick: () -> Unit) {
    val confirm = rememberWearConfirm()
    EdgeButton(
        onClick = { confirm(); onClick() },
        buttonSize = EdgeButtonSize.Large
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                WaterIcons.Plus,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(4.dp))
            Text(
                "$ml ml",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CustomPill(onClick: () -> Unit) {
    val tick = rememberWearTick()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { tick(); onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                WaterIcons.Plus,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.size(6.dp))
            Text(
                "Custom amount…",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun SyncStatusChip(conn: WaterStore.ConnectionState) {
    val (label, dotColor) = when {
        conn.pendingSync > 0 -> "${conn.pendingSync} pending" to Color(0xFFFFB74D)
        conn.phoneReachable  -> "Synced"                      to Color(0xFF81C784)
        else                 -> "Waiting for phone"           to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    }
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, shape = CircleShape)
        )
        Spacer(Modifier.size(5.dp))
        Text(
            label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EntryRow(entry: rocks.claudiusthebot.watertracker.shared.WaterEntry) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${entry.volumeMl} ml",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
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
private fun colorForDrink(ml: Int): Color = when {
    ml <= 150 -> MaterialTheme.colorScheme.tertiaryContainer
    ml <= 400 -> MaterialTheme.colorScheme.primaryContainer
    else      -> MaterialTheme.colorScheme.secondaryContainer
}

@Composable
private fun contentForDrink(ml: Int): Color = when {
    ml <= 150 -> MaterialTheme.colorScheme.onTertiaryContainer
    ml <= 400 -> MaterialTheme.colorScheme.onPrimaryContainer
    else      -> MaterialTheme.colorScheme.onSecondaryContainer
}
