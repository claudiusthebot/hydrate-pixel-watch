package rocks.claudiusthebot.watertracker.phone.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import rocks.claudiusthebot.watertracker.phone.WaterViewModel
import rocks.claudiusthebot.watertracker.shared.WaterEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(vm: WaterViewModel) {
    val today by vm.today.collectAsState()
    val settings by vm.settings.collectAsState()

    var sheetOpen by remember { mutableStateOf(false) }

    val navInset = LocalFloatingNavInset.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 14.dp,
            bottom = 14.dp + navInset
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "hero") {
            WaterFillHero(
                currentMl = today.totalMl,
                goalMl = today.goalMl
            )
        }

        item(key = "quickAdd") {
            QuickDrinkGrid(
                quickAdds = settings.quickAddsMl,
                onPick = { vm.addIntake(it) },
                onCustom = { sheetOpen = true }
            )
        }

        if (today.entries.isEmpty()) {
            item(key = "empty") {
                EmptyTodayIllustration()
            }
        } else {
            item(key = "logHeader") {
                TodayLogHeader(
                    count = today.entries.size,
                    totalMl = today.totalMl
                )
            }
            // The whole entries section lives inside ONE LazyColumn item
            // and animates its own height via animateContentSize. This
            // avoids the per-item add/remove triggering a LazyColumn
            // re-measure of the entire list — which was making the
            // top-of-screen items (hero / quick-add / log header) jerk
            // when deletes happened on a short or fully-scrolled list.
            item(key = "entriesList") {
                Column(
                    modifier = Modifier.animateContentSize(
                        animationSpec = tween(
                            durationMillis = 260,
                            easing = FastOutSlowInEasing
                        )
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    today.entries.forEach { entry ->
                        key(entry.id) {
                            EntryRow(
                                entry = entry,
                                onDelete = { vm.delete(entry) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (sheetOpen) {
        CustomAmountSheet(
            onDismiss = { sheetOpen = false },
            onConfirm = { ml ->
                sheetOpen = false
                if (ml > 0) vm.addIntake(ml)
            }
        )
    }
}

@Composable
private fun TodayLogHeader(count: Int, totalMl: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 6.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Today's log",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            "$count drink${if (count == 1) "" else "s"} · $totalMl ml",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EntryRow(
    entry: WaterEntry,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fromWatch = entry.source.contains("wear")
    val reject = rememberRejectHaptic()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${entry.volumeMl} ml",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatTime(entry.timestampMs) +
                        if (fromWatch) " · from watch" else "",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { reject(); onDelete() }) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyTodayIllustration(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Nothing logged yet today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap a drink size above to start your day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val dt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    return dt.format(DateTimeFormatter.ofPattern("HH:mm"))
}

@Composable
internal fun rememberHealthConnectPermissionContract():
    androidx.activity.result.contract.ActivityResultContract<Set<String>, Set<String>> {
    return remember {
        androidx.health.connect.client.PermissionController
            .createRequestPermissionResultContract()
    }
}
