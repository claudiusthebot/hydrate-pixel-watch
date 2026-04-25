package rocks.claudiusthebot.watertracker.phone.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Haptic helpers used across the phone UI. We bypass Compose's
 * `LocalHapticFeedback` (which only exposes LongPress / TextHandleMove)
 * and call the platform `View.performHapticFeedback` so we can pick the
 * right effect per interaction (segment tick, confirm, toggle, etc.).
 */

/** Slider step crossing — `SEGMENT_TICK` on API 34+, `CLOCK_TICK` below. */
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

/** Stronger "confirm" tick for slider release / commit. */
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

/** A satisfying single tap — used for primary actions (add a drink, confirm a sheet). */
@Composable
fun rememberTapHaptic(): () -> Unit {
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

/** Light tick — secondary actions (nav rows, toggle off). */
@Composable
fun rememberLightTick(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONTEXT_CLICK
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            view.performHapticFeedback(type)
        }
    }
}

/** Toggle on — distinctive click. */
@Composable
fun rememberToggleOnHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.TOGGLE_ON
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            view.performHapticFeedback(type)
        }
    }
}

/** Toggle off — softer click. */
@Composable
fun rememberToggleOffHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.TOGGLE_OFF
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            view.performHapticFeedback(type)
        }
    }
}

/** Reject / destructive — used for delete. */
@Composable
fun rememberRejectHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            view.performHapticFeedback(type)
        }
    }
}
