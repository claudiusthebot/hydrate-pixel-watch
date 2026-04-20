package rocks.claudiusthebot.watertracker.phone.notif

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Fires water-reminder notifications. Scheduled as a periodic WorkManager job
 * — default 120 min, configurable. Respects quiet hours.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = LocalTime.now()
        val app = applicationContext as? rocks.claudiusthebot.watertracker.phone.WaterApp
            ?: return Result.success()
        val prefs = app.reminderPrefs.snapshot()

        if (!prefs.enabled) return Result.success()
        if (inQuietHours(now, prefs.quietStart, prefs.quietEnd)) return Result.success()

        HydrateNotifications.showReminder(applicationContext, prefs.quickMl)
        return Result.success()
    }

    private fun inQuietHours(now: LocalTime, startH: Int, endH: Int): Boolean {
        val nowH = now.hour + now.minute / 60f
        return if (startH <= endH) nowH >= startH && nowH < endH
        else nowH >= startH || nowH < endH   // wrap around midnight
    }

    companion object {
        const val UNIQUE = "hydrate-reminder"

        fun schedule(context: Context, minutes: Long) {
            val req = PeriodicWorkRequestBuilder<ReminderWorker>(
                minutes.coerceAtLeast(15L),
                TimeUnit.MINUTES
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE,
                ExistingPeriodicWorkPolicy.UPDATE,
                req
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE)
        }
    }
}
