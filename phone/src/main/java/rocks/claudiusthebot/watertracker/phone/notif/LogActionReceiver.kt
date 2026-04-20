package rocks.claudiusthebot.watertracker.phone.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import rocks.claudiusthebot.watertracker.phone.WaterApp
import rocks.claudiusthebot.watertracker.phone.sync.WearSync

/**
 * Handles the "Log 250ml" action from a reminder notification. Runs on the
 * main broadcast thread, so we bounce the HC write onto a scope.
 */
class LogActionReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != HydrateNotifications.ACTION_LOG) return
        val ml = intent.getIntExtra(HydrateNotifications.EXTRA_ML, 0)
        if (ml <= 0) return

        val app = context.applicationContext as? WaterApp ?: return
        scope.launch {
            val entry = app.repo.addIntake(ml, source = "phone")
            WearSync(context).pushIntakeAdd(entry)
            val t = app.repo.today.value
            WearSync(context).pushTotalUpdate(t.totalMl, t.goalMl)
            // dismiss the reminder after a successful log
            androidx.core.app.NotificationManagerCompat.from(context).cancel(1001)
        }
    }
}
