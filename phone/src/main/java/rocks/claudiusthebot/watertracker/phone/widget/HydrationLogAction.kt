package rocks.claudiusthebot.watertracker.phone.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import rocks.claudiusthebot.watertracker.phone.WaterApp
import rocks.claudiusthebot.watertracker.phone.sync.WearSync

/**
 * Glance ActionCallback wired to the widget's quick-add button.
 *
 * Reads the volume from the [HydrationWidget.ParamMl] action parameter, writes
 * to Health Connect via the same path the in-app and reminder-notification
 * quick-adds use, and lets the repository's onTodayChanged hook trigger the
 * widget recomposition (no manual updateAll here).
 *
 * The launched scope is supervised + IO-dispatched so the broadcast-thread call
 * site returns immediately. We pull the value from the action parameters with
 * a sane default (250 ml) in case the widget XML drifts.
 */
class HydrationLogAction : ActionCallback {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val app = context.applicationContext as? WaterApp ?: return
        val ml = parameters[HydrationWidget.ParamMl] ?: DEFAULT_ML
        if (ml <= 0) return
        scope.launch {
            val entry = app.repo.addIntake(ml, source = "phone-widget")
            // Mirror the phone-side intake to the watch so both ends stay in sync.
            val sync = WearSync(context)
            sync.pushIntakeAdd(entry)
            val t = app.repo.today.value
            sync.pushTotalUpdate(t.totalMl, t.goalMl)
        }
    }

    companion object {
        private const val DEFAULT_ML = 250
    }
}
