package rocks.claudiusthebot.watertracker.phone.ui

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rocks.claudiusthebot.watertracker.phone.WaterViewModel
import rocks.claudiusthebot.watertracker.phone.health.HealthConnectManager
import rocks.claudiusthebot.watertracker.shared.WaterEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(vm: WaterViewModel) {
    val today by vm.today.collectAsState()
    val hc by vm.hcState.collectAsState()
    val settings by vm.settings.collectAsState()
    val lastHcEvent by vm.lastHcEvent.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = rememberHealthConnectPermissionContract()
    ) { _: Set<String> ->
        vm.onPermissionsResult()
    }

    var sheetOpen by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as rocks.claudiusthebot.watertracker.phone.WaterApp
    val wearSync = remember { rocks.claudiusthebot.watertracker.phone.sync.WearSync(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HcDiagnosticsCard(
                availability = hc.availability,
                hasPermissions = hc.hasPermissions,
                lastEvent = lastHcEvent,
                app = app,
                onOpenPermissions = { launcher.launch(HealthConnectManager.PERMISSIONS) }
            )
        }
        item { SyncDiagnosticsCard(sync = wearSync) }

        item {
            WaterFillHero(
                currentMl = today.totalMl,
                goalMl = today.goalMl
            )
        }

        item {
            QuickDrinkGrid(
                quickAdds = settings.quickAddsMl,
                onPick = { vm.addIntake(it) },
                onCustom = { sheetOpen = true }
            )
        }

        if (today.entries.isEmpty()) {
            item { EmptyTodayIllustration() }
        } else {
            item { TodayLogHeader(count = today.entries.size, totalMl = today.totalMl) }
            items(today.entries, key = { it.id }) { entry ->
                EntryRow(entry = entry, onDelete = { vm.delete(entry) })
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
private fun TodayLogHeader(count: Int, totalMl: Int) {
    Row(
        Modifier
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
private fun EntryRow(entry: WaterEntry, onDelete: () -> Unit) {
    val fromWatch = entry.source.contains("wear")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Drop disc
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFF0288D1).copy(alpha = 0.12f),
                        radius = size.minDimension / 2f,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }
                Icon(
                    Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
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
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete entry")
            }
        }
    }
}

@Composable
private fun EmptyTodayIllustration() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            listOf(
                                Color(0xFF4FC3F7).copy(alpha = 0.35f),
                                Color(0xFF0288D1).copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension / 2f,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }
                Icon(
                    Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
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
private fun rememberHealthConnectPermissionContract():
    androidx.activity.result.contract.ActivityResultContract<Set<String>, Set<String>> {
    return remember {
        androidx.health.connect.client.PermissionController
            .createRequestPermissionResultContract()
    }
}
