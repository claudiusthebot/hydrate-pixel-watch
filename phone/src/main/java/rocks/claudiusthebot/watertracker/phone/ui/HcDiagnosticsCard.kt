package rocks.claudiusthebot.watertracker.phone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rocks.claudiusthebot.watertracker.phone.WaterApp
import rocks.claudiusthebot.watertracker.phone.health.HealthConnectManager

@Composable
fun HcDiagnosticsCard(
    availability: HealthConnectManager.Availability?,
    hasPermissions: Boolean,
    lastEvent: String?,
    app: WaterApp,
    onOpenPermissions: () -> Unit
) {
    var granted by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(hasPermissions, availability) {
        granted = app.hc.grantedPermissions()
    }

    val container = when {
        availability != HealthConnectManager.Availability.INSTALLED ->
            MaterialTheme.colorScheme.errorContainer
        !hasPermissions ->
            MaterialTheme.colorScheme.secondaryContainer
        else ->
            MaterialTheme.colorScheme.tertiaryContainer
    }
    val onContainer = when {
        availability != HealthConnectManager.Availability.INSTALLED ->
            MaterialTheme.colorScheme.onErrorContainer
        !hasPermissions ->
            MaterialTheme.colorScheme.onSecondaryContainer
        else ->
            MaterialTheme.colorScheme.onTertiaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.HealthAndSafety,
                    contentDescription = null,
                    tint = onContainer
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Health Connect",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onContainer
                )
            }

            Spacer(Modifier.height(10.dp))

            Row2("SDK status", when (availability) {
                HealthConnectManager.Availability.INSTALLED -> "available"
                HealthConnectManager.Availability.NEEDS_UPDATE -> "update needed"
                HealthConnectManager.Availability.NOT_SUPPORTED -> "not supported"
                null -> "checking…"
            }, onContainer)
            Row2("Permissions", "${granted.size} / ${HealthConnectManager.PERMISSIONS.size}",
                onContainer)
            if (lastEvent != null) {
                Row2("Last event", lastEvent, onContainer)
            }

            Spacer(Modifier.height(14.dp))

            when (availability) {
                HealthConnectManager.Availability.INSTALLED -> {
                    if (!hasPermissions) {
                        Button(onClick = onOpenPermissions, shape = MaterialTheme.shapes.medium) {
                            Text("Grant permissions")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                app.hc.openHealthConnectIntent()?.let { app.startActivity(it) }
                            },
                            shape = MaterialTheme.shapes.medium
                        ) { Text("Open Health Connect") }
                    }
                }
                HealthConnectManager.Availability.NEEDS_UPDATE,
                HealthConnectManager.Availability.NOT_SUPPORTED -> {
                    Button(
                        onClick = {
                            app.hc.openHealthConnectIntent()?.let { app.startActivity(it) }
                        },
                        shape = MaterialTheme.shapes.medium
                    ) { Text("Install / Update Health Connect") }
                }
                null -> Unit
            }
        }
    }
}

@Composable
private fun Row2(label: String, value: String, fg: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = fg.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium)
        Text(value, color = fg, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium)
    }
}

