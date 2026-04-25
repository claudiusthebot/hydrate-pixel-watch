package rocks.claudiusthebot.watertracker.phone.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember

/**
 * Two-up grid of drink-size tiles. Each tile is a vertical layout:
 * a contained icon chip on top, then the volume in big type, then "ml".
 * Icons live inside a fixed-size circular chip so they never clip the
 * tile's rounded corners.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickDrinkGrid(
    quickAdds: List<Int>,
    onPick: (Int) -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.LocalDrink,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    "Quick add",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(14.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2
            ) {
                quickAdds.take(4).forEach { ml ->
                    DrinkTile(
                        ml = ml,
                        modifier = Modifier.weight(1f),
                        onPick = onPick
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            FilledTonalButton(
                onClick = onCustom,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    "Custom amount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * One drink tile: icon chip + value + "ml". Vertical layout keeps the
 * icon fully inside the rounded surface, no matter the drink size.
 */
@Composable
private fun DrinkTile(
    ml: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tick = rememberSegmentTick()
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tileScale"
    )

    val icon = drinkIconFor(ml)

    Surface(
        modifier = modifier
            .height(124.dp)
            .scale(scale)
            .clickable(interactionSource = source, indication = null) {
                tick()
                onPick(ml)
            },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Icon chip — fixed 36dp circle, icon always 20dp inside it
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$ml",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = "ml",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

/** Icon hint based on drink size — communicates pour without changing layout. */
private fun drinkIconFor(ml: Int): ImageVector = when {
    ml <= 200  -> Icons.Rounded.LocalCafe
    ml <= 400  -> Icons.Rounded.LocalDrink
    ml <= 600  -> Icons.Rounded.Opacity
    else       -> Icons.Rounded.WaterDrop
}
