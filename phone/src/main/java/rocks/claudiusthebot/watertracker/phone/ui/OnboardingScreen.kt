package rocks.claudiusthebot.watertracker.phone.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import rocks.claudiusthebot.watertracker.phone.WaterViewModel
import rocks.claudiusthebot.watertracker.phone.health.HealthConnectManager
import rocks.claudiusthebot.watertracker.phone.ui.shapes.RoundedStarShape

@Composable
fun OnboardingScreen(
    vm: WaterViewModel,
    onDone: () -> Unit
) {
    val hcState by vm.hcState.collectAsState()
    val hcGranted = hcState.hasPermissions

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val tap = rememberTapHaptic()
    val confirm = rememberConfirmTick()

    val launcher = rememberLauncherForActivityResult(
        contract = rememberHealthConnectPermissionContract()
    ) { _: Set<String> ->
        vm.onPermissionsResult()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 4.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> PermissionsPage(
                        granted = hcGranted,
                        onGrant = {
                            tap()
                            launcher.launch(HealthConnectManager.PERMISSIONS)
                        }
                    )
                }
            }

            // Auto-advance from Welcome → Permissions when user presses
            // Continue. Don't auto-advance from Permissions → done; the
            // user has to grant first.

            BottomBar(
                page = pagerState.currentPage,
                pageCount = 2,
                hcGranted = hcGranted,
                onPrimaryClick = {
                    confirm()
                    when (pagerState.currentPage) {
                        0 -> scope.launch { pagerState.animateScrollToPage(1) }
                        1 -> if (hcGranted) onDone()
                            else launcher.launch(HealthConnectManager.PERMISSIONS)
                    }
                }
            )
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        // Hero blob
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedStarShape(sides = 8, curve = 0.10))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.WaterDrop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(86.dp)
            )
        }

        Spacer(Modifier.height(36.dp))

        Text(
            text = "Welcome to Hydrate",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "A simple water tracker that lives on your phone and Pixel Watch.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(36.dp))

        // Feature bullets
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FeatureBullet(
                icon = Icons.Rounded.WaterDrop,
                title = "Quick logging",
                body = "Tap a preset or pick a custom amount."
            )
            FeatureBullet(
                icon = Icons.Rounded.Sync,
                title = "Phone + watch in sync",
                body = "Log from your wrist or your phone — totals stay aligned."
            )
            FeatureBullet(
                icon = Icons.Rounded.Notifications,
                title = "Gentle reminders",
                body = "Optional periodic nudges with a quick-log shortcut."
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PermissionsPage(
    granted: Boolean,
    onGrant: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        AnimatedContent(
            targetState = granted,
            transitionSpec = {
                (fadeIn(tween(220)) togetherWith fadeOut(tween(140)))
            },
            label = "hcHero"
        ) { isGranted ->
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedStarShape(sides = 8, curve = 0.10))
                    .background(
                        if (isGranted) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Rounded.Check
                        else Icons.Rounded.HealthAndSafety,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(if (isGranted) 80.dp else 78.dp)
                )
            }
        }

        Spacer(Modifier.height(36.dp))

        Text(
            text = if (granted) "All set" else "Connect Health",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (granted)
                "Health Connect is good to go. Tap Get started to begin tracking."
            else
                "Hydrate uses Health Connect to keep your water intake in sync between phone and watch — and shared with any other Health Connect app you use.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        AnimatedVisibility(visible = !granted) {
            OutlinedButton(
                onClick = onGrant,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Icon(Icons.Rounded.HealthAndSafety, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Grant Health Connect access",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun FeatureBullet(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BottomBar(
    page: Int,
    pageCount: Int,
    hcGranted: Boolean,
    onPrimaryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Page indicator dots
        Row(verticalAlignment = Alignment.CenterVertically) {
            for (i in 0 until pageCount) {
                val targetWidth = if (i == page) 22.dp else 8.dp
                val width by animateDpAsState(
                    targetValue = targetWidth,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "dotWidth"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .width(width)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (i == page) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                )
            }
        }

        // Primary CTA
        val label = when {
            page == 0 -> "Continue"
            !hcGranted -> "Grant access"
            else -> "Get started"
        }

        Button(
            onClick = onPrimaryClick,
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.height(56.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null
            )
        }
    }
}

@Suppress("unused")
@Composable
private fun StatusBarSpacer() {
    Box(modifier = Modifier.statusBarsPadding())
}
