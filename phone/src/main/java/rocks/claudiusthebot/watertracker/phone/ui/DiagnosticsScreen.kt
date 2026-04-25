package rocks.claudiusthebot.watertracker.phone.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rocks.claudiusthebot.watertracker.phone.WaterApp
import rocks.claudiusthebot.watertracker.phone.WaterViewModel
import rocks.claudiusthebot.watertracker.phone.health.HealthConnectManager
import rocks.claudiusthebot.watertracker.phone.sync.WearSync

@Composable
fun DiagnosticsScreen(vm: WaterViewModel) {
    val hc by vm.hcState.collectAsState()
    val lastHcEvent by vm.lastHcEvent.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as WaterApp
    val wearSync = remember { WearSync(context) }

    val launcher = rememberLauncherForActivityResult(
        contract = rememberHealthConnectPermissionContract()
    ) { _: Set<String> ->
        vm.onPermissionsResult()
    }

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
        item { DiagnosticsBlurb() }

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
    }
}

@Composable
private fun DiagnosticsBlurb() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Connection status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Verify Health Connect permissions and that your watch is reachable for sync.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
