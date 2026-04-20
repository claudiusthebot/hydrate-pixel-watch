package rocks.claudiusthebot.watertracker.phone.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A slider-based custom amount picker. Uses a ModalBottomSheet with a visual
 * "glass" filling up as the slider moves — more expressive than a bare text
 * field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAmountSheet(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amount by remember { mutableStateOf(250f) }
    val tick = rememberSegmentTick()
    val confirm = rememberConfirmTick()
    // 19 steps across 50..1000 in 50-ml chunks → tick per 50ml.
    var lastStep by remember { mutableIntStateOf((amount / 50f).toInt()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Custom amount",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            GlassIllustration(
                fillFraction = (amount / 1000f).coerceIn(0.05f, 1f),
                modifier = Modifier.size(180.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "${amount.toInt()} ml",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(6.dp))

            Slider(
                value = amount,
                onValueChange = { new ->
                    val step = (new / 50f).toInt()
                    if (step != lastStep) {
                        lastStep = step
                        tick()
                    }
                    amount = new
                },
                onValueChangeFinished = { confirm() },
                valueRange = 50f..1000f,
                steps = 18,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(150, 250, 330, 500, 750).forEach { preset ->
                    OutlinedButton(
                        onClick = { amount = preset.toFloat() },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("$preset", fontSize = 10.sp)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
                Button(
                    onClick = { onConfirm(amount.toInt()) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) { Text("Add", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun GlassIllustration(fillFraction: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val glassW = size.width * 0.62f
        val glassH = size.height * 0.85f
        val topLeft = Offset(
            (size.width - glassW) / 2f,
            (size.height - glassH) / 2f
        )
        // Tapered glass path
        val leftTop = topLeft
        val rightTop = Offset(topLeft.x + glassW, topLeft.y)
        val leftBottom = Offset(topLeft.x + glassW * 0.13f, topLeft.y + glassH)
        val rightBottom = Offset(topLeft.x + glassW * 0.87f, topLeft.y + glassH)

        val glass = Path().apply {
            moveTo(leftTop.x, leftTop.y)
            lineTo(rightTop.x, rightTop.y)
            lineTo(rightBottom.x, rightBottom.y)
            lineTo(leftBottom.x, leftBottom.y)
            close()
        }

        // Outline
        drawPath(
            path = glass,
            color = Color(0xFF90CAF9),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
        )

        // Water
        val waterTop = topLeft.y + glassH * (1f - fillFraction)
        val waterClip = Path().apply {
            addRect(
                androidx.compose.ui.geometry.Rect(
                    Offset(topLeft.x - 4f, waterTop),
                    Size(glassW + 8f, glassH)
                )
            )
        }
        clipPath(waterClip) {
            drawPath(
                glass,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4FC3F7),
                        Color(0xFF0288D1),
                        Color(0xFF01579B)
                    ),
                    startY = waterTop,
                    endY = topLeft.y + glassH
                )
            )
        }

        // Shine
        drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = Offset(topLeft.x + glassW * 0.18f, topLeft.y + glassH * 0.15f),
            end = Offset(topLeft.x + glassW * 0.22f, topLeft.y + glassH * 0.7f),
            strokeWidth = 3.dp.toPx()
        )
    }
}
