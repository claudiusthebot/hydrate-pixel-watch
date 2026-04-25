package rocks.claudiusthebot.watertracker.wear.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState

/**
 * Rotary-dialed amount picker. 20 steps from 50 ml to 1000 ml in 50 ml
 * increments. Crown turns the wheel; tap the ✓ to commit.
 */
@Composable
fun CustomAmountDialog(
    initialMl: Int = 250,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val values = remember { (1..20).map { it * 50 } }
    val initialIndex = values.indexOf(
        values.minByOrNull { kotlin.math.abs(it - initialMl) } ?: 250
    ).coerceAtLeast(0)
    val pickerState = rememberPickerState(
        initialNumberOfOptions = values.size,
        initiallySelectedIndex = initialIndex
    )
    val tick = rememberWearTick()
    val confirm = rememberWearConfirm()

    val selectedMl = values[pickerState.selectedOptionIndex]

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Custom amount",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "$selectedMl ml",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))

            Picker(
                state = pickerState,
                contentDescription = { "Amount" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) { i ->
                Text(
                    text = "${values[i]}",
                    fontSize = 16.sp,
                    fontWeight = if (i == pickerState.selectedOptionIndex) FontWeight.Bold
                                 else FontWeight.Medium,
                    color = if (i == pickerState.selectedOptionIndex)
                                MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                FilledIconButton(
                    onClick = { tick(); onDismiss() },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(WaterIcons.Close, contentDescription = "Cancel")
                }
                FilledIconButton(
                    onClick = { confirm(); onConfirm(selectedMl) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(WaterIcons.Check, contentDescription = "Confirm")
                }
            }
        }
    }
}
