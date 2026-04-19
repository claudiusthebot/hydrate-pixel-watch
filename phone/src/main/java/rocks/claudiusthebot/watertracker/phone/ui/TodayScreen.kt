package rocks.claudiusthebot.watertracker.phone.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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

    val launcher = rememberLauncherForActivityResult(
        contract = rememberHealthConnectPermissionContract()
    ) { _: Set<String> ->
        vm.onPermissionsResult()
    }

    var customOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp, vertical = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Health connect card — surface first if not ready
        when (hc.availability) {
            HealthConnectManager.Availability.NOT_SUPPORTED, null -> Unit
            HealthConnectManager.Availability.NEEDS_UPDATE -> item { HcNeedsInstallCard() }
            HealthConnectManager.Availability.INSTALLED -> {
                if (!hc.hasPermissions) {
                    item {
                        HcConnectCard(onConnect = { launcher.launch(HealthConnectManager.PERMISSIONS) })
                    }
                }
            }
        }

        item {
            HeroProgress(
                current = today.totalMl,
                goal = today.goalMl
            )
        }

        item {
            QuickAddStrip(
                quickAdds = settings.quickAddsMl,
                onPick = { vm.addIntake(it) },
                onCustom = { customOpen = true }
            )
        }

        if (today.entries.isEmpty()) {
            item {
                Text(
                    "Nothing logged yet today — tap a quick-add or + to begin.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            item {
                Text(
                    "Today's log",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(today.entries, key = { it.id }) { entry ->
                EntryRow(entry = entry, onDelete = { vm.delete(entry) })
            }
        }
    }

    if (customOpen) {
        CustomAmountDialog(
            onDismiss = { customOpen = false },
            onConfirm = { ml ->
                customOpen = false
                if (ml > 0) vm.addIntake(ml)
            }
        )
    }
}

@Composable
private fun HeroProgress(current: Int, goal: Int) {
    val pct = if (goal > 0) (current.toFloat() / goal).coerceIn(0f, 1.4f) else 0f
    val animated by animateFloatAsState(pct, tween(durationMillis = 700), label = "progress")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Today",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(6.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
                val track = onContainer.copy(alpha = 0.15f)
                val primary = MaterialTheme.colorScheme.primary
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 18.dp.toPx()
                    val pad = stroke
                    val sz = Size(size.width - pad * 2, size.height - pad * 2)
                    val origin = androidx.compose.ui.geometry.Offset(pad, pad)
                    // track
                    drawArc(
                        color = track,
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = origin, size = sz,
                        style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    // fill
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(primary, primary.copy(alpha = 0.7f), primary),
                            center = androidx.compose.ui.geometry.Offset(
                                origin.x + sz.width / 2,
                                origin.y + sz.height / 2
                            )
                        ),
                        startAngle = -90f, sweepAngle = 360f * animated.coerceAtMost(1f),
                        useCenter = false,
                        topLeft = origin, size = sz,
                        style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$current",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "of $goal ml",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${(animated * 100).toInt()}% of goal",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddStrip(
    quickAdds: List<Int>,
    onPick: (Int) -> Unit,
    onCustom: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Quick add", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                quickAdds.forEach { ml ->
                    Button(
                        onClick = { onPick(ml) },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("$ml")
                    }
                }
                OutlinedButton(
                    onClick = onCustom,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: WaterEntry, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${entry.volumeMl} ml",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatTime(entry.timestampMs) +
                        if (entry.source.contains("wear")) " · ⌚" else "",
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
private fun HcConnectCard(onConnect: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Connect Health Connect",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(6.dp))
            Text("Grant read + write permission for hydration records so intake syncs across your devices.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onConnect, shape = MaterialTheme.shapes.medium) {
                Text("Grant permission")
            }
        }
    }
}

@Composable
private fun HcNeedsInstallCard() {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Health Connect isn't installed",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(6.dp))
            Text("Install Health Connect from Play Store to enable cross-device sync.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun CustomAmountDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom amount") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() } },
                label = { Text("ml") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toIntOrNull() ?: 0) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatTime(ms: Long): String {
    val dt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    return dt.format(DateTimeFormatter.ofPattern("HH:mm"))
}

@Composable
private fun rememberHealthConnectPermissionContract():
    androidx.activity.result.contract.ActivityResultContract<Set<String>, Set<String>> {
    val contract = remember {
        androidx.health.connect.client.PermissionController
            .createRequestPermissionResultContract()
    }
    return contract
}

