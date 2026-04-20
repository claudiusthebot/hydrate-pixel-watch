package rocks.claudiusthebot.watertracker.phone.notif

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.reminderStore by preferencesDataStore(name = "reminder_prefs")

/**
 * Reminder config — enabled flag, interval, quick-add size for the action,
 * quiet hours.
 */
class ReminderPrefs(private val context: Context) {

    data class Snapshot(
        val enabled: Boolean,
        val intervalMinutes: Int,
        val quickMl: Int,
        val quietStart: Int,
        val quietEnd: Int
    )

    private object K {
        val ENABLED = booleanPreferencesKey("enabled")
        val INTERVAL = intPreferencesKey("interval_minutes")
        val QUICK = intPreferencesKey("quick_ml")
        val QUIET_START = intPreferencesKey("quiet_start")
        val QUIET_END = intPreferencesKey("quiet_end")
    }

    val flow: Flow<Snapshot> = context.reminderStore.data.map {
        Snapshot(
            enabled = it[K.ENABLED] ?: true,
            intervalMinutes = it[K.INTERVAL] ?: 120,
            quickMl = it[K.QUICK] ?: 250,
            quietStart = it[K.QUIET_START] ?: 22,
            quietEnd = it[K.QUIET_END] ?: 7
        )
    }

    suspend fun snapshot(): Snapshot = flow.first()

    suspend fun setEnabled(v: Boolean) {
        context.reminderStore.edit { it[K.ENABLED] = v }
    }
    suspend fun setInterval(minutes: Int) {
        context.reminderStore.edit { it[K.INTERVAL] = minutes }
    }
    suspend fun setQuickMl(ml: Int) {
        context.reminderStore.edit { it[K.QUICK] = ml }
    }
    suspend fun setQuietStart(hour: Int) {
        context.reminderStore.edit { it[K.QUIET_START] = hour }
    }
    suspend fun setQuietEnd(hour: Int) {
        context.reminderStore.edit { it[K.QUIET_END] = hour }
    }
}
