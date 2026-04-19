package rocks.claudiusthebot.watertracker.phone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import rocks.claudiusthebot.watertracker.phone.WaterViewModel

@Composable
fun SettingsScreen(vm: WaterViewModel) {
    val settings by vm.settings.collectAsState()

    var goalFloat by remember(settings) { mutableStateOf(settings.dailyGoalMl.toFloat()) }
    LaunchedEffect(settings.dailyGoalMl) { goalFloat = settings.dailyGoalMl.toFloat() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Daily goal", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${goalFloat.toInt()} ml",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = goalFloat,
                        onValueChange = { goalFloat = it },
                        onValueChangeFinished = { vm.setGoal(goalFloat.toInt()) },
                        valueRange = 1000f..4000f,
                        steps = 29
                    )
                    Text(
                        "Drag to pick anywhere from 1000 to 4000 ml.",
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
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("About", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Hydrate logs water intake to Health Connect on both phone and watch. Any device with Health Connect sees the same records.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddEditor(current: List<Int>, onSave: (List<Int>) -> Unit) {
    val padded = remember(current) {
        (current + listOf(0, 0, 0, 0)).take(4).toMutableList()
    }
    val values = remember { mutableStateOf(padded.map { it.toString() }) }

    Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Quick adds", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "The four buttons on the Today screen, in millilitres.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
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
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val nums = values.value.mapNotNull { it.toIntOrNull() }
                        .filter { it > 0 }
                    if (nums.isNotEmpty()) onSave(nums)
                },
                shape = MaterialTheme.shapes.medium
            ) { Text("Save") }
        }
    }
}
