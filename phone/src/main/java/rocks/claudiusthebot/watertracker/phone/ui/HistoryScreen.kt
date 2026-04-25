package rocks.claudiusthebot.watertracker.phone.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import rocks.claudiusthebot.watertracker.phone.WaterViewModel
import rocks.claudiusthebot.watertracker.phone.ui.shapes.RoundedStarShape
import rocks.claudiusthebot.watertracker.shared.DaySummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class HistoryRange(val days: Int, val label: String) {
    WEEK(7, "Week"),
    FORTNIGHT(14, "2 Weeks"),
    MONTH(30, "Month")
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(vm: WaterViewModel) {
    val allDays by vm.historyDays.collectAsState()
    val loading by vm.historyLoading.collectAsState()
    var range by remember { mutableStateOf(HistoryRange.WEEK) }

    // Trigger a refresh on every entry; the VM dedupes the spinner so
    // re-entering the tab uses the cache without flashing empty.
    LaunchedEffect(Unit) { vm.refreshHistory(30) }

    val days = remember(allDays, range) { allDays.take(range.days) }
    val stats = remember(days) { computeStats(days) }
    val isFirstLoad = loading && allDays.isEmpty()

    val navInset = LocalFloatingNavInset.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 14.dp,
            bottom = 14.dp + navInset
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item("hero") { HeroPair(stats = stats) }

        item("rangeTabs") {
            RangeTabsHeader(
                ranges = HistoryRange.entries,
                selected = range,
                onSelected = { range = it }
            )
        }

        item("progress") {
            DailyProgressCard(
                pct = stats.todayPct,
                hit = stats.todayHit,
                totalMl = stats.todayMl,
                goalMl = stats.todayGoal
            )
        }

        item("chart") {
            ChartCard(
                days = days,
                range = range
            )
        }

        item("habits") {
            HabitsCard(stats = stats, range = range)
        }

        item("daysHeader") {
            Text(
                text = "${range.label.uppercase()} · LOG",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )
        }

        // Day rows live in one item with a tight 3dp internal gap so the
        // grouped-corner trick reads as one connected card. Outer spacing
        // (18dp) still applies to neighbours above and below.
        item("daysList") {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                days.forEachIndexed { index, day ->
                    DayRow(
                        day = day,
                        position = when {
                            days.size == 1 -> RowPosition.Single
                            index == 0 -> RowPosition.First
                            index == days.size - 1 -> RowPosition.Last
                            else -> RowPosition.Middle
                        }
                    )
                }
            }
        }

        if (isFirstLoad) {
            item("loading") { LoadingFiller() }
        }
    }
}

@Composable
private fun LoadingFiller() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Loading…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// Stats model
// ─────────────────────────────────────────────────────────────────────

private data class HistoryStats(
    val todayMl: Int,
    val todayGoal: Int,
    val todayPct: Float,
    val todayHit: Boolean,
    val avgMl: Int,
    val daysHitGoal: Int,
    val streakDays: Int,
    val bestDayMl: Int,
    val bestDayLabel: String
)

private fun computeStats(days: List<DaySummary>): HistoryStats {
    if (days.isEmpty()) {
        return HistoryStats(0, 2000, 0f, false, 0, 0, 0, 0, "—")
    }
    val today = days.first()
    val pct = if (today.goalMl > 0) today.totalMl.toFloat() / today.goalMl else 0f
    val avg = days.map { it.totalMl }.average().toInt()
    val daysHit = days.count { it.goalMl > 0 && it.totalMl >= it.goalMl }
    // Streak counts back from today across consecutive goal-hit days.
    var streak = 0
    for (d in days) {
        if (d.goalMl > 0 && d.totalMl >= d.goalMl) streak++ else break
    }
    val best = days.maxByOrNull { it.totalMl }
    val bestLabel = best?.date?.let { iso ->
        val d = LocalDate.parse(iso)
        when (d) {
            LocalDate.now() -> "Today"
            LocalDate.now().minusDays(1) -> "Yesterday"
            else -> d.format(DateTimeFormatter.ofPattern("EEE d MMM"))
        }
    } ?: "—"
    return HistoryStats(
        todayMl = today.totalMl,
        todayGoal = today.goalMl,
        todayPct = pct.coerceIn(0f, 1.5f),
        todayHit = today.totalMl >= today.goalMl && today.goalMl > 0,
        avgMl = avg,
        daysHitGoal = daysHit,
        streakDays = streak,
        bestDayMl = best?.totalMl ?: 0,
        bestDayLabel = bestLabel
    )
}

// ─────────────────────────────────────────────────────────────────────
// Hero pair (Avg %, Streak)
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun HeroPair(stats: HistoryStats) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroTile(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            label = "Avg / day",
            value = "${stats.avgMl} ml",
            icon = Icons.AutoMirrored.Rounded.ShowChart,
            container = MaterialTheme.colorScheme.primaryContainer,
            onContainer = MaterialTheme.colorScheme.onPrimaryContainer
        )
        HeroTile(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            label = "Streak",
            value = if (stats.streakDays == 0) "—"
                else "${stats.streakDays} day${if (stats.streakDays == 1) "" else "s"}",
            icon = Icons.Rounded.LocalFireDepartment,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            onContainer = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun HeroTile(
    label: String,
    value: String,
    icon: ImageVector,
    container: Color,
    onContainer: Color,
    modifier: Modifier = Modifier
) {
    val displayValue by rememberAnimatedString(value)

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(container)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedStarShape(sides = 8, curve = 0.10))
                    .background(onContainer.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = onContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = onContainer.copy(alpha = 0.85f)
            )
        }
        Spacer(Modifier.height(2.dp))
        AnimatedContent(
            targetState = displayValue,
            transitionSpec = {
                (fadeIn(tween(220)) togetherWith fadeOut(tween(160)))
                    .using(androidx.compose.animation.SizeTransform(clip = false))
            },
            label = "heroValue"
        ) { v ->
            Text(
                text = v,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = onContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun rememberAnimatedString(value: String): androidx.compose.runtime.State<String> {
    val state = remember { androidx.compose.runtime.mutableStateOf(value) }
    LaunchedEffect(value) { state.value = value }
    return state
}

// ─────────────────────────────────────────────────────────────────────
// Range tabs
// ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RangeTabsHeader(
    ranges: List<HistoryRange>,
    selected: HistoryRange,
    onSelected: (HistoryRange) -> Unit
) {
    val selectedIndex = remember(selected, ranges) { ranges.indexOf(selected).coerceAtLeast(0) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 4.dp,
            divider = {},
            indicator = { TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.height(0.dp), color = Color.Transparent
            ) },
            containerColor = Color.Transparent
        ) {
            ranges.forEachIndexed { index, r ->
                AnimatedTabPill(
                    index = index,
                    selectedIndex = selectedIndex,
                    onClick = { onSelected(r) },
                    selectedColor = MaterialTheme.colorScheme.primary,
                    onSelectedColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedColor = MaterialTheme.colorScheme.surfaceContainer,
                    onUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        r.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (index == selectedIndex) FontWeight.Bold
                            else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Daily progress (wavy) card
// ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DailyProgressCard(
    pct: Float,
    hit: Boolean,
    totalMl: Int,
    goalMl: Int
) {
    val animatedPct by animateFloatAsState(
        targetValue = pct.coerceAtMost(1f),
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "dailyPct"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Today's progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (goalMl > 0) "$totalMl / $goalMl ml" else "$totalMl ml",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearWavyProgressIndicator(
            progress = { animatedPct },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape),
            color = if (hit) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        )
        Text(
            text = if (hit) "Goal reached. Nice."
                else "${(animatedPct * 100).toInt()}% of daily goal",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// Bar chart card (M3 stadium pills)
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun ChartCard(
    days: List<DaySummary>,
    range: HistoryRange
) {
    val sorted = remember(days) { days.reversed() } // oldest → newest
    val maxValue = remember(sorted) {
        (sorted.maxOfOrNull { it.totalMl } ?: 0).coerceAtLeast(1)
    }
    val goalRef = remember(sorted) {
        (sorted.firstOrNull { it.goalMl > 0 }?.goalMl ?: 2000)
    }
    val ceiling = remember(maxValue, goalRef) { maxOf(maxValue, goalRef) * 12 / 10 }

    val cardColor = when (range) {
        HistoryRange.WEEK -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        HistoryRange.FORTNIGHT -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
        HistoryRange.MONTH -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.40f)
    }
    val highlight = when (range) {
        HistoryRange.WEEK -> MaterialTheme.colorScheme.primary
        HistoryRange.FORTNIGHT -> MaterialTheme.colorScheme.secondary
        HistoryRange.MONTH -> MaterialTheme.colorScheme.tertiary
    }
    val regular = highlight.copy(alpha = 0.55f)
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(cardColor)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedStarShape(sides = 8, curve = 0.10))
                    .background(highlight.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Timeline,
                    contentDescription = null,
                    tint = highlight,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Intake timeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    when (range) {
                        HistoryRange.WEEK -> "Last 7 days"
                        HistoryRange.FORTNIGHT -> "Last 14 days"
                        HistoryRange.MONTH -> "Last 30 days"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val itemCount = sorted.size.coerceAtLeast(1)
            val spacing = if (range == HistoryRange.MONTH) 4.dp else 8.dp
            val totalSpacing = spacing * (itemCount - 1).coerceAtLeast(0)
            val perBar = ((this.maxWidth - totalSpacing) / itemCount).coerceAtLeast(8.dp)

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.Bottom,
                userScrollEnabled = false
            ) {
                itemsIndexed(sorted, key = { _, d -> d.date }) { _, d ->
                    val ratio = (d.totalMl.toFloat() / ceiling).coerceIn(0f, 1f)
                    val isPeak = d.totalMl == maxValue && d.totalMl > 0
                    BarColumn(
                        ratio = ratio,
                        label = shortDayLabel(d.date, range),
                        valueLabel = formatMlCompact(d.totalMl),
                        barWidth = perBar,
                        track = track,
                        fill = if (isPeak) highlight else regular,
                        emphasised = isPeak,
                        showValueLabel = range != HistoryRange.MONTH
                    )
                }
            }
        }

        // Highlight strip
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(highlight.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = highlight
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Peak day",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${formatMlCompact(maxValue)} ${peakPhrase(sorted, maxValue)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun BarColumn(
    ratio: Float,
    label: String,
    valueLabel: String,
    barWidth: androidx.compose.ui.unit.Dp,
    track: Color,
    fill: Color,
    emphasised: Boolean,
    showValueLabel: Boolean
) {
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "barRatio"
    )
    Column(
        modifier = Modifier.width(barWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showValueLabel) {
            Text(
                valueLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (emphasised) FontWeight.Bold else FontWeight.Medium,
                color = if (emphasised) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(CircleShape)
                .background(track),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp * animatedRatio)
                    .clip(CircleShape)
                    .background(fill)
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (emphasised) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// Habits card (4 metrics)
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun HabitsCard(stats: HistoryStats, range: HistoryRange) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Habits",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        HabitRow(
            icon = Icons.Rounded.WaterDrop,
            label = "Average per day",
            value = "${stats.avgMl} ml"
        )
        HabitRow(
            icon = Icons.Rounded.Check,
            label = "Days hit goal",
            value = "${stats.daysHitGoal} / ${range.days}"
        )
        HabitRow(
            icon = Icons.Rounded.AutoAwesome,
            label = "Best day",
            value = if (stats.bestDayMl == 0) "—"
                else "${stats.bestDayLabel} · ${formatMlCompact(stats.bestDayMl)}"
        )
        HabitRow(
            icon = Icons.Rounded.LocalFireDepartment,
            label = "Current streak",
            value = if (stats.streakDays == 0) "0 days"
                else "${stats.streakDays} day${if (stats.streakDays == 1) "" else "s"}"
        )
    }
}

@Composable
private fun HabitRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Per-day rows (grouped corners)
// ─────────────────────────────────────────────────────────────────────

private enum class RowPosition { Single, First, Middle, Last }

@Composable
private fun DayRow(day: DaySummary, position: RowPosition) {
    val pct = if (day.goalMl > 0) (day.totalMl.toFloat() / day.goalMl).coerceIn(0f, 1.5f) else 0f
    val hit = pct >= 1f
    val animatedPct by animateFloatAsState(
        targetValue = pct.coerceAtMost(1f),
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "dayPct"
    )
    val animatedTotal by animateIntAsState(
        targetValue = day.totalMl,
        animationSpec = tween(400),
        label = "dayMl"
    )

    val shape = when (position) {
        RowPosition.Single -> RoundedCornerShape(24.dp)
        RowPosition.First -> RoundedCornerShape(
            topStart = 24.dp, topEnd = 24.dp,
            bottomStart = 6.dp, bottomEnd = 6.dp
        )
        RowPosition.Middle -> RoundedCornerShape(6.dp)
        RowPosition.Last -> RoundedCornerShape(
            topStart = 6.dp, topEnd = 6.dp,
            bottomStart = 24.dp, bottomEnd = 24.dp
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = if (hit) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        longDay(day.date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hit) MaterialTheme.colorScheme.onTertiaryContainer
                            else MaterialTheme.colorScheme.onSurface
                    )
                    if (hit) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "$animatedTotal / ${day.goalMl} ml",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hit)
                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressBar(
                    pct = animatedPct,
                    hit = hit
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = "${(pct * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (hit) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LinearProgressBar(pct: Float, hit: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(pct)
                .height(6.dp)
                .clip(CircleShape)
                .background(
                    if (hit) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────

private fun shortDayLabel(dateIso: String, range: HistoryRange): String {
    val d = LocalDate.parse(dateIso)
    return when (range) {
        HistoryRange.WEEK -> d.format(DateTimeFormatter.ofPattern("EEE"))
        HistoryRange.FORTNIGHT -> d.format(DateTimeFormatter.ofPattern("d"))
        HistoryRange.MONTH -> d.format(DateTimeFormatter.ofPattern("d"))
    }
}

private fun longDay(dateIso: String): String {
    val d = LocalDate.parse(dateIso)
    val today = LocalDate.now()
    return when (d) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> d.format(DateTimeFormatter.ofPattern("EEE d MMM"))
    }
}

private fun formatMlCompact(ml: Int): String =
    if (ml >= 1000) String.format("%.1fL", ml / 1000f) else "${ml}ml"

/**
 * Renders the peak-day phrase with the right preposition for the date class:
 *   today      → "today"      (no "on" — reads naturally with the preceding ml value)
 *   yesterday  → "yesterday"
 *   any other  → "on Sat 25 Apr"
 */
private fun peakPhrase(sorted: List<DaySummary>, peak: Int): String {
    val match = sorted.firstOrNull { it.totalMl == peak } ?: return "—"
    val d = LocalDate.parse(match.date)
    val today = LocalDate.now()
    return when (d) {
        today -> "today"
        today.minusDays(1) -> "yesterday"
        else -> "on ${d.format(DateTimeFormatter.ofPattern("EEE d MMM"))}"
    }
}

@Suppress("unused")
@Composable
private fun TodayCalendarPill() {
    // Reserved for a future "today" pill in the chart card header.
    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
}
