package rocks.claudiusthebot.watertracker.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import rocks.claudiusthebot.watertracker.phone.sync.WearSync

/**
 * Shows watch reachability via the Wearable capability — updates every 3s
 * while visible so the user can actually watch a disconnected/reconnected
 * state flip.
 */
@Composable
fun SyncDiagnosticsCard(sync: WearSync) {
    var reachable by remember { mutableStateOf<Boolean?>(null) }
    var nodeCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            val ids = sync.wearNodeIds()
            reachable = ids.isNotEmpty()
            nodeCount = ids.size
            delay(3_000)
        }
    }

    val container = when (reachable) {
        true -> MaterialTheme.colorScheme.tertiaryContainer
        false -> MaterialTheme.colorScheme.surfaceVariant
        null -> MaterialTheme.colorScheme.surfaceVariant
    }
    val onContainer = when (reachable) {
        true -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dotColor = when (reachable) {
        true -> Color(0xFF4CAF50)
        false -> Color(0xFFB0BEC5)
        null -> Color(0xFFFFB74D)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Watch,
                contentDescription = null,
                tint = onContainer
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Watch sync",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onContainer
                )
                Text(
                    text = when (reachable) {
                        true -> if (nodeCount == 1) "Paired, reachable"
                                else "$nodeCount watches reachable"
                        false -> "No watch with Hydrate found"
                        null -> "Checking…"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainer.copy(alpha = 0.75f)
                )
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}
