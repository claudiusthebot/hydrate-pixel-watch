package rocks.claudiusthebot.watertracker.phone

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import rocks.claudiusthebot.watertracker.phone.data.WaterRepository
import rocks.claudiusthebot.watertracker.phone.health.HealthConnectManager
import rocks.claudiusthebot.watertracker.phone.notif.HydrateNotifications
import rocks.claudiusthebot.watertracker.phone.notif.ReminderPrefs
import rocks.claudiusthebot.watertracker.phone.notif.ReminderWorker

class WaterApp : Application() {
    lateinit var hc: HealthConnectManager
        private set
    lateinit var repo: WaterRepository
        private set
    lateinit var reminderPrefs: ReminderPrefs
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        hc = HealthConnectManager(this)
        repo = WaterRepository(this, hc)
        reminderPrefs = ReminderPrefs(this)

        HydrateNotifications.ensureChannels(this)

        // Apply the user's reminder config on boot — idempotent.
        scope.launch {
            val s = reminderPrefs.snapshot()
            if (s.enabled) ReminderWorker.schedule(this@WaterApp, s.intervalMinutes.toLong())
            else ReminderWorker.cancel(this@WaterApp)
        }
    }
}
