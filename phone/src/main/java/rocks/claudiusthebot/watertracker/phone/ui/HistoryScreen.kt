package rocks.claudiusthebot.watertracker.phone.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rocks.claudiusthebot.watertracker.phone.WaterViewModel
import rocks.claudiusthebot.watertracker.shared.DaySummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(vm: WaterViewModel) {
    var days by remember { mutableStateOf(emptyList<DaySummary>()) }

    LaunchedEffect(Unit) {
        val today = LocalDate.now()
        val list = (0..6).map { offset ->
            vm.loadDate(today.minusDays(offset.toLong()))
        }
        days = list
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Last 7 days",
                style = MaterialTheme.typography.headlineSmall
            )
        }
        item {
            WeeklyBarCard(days = days)
        }
        items(days, key = { it.date }) { d ->
            DayRow(d)
        }
    }
}

@Composable
private fun WeeklyBarCard(days: List<DaySummary>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Weekly intake",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(12.dp))

            val max = (days.maxOfOrNull { maxOf(it.totalMl, it.goalMl) } ?: 2000).toFloat()
            val barColor = MaterialTheme.colorScheme.primary
            val goalColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                if (days.isEmpty()) return@Canvas
                val n = days.size
                val barSpacing = 8.dp.toPx()
                val totalW = size.width
                val barW = (totalW - barSpacing * (n - 1)) / n
                val sortedOld = days.reversed()

                sortedOld.forEachIndexed { i, d ->
                    val x = i * (barW + barSpacing)
                    // goal tick
                    val goalY = size.height * (1f - (d.goalMl.toFloat() / max).coerceAtMost(1f))
                    drawLine(
                        color = goalColor,
                        start = Offset(x, goalY),
                        end = Offset(x + barW, goalY),
                        strokeWidth = 2f
                    )
                    // intake bar
                    val h = size.height * (d.totalMl.toFloat() / max).coerceAtMost(1f)
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, size.height - h),
                        size = Size(barW, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 4)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.reversed().forEach {
                    Text(
                        text = shortDay(it.date),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun DayRow(day: DaySummary) {
    val pct = if (day.goalMl > 0) (day.totalMl.toFloat() / day.goalMl).coerceIn(0f, 1.5f) else 0f
    val hit = pct >= 1f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (hit)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(longDay(day.date), style = MaterialTheme.typography.titleMedium)
                Text("${day.totalMl} / ${day.goalMl} ml",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "${(pct * 100).toInt()}%",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun shortDay(dateIso: String): String =
    LocalDate.parse(dateIso).format(DateTimeFormatter.ofPattern("EEE"))

private fun longDay(dateIso: String): String {
    val d = LocalDate.parse(dateIso)
    val today = LocalDate.now()
    return when (d) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> d.format(DateTimeFormatter.ofPattern("EEE d MMM"))
    }
}
