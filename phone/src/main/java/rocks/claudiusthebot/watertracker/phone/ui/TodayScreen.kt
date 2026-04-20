package rocks.claudiusthebot.watertracker.phone.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

    val launcher = rememberLauncherForActivityResult(
        contract = rememberHealthConnectPermissionContract()
    ) { _: Set<String> ->
        vm.onPermissionsResult()
    }

    var sheetOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (hc.availability) {
            HealthConnectManager.Availability.NOT_SUPPORTED, null -> Unit
            HealthConnectManager.Availability.NEEDS_UPDATE -> item { HcNeedsInstallCard() }
            HealthConnectManager.Availability.INSTALLED -> if (!hc.hasPermissions) {
                item {
                    HcConnectCard(onConnect = { launcher.launch(HealthConnectManager.PERMISSIONS) })
                }
            }
        }

        item {
            WaterFillHero(
                currentMl = today.totalMl,
                goalMl = today.goalMl
            )
        }

        item {
            QuickAddStrip(
                quickAdds = settings.quickAddsMl,
                onPick = { vm.addIntake(it) },
                onCustom = { sheetOpen = true }
            )
        }

        if (today.entries.isEmpty()) {
            item {
                Text(
                    "Nothing logged yet today — tap a quick-add to begin.",
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
private fun QuickAddStrip(
    quickAdds: List<Int>,
    onPick: (Int) -> Unit,
    onCustom: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.LocalDrink,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Quick add", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                quickAdds.take(4).forEach { ml ->
                    QuickButton(
                        label = "$ml",
                        modifier = Modifier.weight(1f),
                        onClick = { onPick(ml) }
                    )
                }
                OutlinedButton(
                    onClick = onCustom,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(0.6f)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Custom")
                }
            }
        }
    }
}

@Composable
private fun QuickButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Button(
        onClick = onClick,
        interactionSource = source,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.scale(scale)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EntryRow(entry: WaterEntry, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.LocalDrink,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${entry.volumeMl} ml",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatTime(entry.timestampMs) +
                        if (entry.source.contains("wear")) " · from watch" else "",
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
    ElevatedCard(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Connect Health Connect",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(6.dp))
            Text("Grant read + write permission for hydration records so intake syncs across your devices.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onConnect, shape = MaterialTheme.shapes.medium) {
                Text("Grant permission")
            }
        }
    }
}

@Composable
private fun HcNeedsInstallCard() {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Health Connect isn't installed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(6.dp))
            Text("Install Health Connect from Play Store to enable cross-device sync.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer)
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

