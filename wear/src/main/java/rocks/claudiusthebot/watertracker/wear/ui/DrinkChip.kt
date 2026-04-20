package rocks.claudiusthebot.watertracker.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

/**
 * Full-width vertical drink chip — the watch equivalent of the phone's
 * drink tile. Big tap target, colored container derived from Material 3
 * Expressive, icon scales with volume.
 */
@Composable
fun DrinkChip(
    ml: Int,
    containerColor: Color,
    contentColor: Color,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tick = rememberWearTick()
    val (icon, iconSize) = drinkIconFor(ml)

    Card(
        onClick = { tick(); onPick(ml) },
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
            }
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "$ml ml",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = "+add",
                fontSize = 9.sp,
                color = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

private fun drinkIconFor(ml: Int): Pair<ImageVector, Dp> = when {
    ml <= 150  -> WaterIcons.Espresso to 20.dp
    ml <= 300  -> WaterIcons.Mug      to 22.dp
    ml <= 500  -> WaterIcons.Glass    to 24.dp
    ml <= 700  -> WaterIcons.Bottle   to 26.dp
    else       -> WaterIcons.Drop     to 28.dp
}
