package rocks.claudiusthebot.watertracker.phone.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Haptic helpers for stepped sliders.
 *
 * Compose's own `LocalHapticFeedback` only exposes `LongPress` / `TextHandleMove`
 * — nothing slider-ish. Dropping to the platform `View.performHapticFeedback`
 * gives us the real `SEGMENT_TICK` / `CLOCK_TICK` feel when dragging across a
 * stepped slider.
 *
 * Usage:
 * ```
 * val tick = rememberSegmentTick()
 * val confirm = rememberConfirmTick()
 * Slider(
 *     value = v,
 *     onValueChange = { new ->
 *         if (new.toInt() != v.toInt()) tick()
 *         v = new
 *     },
 *     onValueChangeFinished = { confirm(); save() },
 *     ...
 * )
 * ```
 */

/** Fires on each step change — `SEGMENT_TICK` on API 34+, `CLOCK_TICK` below. */
@Composable
fun rememberSegmentTick(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.SEGMENT_TICK
            } else {
                @Suppress("DEPRECATION")
                HapticFeedbackConstants.CLOCK_TICK
            }
            view.performHapticFeedback(type)
        }
    }
}

/** Stronger "confirm" tick when the user lets go of the slider. */
@Composable
fun rememberConfirmTick(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            view.performHapticFeedback(type)
        }
    }
}

