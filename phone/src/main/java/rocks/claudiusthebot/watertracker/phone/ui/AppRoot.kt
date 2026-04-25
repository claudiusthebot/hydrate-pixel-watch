package rocks.claudiusthebot.watertracker.phone.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import rocks.claudiusthebot.watertracker.phone.WaterViewModel
import rocks.claudiusthebot.watertracker.phone.health.HealthConnectManager

/**
 * Top-level decision point: branches between the first-launch onboarding
 * and the main app. Once the user is in the main app, also overlays a
 * permission re-prompt dialog if Health Connect access has been revoked.
 */
@Composable
fun AppRoot(vm: WaterViewModel) {
    val onboardingComplete by vm.onboardingComplete.collectAsState()

    when (onboardingComplete) {
        null -> SplashFiller()
        false -> OnboardingScreen(
            vm = vm,
            onDone = { vm.completeOnboarding() }
        )
        true -> {
            RootNav(vm = vm)
            HcPermissionGate(vm = vm)
        }
    }
}

/**
 * Shown briefly while DataStore loads. Same surface color as the rest of
 * the app so there's no visible flash before the routed UI appears.
 */
@Composable
private fun SplashFiller() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        content = {}
    )
}

/**
 * If the user has finished onboarding but Health Connect access has been
 * revoked from outside the app (Settings → Health Connect), prompt them
 * to re-grant. Dismissable for the rest of the session — diagnostics still
 * surfaces it permanently.
 */
@Composable
private fun HcPermissionGate(vm: WaterViewModel) {
    val hcState by vm.hcState.collectAsState()

    var dismissedThisSession by remember { mutableStateOf(false) }

    val needsGrant = hcState.availability == HealthConnectManager.Availability.INSTALLED &&
        !hcState.hasPermissions

    val launcher = rememberLauncherForActivityResult(
        contract = rememberHealthConnectPermissionContract()
    ) { _: Set<String> ->
        vm.onPermissionsResult()
    }

    if (needsGrant && !dismissedThisSession) {
        AlertDialog(
            onDismissRequest = { dismissedThisSession = true },
            icon = {
                Icon(
                    Icons.Rounded.HealthAndSafety,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    "Health Connect access",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    "Hydrate stores your water intake in Health Connect so it stays in sync between your phone and watch. Grant access to keep tracking.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = {
                    launcher.launch(HealthConnectManager.PERMISSIONS)
                }) {
                    Text("Grant access")
                }
            },
            dismissButton = {
                TextButton(onClick = { dismissedThisSession = true }) {
                    Text("Not now")
                }
            }
        )
    }
}

