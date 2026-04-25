package rocks.claudiusthebot.watertracker.phone.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import rocks.claudiusthebot.watertracker.phone.WaterViewModel

@Composable
fun SettingsScreen(
    vm: WaterViewModel,
    onOpenDiagnostics: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val settings by vm.settings.collectAsState()
    val reminders by vm.reminders.collectAsState()

    var goalFloat by remember(settings) { mutableStateOf(settings.dailyGoalMl.toFloat()) }
    LaunchedEffect(settings.dailyGoalMl) { goalFloat = settings.dailyGoalMl.toFloat() }

    val tick = rememberSegmentTick()
    val confirm = rememberConfirmTick()
    val toggleOn = rememberToggleOnHaptic()
    val toggleOff = rememberToggleOffHaptic()
    val lightTick = rememberLightTick()

    var lastGoalStep by remember(goalFloat) { mutableIntStateOf((goalFloat / 100f).toInt()) }
    var lastIntervalStep by remember(reminders.intervalMinutes) {
        mutableIntStateOf(reminders.intervalMinutes / 30)
    }
    var lastQuietStartHour by remember(reminders.quietStart) {
        mutableIntStateOf(reminders.quietStart)
    }
    var lastQuietEndHour by remember(reminders.quietEnd) {
        mutableIntStateOf(reminders.quietEnd)
    }

    val navInset = LocalFloatingNavInset.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 14.dp,
            bottom = 14.dp + navInset
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionHeader("Hydration") }

        item {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(22.dp)) {
                    SettingHeader(
                        icon = Icons.Rounded.WaterDrop,
                        title = "Daily goal"
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${goalFloat.toInt()} ml",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = goalFloat,
                        onValueChange = { new ->
                            val step = (new / 100f).toInt()
                            if (step != lastGoalStep) {
                                lastGoalStep = step
                                tick()
                            }
                            goalFloat = new
                        },
                        onValueChangeFinished = {
                            confirm()
                            vm.setGoal(goalFloat.toInt())
                        },
                        valueRange = 1000f..4000f,
                        steps = 29
                    )
                    Text(
                        "1000–4000 ml in 100 ml steps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            QuickAddEditor(
                current = settings.quickAddsMl,
                onSave = { vm.setQuickAdds(it) }
            )
        }

        item { SectionHeader("Reminders") }

        item {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingHeader(
                            icon = Icons.Rounded.NotificationsActive,
                            title = "Periodic nudges",
                            subtitle = "Quick-log notifications through the day",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = reminders.enabled,
                            onCheckedChange = {
                                if (it) toggleOn() else toggleOff()
                                vm.setReminderEnabled(it)
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = reminders.enabled,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                        exit = shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeOut()
                    ) {
                        Column {
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "Every ${reminders.intervalMinutes} min",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Slider(
                                value = reminders.intervalMinutes.toFloat(),
                                onValueChange = { new ->
                                    val step = new.toInt() / 30
                                    if (step != lastIntervalStep) {
                                        lastIntervalStep = step
                                        tick()
                                    }
                                    vm.setReminderInterval(new.toInt())
                                },
                                onValueChangeFinished = { confirm() },
                                valueRange = 30f..240f,
                                steps = 6
                            )

                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Quiet hours: " +
                                    "${reminders.quietStart.toString().padStart(2, '0')}:00 – " +
                                    "${reminders.quietEnd.toString().padStart(2, '0')}:00",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Start", modifier = Modifier.width(48.dp),
                                    style = MaterialTheme.typography.labelLarge)
                                Slider(
                                    value = reminders.quietStart.toFloat(),
                                    onValueChange = { new ->
                                        val h = new.toInt()
                                        if (h != lastQuietStartHour) {
                                            lastQuietStartHour = h
                                            tick()
                                        }
                                        vm.setQuietStart(h)
                                    },
                                    onValueChangeFinished = { confirm() },
                                    valueRange = 0f..23f,
                                    steps = 22
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("End", modifier = Modifier.width(48.dp),
                                    style = MaterialTheme.typography.labelLarge)
                                Slider(
                                    value = reminders.quietEnd.toFloat(),
                                    onValueChange = { new ->
                                        val h = new.toInt()
                                        if (h != lastQuietEndHour) {
                                            lastQuietEndHour = h
                                            tick()
                                        }
                                        vm.setQuietEnd(h)
                                    },
                                    onValueChangeFinished = { confirm() },
                                    valueRange = 0f..23f,
                                    steps = 22
                                )
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeader("More") }

        item {
            NavListRow(
                icon = Icons.Rounded.Tune,
                title = "Diagnostics",
                subtitle = "Health Connect & watch sync status",
                onClick = { lightTick(); onOpenDiagnostics() }
            )
        }

        item {
            NavListRow(
                icon = Icons.Rounded.Info,
                title = "About",
                subtitle = "Version, credits & source",
                onClick = { lightTick(); onOpenAbout() }
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 0.dp)
    )
}

@Composable
private fun SettingHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NavListRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
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
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickAddEditor(current: List<Int>, onSave: (List<Int>) -> Unit) {
    val padded = remember(current) {
        (current + listOf(0, 0, 0, 0)).take(4).toMutableList()
    }
    val values = remember { mutableStateOf(padded.map { it.toString() }) }
    val confirm = rememberConfirmTick()

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp)) {
            SettingHeader(
                icon = Icons.Rounded.Bolt,
                title = "Quick adds"
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "The four buttons on the Today screen, in millilitres.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in 0..3) {
                    OutlinedTextField(
                        value = values.value[i],
                        onValueChange = { v ->
                            val digits = v.filter { it.isDigit() }
                            values.value = values.value.toMutableList().also { it[i] = digits }
                        },
                        label = { Text("#${i + 1}") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    val nums = values.value.mapNotNull { it.toIntOrNull() }
                        .filter { it > 0 }
                    if (nums.isNotEmpty()) {
                        confirm()
                        onSave(nums)
                    }
                },
                shape = MaterialTheme.shapes.large
            ) { Text("Save") }
        }
    }
}
