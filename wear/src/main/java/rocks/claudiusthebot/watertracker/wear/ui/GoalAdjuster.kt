package rocks.claudiusthebot.watertracker.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

/**
 * Tidy inline goal stepper — two icon buttons flanking the goal value.
 * Each tap adjusts by 250 ml; haptics handled by the caller.
 */
@Composable
fun GoalAdjuster(
    goalMl: Int,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tick = rememberWearTick()
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledIconButton(
            onClick = { tick(); onStep(-250) },
            modifier = Modifier.size(36.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors()
        ) {
            Icon(WaterIcons.Minus, contentDescription = "Decrease goal",
                modifier = Modifier.size(18.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Goal",
                fontSize = 8.sp,
                color = Color(0xFF6FB3E0),
                fontWeight = FontWeight.Medium
            )
            Text(
                "$goalMl ml",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        FilledIconButton(
            onClick = { tick(); onStep(250) },
            modifier = Modifier.size(36.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors()
        ) {
            Icon(WaterIcons.Plus, contentDescription = "Increase goal",
                modifier = Modifier.size(18.dp))
        }
    }
}
