package rocks.claudiusthebot.watertracker.wear.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Wear-side haptic helpers — same pattern as the phone side but tuned for
 * watch-scale feedback. Uses platform `View.performHapticFeedback` because
 * Compose's `LocalHapticFeedback` doesn't expose tick/confirm types.
 */

/** Fires a "segment tick" — subtle click used for stepped / quick actions. */
@Composable
fun rememberWearTick(): () -> Unit {
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

/** Stronger confirmation buzz — EdgeButton press, goal commit, celebration. */
@Composable
fun rememberWearConfirm(): () -> Unit {
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

/** Celebration — "Heavy Click" when goal first hits 100%. */
@Composable
fun rememberWearCelebrate(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.GESTURE_END
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            view.performHapticFeedback(type)
        }
    }
}
