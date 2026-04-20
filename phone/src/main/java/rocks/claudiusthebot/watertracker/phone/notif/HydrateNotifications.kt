package rocks.claudiusthebot.watertracker.phone.notif

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import rocks.claudiusthebot.watertracker.phone.MainActivity
import rocks.claudiusthebot.watertracker.phone.R

/**
 * Notification helpers:
 *   • `showReminder` — "Time to drink water" nudge with a quick-log action.
 *   • `showGoalReached` — one-shot celebration when the daily goal hits 100%.
 *   • `showOngoing` — dismissible progress card while the app is active.
 */
object HydrateNotifications {
    const val CH_REMINDER = "hydrate_reminder"
    const val CH_ONGOING = "hydrate_ongoing"
    const val CH_CELEBRATE = "hydrate_celebrate"
    private const val ID_REMINDER = 1001
    private const val ID_ONGOING  = 1002
    private const val ID_CELEBRATE = 1003

    const val ACTION_LOG = "rocks.claudiusthebot.watertracker.LOG"
    const val EXTRA_ML = "ml"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannels(listOf(
            NotificationChannel(
                CH_REMINDER, "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Gentle nudges to drink water" },
            NotificationChannel(
                CH_ONGOING, "Ongoing progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) },
            NotificationChannel(
                CH_CELEBRATE, "Goal reached",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "One-shot celebration when you hit your daily goal" }
        ))
    }

    fun showReminder(context: Context, quickMl: Int = 250) {
        ensureChannels(context)
        val open = openIntent(context)

        val logIntent = Intent(context, LogActionReceiver::class.java).apply {
            action = ACTION_LOG
            putExtra(EXTRA_ML, quickMl)
        }
        val logPi = PendingIntent.getBroadcast(
            context, quickMl, logIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(context, CH_REMINDER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Time to hydrate 💧")
            .setContentText("Tap to log a glass. You got this.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, "Log $quickMl ml", logPi)
            .build()

        nm(context).notify(ID_REMINDER, notif)
    }

    fun showGoalReached(context: Context, totalMl: Int) {
        ensureChannels(context)
        val notif = NotificationCompat.Builder(context, CH_CELEBRATE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Goal reached! 🎉")
            .setContentText("$totalMl ml and counting — nice work.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$totalMl ml logged today. Staying hydrated lines your brain up for the rest of the day."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openIntent(context))
            .build()
        nm(context).notify(ID_CELEBRATE, notif)
    }

    fun showOngoing(context: Context, totalMl: Int, goalMl: Int) {
        ensureChannels(context)
        val pct = if (goalMl > 0) (totalMl * 100 / goalMl).coerceIn(0, 100) else 0
        val notif = NotificationCompat.Builder(context, CH_ONGOING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$totalMl / $goalMl ml")
            .setContentText("$pct% of today's goal")
            .setOngoing(false) // user can swipe away; it's informational
            .setOnlyAlertOnce(true)
            .setProgress(goalMl.coerceAtLeast(1), totalMl.coerceAtMost(goalMl), false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent(context))
            .build()
        nm(context).notify(ID_ONGOING, notif)
    }

    fun clearOngoing(context: Context) {
        nm(context).cancel(ID_ONGOING)
    }

    private fun nm(context: Context) =
        context.getSystemService(NotificationManager::class.java)!!

    private fun openIntent(context: Context): PendingIntent {
        val i = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context, 0, i,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
