package rocks.claudiusthebot.watertracker.wear

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rocks.claudiusthebot.watertracker.shared.DaySummary
import rocks.claudiusthebot.watertracker.shared.WaterEntry
import rocks.claudiusthebot.watertracker.wear.ongoing.OngoingHydration
import rocks.claudiusthebot.watertracker.wear.sync.PhoneSync
import rocks.claudiusthebot.watertracker.wear.tile.HydrationTileService
import androidx.wear.tiles.TileService
import java.time.LocalDate
import java.util.UUID

private val Context.wearDs by preferencesDataStore(name = "wear_water")

/**
 * Watch-side store. **No Health Connect dependency.** The phone is the single
 * source of truth; we cache today's data locally so the UI is always populated
 * and log new intakes optimistically, pushing them to the phone via Data Layer.
 *
 * Flow:
 *   • User taps +250ml on watch → optimistic cache update + PhoneSync send
 *   • Phone writes to Health Connect → pushes back `total_update` + `entries_sync`
 *   • Cache syncs with phone's source of truth
 */
class WaterStore(
    private val context: Context,
    private val sync: PhoneSync
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _today = MutableStateFlow(DaySummary(LocalDate.now().toString(), 0, 2000))
    val today: StateFlow<DaySummary> = _today.asStateFlow()

    private val _connection = MutableStateFlow(ConnectionState())
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    data class ConnectionState(
        /** Has the watch seen at least one message from the phone since launch? */
        val phoneReachable: Boolean = false,
        /** Wall-clock millis of the last successful push TO the phone. */
        val lastSyncMs: Long = 0L,
        /** Count of intakes logged locally that haven't made the round trip yet. */
        val pendingSync: Int = 0
    )

    private object Keys {
        val GOAL = intPreferencesKey("goal_ml")
        val QUICK = stringPreferencesKey("quicks")
        val CACHED_TOTAL = intPreferencesKey("cached_total")
        val CACHED_DATE = stringPreferencesKey("cached_date")
        val CACHED_ENTRIES = stringPreferencesKey("cached_entries")
        val LAST_SEEN_PHONE = longPreferencesKey("last_seen_phone")
    }

    init {
        scope.launch { loadCache() }
    }

    // ---- cache ----------------------------------------------------------

    private suspend fun loadCache() {
        val p = context.wearDs.data.first()
        val cachedDate = p[Keys.CACHED_DATE]
        val today = LocalDate.now().toString()
        if (cachedDate == today) {
            val entriesRaw = p[Keys.CACHED_ENTRIES]
            val entries = try {
                entriesRaw?.let { Json.decodeFromString<List<WaterEntry>>(it) } ?: emptyList()
            } catch (_: Exception) { emptyList() }
            _today.value = DaySummary(
                date = today,
                totalMl = p[Keys.CACHED_TOTAL] ?: 0,
                goalMl = p[Keys.GOAL] ?: 2000,
                entries = entries
            )
        } else {
            // Date rolled over — start fresh.
            _today.value = DaySummary(today, 0, p[Keys.GOAL] ?: 2000)
        }
        _connection.value = _connection.value.copy(
            lastSyncMs = p[Keys.LAST_SEEN_PHONE] ?: 0L,
            phoneReachable = (p[Keys.LAST_SEEN_PHONE] ?: 0L) > 0L
        )
        OngoingHydration.update(context, _today.value.totalMl, _today.value.goalMl)
        nudgeTile()
    }

    private suspend fun saveCache() {
        val t = _today.value
        context.wearDs.edit {
            it[Keys.CACHED_TOTAL] = t.totalMl
            it[Keys.CACHED_DATE] = t.date
            it[Keys.CACHED_ENTRIES] = Json.encodeToString(t.entries)
        }
    }

    // ---- settings -------------------------------------------------------

    suspend fun currentGoal(): Int = context.wearDs.data.first()[Keys.GOAL] ?: 2000
    suspend fun currentQuicks(): List<Int> {
        val raw = context.wearDs.data.first()[Keys.QUICK]
        return raw?.split(",")?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.isNotEmpty() } ?: listOf(200, 300, 500)
    }

    suspend fun setGoal(ml: Int) {
        context.wearDs.edit { it[Keys.GOAL] = ml }
        _today.value = _today.value.copy(goalMl = ml)
        OngoingHydration.update(context, _today.value.totalMl, _today.value.goalMl)
        nudgeTile()
        saveCache()
    }

    // ---- intake ---------------------------------------------------------

    /**
     * Watch-initiated logging. Optimistically updates the cache + notif and
     * pushes the event to the phone. If the phone isn't reachable right now,
     * the event still shows locally; next successful `entries_sync` from the
     * phone will reconcile.
     */
    suspend fun addIntake(ml: Int) {
        if (ml <= 0) return
        val entry = WaterEntry(
            id = UUID.randomUUID().toString(),
            volumeMl = ml,
            timestampMs = System.currentTimeMillis(),
            source = "wear"
        )

        // Optimistic local update
        val prev = _today.value
        val newEntries = (listOf(entry) + prev.entries).take(30)
        _today.value = prev.copy(
            totalMl = prev.totalMl + ml,
            entries = newEntries
        )
        _connection.value = _connection.value.copy(
            pendingSync = _connection.value.pendingSync + 1
        )
        saveCache()
        OngoingHydration.update(context, _today.value.totalMl, _today.value.goalMl)
        nudgeTile()

        // Push to phone
        val ok = sync.pushIntakeAdd(entry)
        if (ok) {
            _connection.value = _connection.value.copy(
                pendingSync = (_connection.value.pendingSync - 1).coerceAtLeast(0),
                lastSyncMs = System.currentTimeMillis()
            )
        }
    }

    // ---- phone-initiated updates ----------------------------------------

    /**
     * Called by the listener when the phone pushes `/water/total/update`.
     * Authoritative total from the phone's Health Connect — overrides
     * anything optimistic.
     */
    suspend fun applyRemoteTotal(totalMl: Int, goalMl: Int) {
        _today.value = _today.value.copy(totalMl = totalMl, goalMl = goalMl)
        _connection.value = _connection.value.copy(
            phoneReachable = true,
            lastSyncMs = System.currentTimeMillis(),
            pendingSync = 0
        )
        saveGoalIfDiff(goalMl)
        saveCache()
        context.wearDs.edit {
            it[Keys.LAST_SEEN_PHONE] = System.currentTimeMillis()
        }
        OngoingHydration.update(context, _today.value.totalMl, _today.value.goalMl)
        nudgeTile()
    }

    /**
     * Called when the phone pushes the full entries list `/water/entries/sync`.
     * Replaces the local cache with the phone's truth.
     */
    suspend fun applyRemoteEntries(entries: List<WaterEntry>) {
        val total = entries.sumOf { it.volumeMl }
        val sorted = entries.sortedByDescending { it.timestampMs }
        _today.value = _today.value.copy(
            totalMl = total,
            entries = sorted
        )
        _connection.value = _connection.value.copy(
            phoneReachable = true,
            lastSyncMs = System.currentTimeMillis(),
            pendingSync = 0
        )
        saveCache()
        OngoingHydration.update(context, _today.value.totalMl, _today.value.goalMl)
        nudgeTile()
    }

    private suspend fun saveGoalIfDiff(goalMl: Int) {
        val existing = currentGoal()
        if (existing != goalMl) context.wearDs.edit { it[Keys.GOAL] = goalMl }
    }

    /** Kick a manual resync request to the phone. */
    fun requestRefresh() {
        scope.launch { sync.requestRefresh() }
    }

    fun refreshAsync() { scope.launch { loadCache() } }

    suspend fun refresh() = loadCache()

    /**
     * Ask the system to re-render the Hydrate tile. Cheap — the tile
     * service re-runs its layout build which reads from this store.
     * No-op on hosts that don't support tiles. Wrapped in try/catch
     * because some emulators throw NPE in the updater plumbing.
     */
    private fun nudgeTile() {
        try {
            TileService.getUpdater(context)
                .requestUpdate(HydrationTileService::class.java)
        } catch (_: Throwable) { /* ignore */ }
    }
}
