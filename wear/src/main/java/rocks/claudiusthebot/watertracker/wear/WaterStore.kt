package rocks.claudiusthebot.watertracker.wear

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import rocks.claudiusthebot.watertracker.shared.DaySummary
import rocks.claudiusthebot.watertracker.shared.WaterEntry
import rocks.claudiusthebot.watertracker.wear.health.WearHealthConnect
import rocks.claudiusthebot.watertracker.wear.ongoing.OngoingHydration
import rocks.claudiusthebot.watertracker.wear.sync.PhoneSync
import java.time.LocalDate
import java.util.UUID

private val Context.wearDs by preferencesDataStore(name = "wear_water")

class WaterStore(
    private val context: Context,
    private val hc: WearHealthConnect,
    private val sync: PhoneSync
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _today = MutableStateFlow(DaySummary(LocalDate.now().toString(), 0, 2000))
    val today: StateFlow<DaySummary> = _today.asStateFlow()

    private val _ready = MutableStateFlow(ReadyState())
    val ready: StateFlow<ReadyState> = _ready.asStateFlow()

    private object Keys {
        val GOAL = intPreferencesKey("goal_ml")
        val QUICK = stringPreferencesKey("quicks")
    }

    suspend fun currentGoal(): Int =
        context.wearDs.data.first()[Keys.GOAL] ?: 2000

    suspend fun currentQuicks(): List<Int> {
        val raw = context.wearDs.data.first()[Keys.QUICK]
        return raw?.split(",")?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(200, 300, 500)
    }

    suspend fun setGoal(ml: Int) {
        context.wearDs.edit { it[Keys.GOAL] = ml }
        refresh()
    }

    fun refreshAsync() { scope.launch { refresh() } }

    suspend fun refresh() {
        _ready.value = _ready.value.copy(
            availability = hc.availability(),
            hasPermissions = hc.hasAllPermissions()
        )
        if (_ready.value.hasPermissions) {
            val entries = hc.readForDate(LocalDate.now())
                .sortedByDescending { it.timestampMs }
            val goal = currentGoal()
            _today.value = DaySummary(
                date = LocalDate.now().toString(),
                totalMl = entries.sumOf { it.volumeMl },
                goalMl = goal,
                entries = entries
            )
            // Update the on-watch-face ongoing progress chip.
            OngoingHydration.update(context, _today.value.totalMl, _today.value.goalMl)
        }
    }

    /** Watch-initiated logging: write to HC, push to phone. */
    suspend fun addIntake(ml: Int) {
        if (ml <= 0) return
        val now = System.currentTimeMillis()
        val id = hc.writeHydration(ml, now) ?: UUID.randomUUID().toString()
        val entry = WaterEntry(id, ml, now, source = "wear")
        sync.pushIntakeAdd(entry)
        refresh()
    }

    /** Called by the listener when the phone already wrote to its HC — mirror to ours. */
    suspend fun addIntakeFromPhone(ml: Int) {
        if (ml <= 0) return
        hc.writeHydration(ml, System.currentTimeMillis())
        refresh()
    }

    /** Apply the phone's authoritative daily totals without needing HC read. */
    fun applyRemoteTotal(totalMl: Int, goalMl: Int) {
        _today.value = _today.value.copy(totalMl = totalMl, goalMl = goalMl)
    }

    data class ReadyState(
        val availability: WearHealthConnect.Availability? = null,
        val hasPermissions: Boolean = false
    )
}
