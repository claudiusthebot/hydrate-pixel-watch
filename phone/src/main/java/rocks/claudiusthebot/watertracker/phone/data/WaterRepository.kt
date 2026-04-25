package rocks.claudiusthebot.watertracker.phone.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import rocks.claudiusthebot.watertracker.phone.health.HealthConnectManager
import rocks.claudiusthebot.watertracker.shared.DaySummary
import rocks.claudiusthebot.watertracker.shared.UserSettings
import rocks.claudiusthebot.watertracker.shared.WaterEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

private val Context.settingsStore by preferencesDataStore(name = "water_settings")

class WaterRepository(
    private val context: Context,
    private val hc: HealthConnectManager
) {
    private object Keys {
        val GOAL_ML = intPreferencesKey("goal_ml")
        val QUICK_ADDS = stringPreferencesKey("quick_adds")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    private val _today = MutableStateFlow(DaySummary(today(), 0, 2000))
    val today = _today.asStateFlow()

    val settingsFlow: Flow<UserSettings> = context.settingsStore.data.map { p ->
        val quickRaw = p[Keys.QUICK_ADDS]
        val quick = quickRaw?.split(",")?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(150, 250, 330, 500)
        UserSettings(
            dailyGoalMl = p[Keys.GOAL_ML] ?: 2000,
            quickAddsMl = quick
        )
    }

    suspend fun currentSettings(): UserSettings {
        val p = context.settingsStore.data.first()
        val quickRaw = p[Keys.QUICK_ADDS]
        val quick = quickRaw?.split(",")?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(150, 250, 330, 500)
        return UserSettings(
            dailyGoalMl = p[Keys.GOAL_ML] ?: 2000,
            quickAddsMl = quick
        )
    }

    suspend fun setGoal(ml: Int) {
        context.settingsStore.edit { it[Keys.GOAL_ML] = ml }
    }

    suspend fun setQuickAdds(quickAdds: List<Int>) {
        context.settingsStore.edit {
            it[Keys.QUICK_ADDS] = quickAdds.joinToString(",")
        }
    }

    /** Whether the first-launch onboarding has been completed. */
    val onboardingFlow: Flow<Boolean> = context.settingsStore.data.map {
        it[Keys.ONBOARDING_COMPLETE] ?: false
    }

    suspend fun setOnboardingComplete() {
        context.settingsStore.edit { it[Keys.ONBOARDING_COMPLETE] = true }
    }

    /** Write an intake to Health Connect and refresh today. */
    suspend fun addIntake(ml: Int, source: String = "phone"): WaterEntry {
        val now = System.currentTimeMillis()
        val hcId = hc.writeHydration(ml, now)
        val entry = WaterEntry(
            id = hcId ?: UUID.randomUUID().toString(),
            volumeMl = ml,
            timestampMs = now,
            source = source
        )
        refreshToday()
        return entry
    }

    /** Delete one entry and refresh. */
    suspend fun delete(entry: WaterEntry) {
        hc.deleteHydration(entry.id)
        refreshToday()
    }

    /** Pull today's records from Health Connect and update the state. */
    suspend fun refreshToday() {
        val date = LocalDate.now()
        val entries = hc.readForDate(date).sortedByDescending { it.timestampMs }
        val goal = currentSettings().dailyGoalMl
        _today.value = DaySummary(
            date = date.toString(),
            totalMl = entries.sumOf { it.volumeMl },
            goalMl = goal,
            entries = entries
        )
    }

    suspend fun readDate(date: LocalDate): DaySummary {
        val entries = hc.readForDate(date).sortedByDescending { it.timestampMs }
        val goal = currentSettings().dailyGoalMl
        return DaySummary(
            date = date.toString(),
            totalMl = entries.sumOf { it.volumeMl },
            goalMl = goal,
            entries = entries
        )
    }

    /**
     * Bulk-load the last `daysBack` days of history in a single Health
     * Connect IPC. Returns days newest-first.
     *
     * Replaces N sequential `readDate` calls — for daysBack=30 that's
     * one process-boundary read instead of thirty.
     */
    suspend fun loadHistory(daysBack: Int): List<DaySummary> {
        if (daysBack <= 0) return emptyList()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val rangeStart = today.minusDays((daysBack - 1).toLong())
            .atStartOfDay(zone).toInstant()
        val rangeEnd = today.plusDays(1).atStartOfDay(zone).toInstant()
        val all = hc.readRange(rangeStart, rangeEnd)
        val byLocalDate: Map<String, List<WaterEntry>> = all.groupBy { e ->
            Instant.ofEpochMilli(e.timestampMs).atZone(zone).toLocalDate().toString()
        }
        val goal = currentSettings().dailyGoalMl
        return (0 until daysBack).map { offset ->
            val date = today.minusDays(offset.toLong()).toString()
            val entries = (byLocalDate[date] ?: emptyList())
                .sortedByDescending { it.timestampMs }
            DaySummary(
                date = date,
                totalMl = entries.sumOf { it.volumeMl },
                goalMl = goal,
                entries = entries
            )
        }
    }

    private fun today(): String = LocalDate.now().toString()
}
