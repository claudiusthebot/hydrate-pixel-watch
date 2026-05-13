package rocks.claudiusthebot.watertracker.phone

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import rocks.claudiusthebot.watertracker.phone.data.WaterRepository
import rocks.claudiusthebot.watertracker.phone.health.HealthConnectManager
import rocks.claudiusthebot.watertracker.phone.notif.HydrateNotifications
import rocks.claudiusthebot.watertracker.phone.notif.ReminderPrefs
import rocks.claudiusthebot.watertracker.phone.notif.ReminderWorker
import rocks.claudiusthebot.watertracker.phone.sync.WearSync
import rocks.claudiusthebot.watertracker.phone.widget.HydrationWidget
import androidx.glance.appwidget.updateAll

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

        // Register our wearable capability so the watch can discover us.
        scope.launch { WearSync(this@WaterApp).ensureLocalCapability() }

        // Mirror today's hydration state to any installed Glance widgets so the
        // home-screen tile stays in step with in-app, reminder, and watch-side
        // adds without the widget package needing a back-reference into the
        // repository.
        scope.launch {
            repo.today
                .distinctUntilChanged { a, b -> a.totalMl == b.totalMl && a.goalMl == b.goalMl }
                .collect {
                    runCatching { HydrationWidget().updateAll(this@WaterApp) }
                }
        }
        // Also refresh the widget when the user's quick-add buttons change so
        // the pill always shows the current "primary" volume.
        scope.launch {
            repo.settingsFlow
                .distinctUntilChanged { a, b -> a.quickAddsMl.firstOrNull() == b.quickAddsMl.firstOrNull() }
                .collect {
                    runCatching { HydrationWidget().updateAll(this@WaterApp) }
                }
        }
    }
}
