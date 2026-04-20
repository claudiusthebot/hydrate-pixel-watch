package rocks.claudiusthebot.watertracker.wear.ongoing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import rocks.claudiusthebot.watertracker.wear.MainActivity
import rocks.claudiusthebot.watertracker.wear.R

/**
 * Publishes an ongoing-activity status chip on the watch face showing today's
 * hydration progress. The chip opens the app when tapped.
 */
object OngoingHydration {
    private const val CHANNEL = "hydrate_ongoing"
    private const val NOTIF_ID = 9001

    fun update(context: Context, totalMl: Int, goalMl: Int) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL,
                        "Hydrate progress",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        setShowBadge(false)
                    }
                )
            }
        }

        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pct = if (goalMl > 0) ((totalMl.toFloat() / goalMl) * 100f).toInt().coerceIn(0, 100) else 0

        val notif = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$totalMl / $goalMl ml")
            .setContentText("$pct% of goal")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(goalMl.coerceAtLeast(1), totalMl.coerceAtMost(goalMl), false)
            .setContentIntent(pi)
            .setCategory(NotificationCompat.CATEGORY_STATUS)

        val status = Status.Builder()
            .addTemplate("$totalMl / $goalMl ml ($pct%)")
            .build()

        OngoingActivity.Builder(context, NOTIF_ID, notif)
            .setStaticIcon(R.mipmap.ic_launcher)
            .setTouchIntent(pi)
            .setStatus(status)
            .build()
            .apply(context)

        nm.notify(NOTIF_ID, notif.build())
    }

    fun clear(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.cancel(NOTIF_ID)
    }
}
