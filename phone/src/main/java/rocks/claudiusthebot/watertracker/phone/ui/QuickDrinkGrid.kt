package rocks.claudiusthebot.watertracker.phone.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember

/**
 * Two-up grid of drink-size tiles. Replaces the v0.5 horizontal row that
 * was wrapping "250" into "25\n0" when the 4-button row got too narrow.
 *
 * Each tile is a Material 3 Expressive surface card with a sized drink
 * icon (cup/glass/bottle), the volume in big type, and "ml" beneath.
 * Flows to two rows on phone, collapses to one on large screens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickDrinkGrid(
    quickAdds: List<Int>,
    onPick: (Int) -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.padding(18.dp)) {
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
            Spacer(Modifier.height(12.dp))

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

            Spacer(Modifier.height(10.dp))

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

/** One drink-size tile — icon scaled by volume, huge number, ml label. */
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
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tileScale"
    )

    val (icon, iconSize) = drinkIconFor(ml)

    Surface(
        modifier = modifier
            .height(110.dp)
            .scale(scale)
            .clickable(interactionSource = source, indication = null) {
                tick()
                onPick(ml)
            },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Box(Modifier.fillMaxWidth()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(iconSize)
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "$ml",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "ml",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/** Pick an icon + size based on volume — visually communicates the pour size. */
private fun drinkIconFor(ml: Int): Pair<ImageVector, Dp> = when {
    ml <= 150  -> Icons.Rounded.LocalCafe  to 28.dp   // espresso
    ml <= 250  -> Icons.Rounded.LocalCafe  to 34.dp   // cup
    ml <= 400  -> Icons.Rounded.LocalDrink to 42.dp   // glass
    ml <= 600  -> Icons.Rounded.Opacity    to 48.dp   // bottle
    else       -> Icons.Rounded.WaterDrop  to 56.dp   // big bottle
}
