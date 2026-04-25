package rocks.claudiusthebot.watertracker.phone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rocks.claudiusthebot.watertracker.phone.data.WaterRepository
import rocks.claudiusthebot.watertracker.phone.health.HealthConnectManager
import rocks.claudiusthebot.watertracker.phone.notif.HydrateNotifications
import rocks.claudiusthebot.watertracker.phone.notif.ReminderPrefs
import rocks.claudiusthebot.watertracker.phone.notif.ReminderWorker
import rocks.claudiusthebot.watertracker.phone.sync.WearSync
import rocks.claudiusthebot.watertracker.shared.DaySummary
import rocks.claudiusthebot.watertracker.shared.UserSettings
import rocks.claudiusthebot.watertracker.shared.WaterEntry
import java.time.LocalDate

class WaterViewModel(app: Application) : AndroidViewModel(app) {

    private val appCtx = getApplication<WaterApp>()
    private val repo: WaterRepository = appCtx.repo
    private val hc: HealthConnectManager = appCtx.hc
    private val reminderPrefs: ReminderPrefs = appCtx.reminderPrefs
    private val wearSync = WearSync(app)

    private val _hcState = MutableStateFlow(HcState())
    val hcState: StateFlow<HcState> = _hcState.asStateFlow()

    val today: StateFlow<DaySummary> = repo.today
    val settings: StateFlow<UserSettings> = repo.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserSettings()
    )

    val reminders: StateFlow<ReminderPrefs.Snapshot> = reminderPrefs.flow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ReminderPrefs.Snapshot(true, 120, 250, 22, 7)
    )

    /**
     * `null` until DataStore loads, then `true`/`false`. UI keeps a splash
     * placeholder visible while it's null so we don't briefly flash the
     * onboarding to returning users.
     */
    val onboardingComplete: StateFlow<Boolean?> = repo.onboardingFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null
    )

    fun completeOnboarding() {
        viewModelScope.launch { repo.setOnboardingComplete() }
    }

    // Tracks goal-reached so we only celebrate once per day.
    private var celebratedForDate: String? = null

    // Records last write success/failure for diagnostics.
    private val _lastHcEvent = MutableStateFlow<String?>(null)
    val lastHcEvent: StateFlow<String?> = _lastHcEvent.asStateFlow()

    // Cached history for the History tab. First open kicks off a single
    // bulk Health Connect read; subsequent opens render instantly from
    // this flow while a quiet refresh runs in the background.
    private val _historyDays = MutableStateFlow<List<DaySummary>>(emptyList())
    val historyDays: StateFlow<List<DaySummary>> = _historyDays.asStateFlow()
    private val _historyLoading = MutableStateFlow(false)
    val historyLoading: StateFlow<Boolean> = _historyLoading.asStateFlow()
    private var historyLoadedOnce = false

    init {
        refresh()
    }

    fun refresh() {
        _hcState.value = _hcState.value.copy(
            availability = hc.availability()
        )
        viewModelScope.launch {
            _hcState.value = _hcState.value.copy(
                hasPermissions = hc.hasAllPermissions()
            )
            if (_hcState.value.hasPermissions) {
                try {
                    repo.refreshToday()
                    val t = repo.today.value
                    wearSync.pushTotalUpdate(t.totalMl, t.goalMl)
                    wearSync.pushEntriesSync(t.entries)
                    maybeCelebrate(t)
                    _lastHcEvent.value = "read ok (${t.entries.size} entries)"
                } catch (e: Exception) {
                    _lastHcEvent.value = "read failed: ${e.message}"
                }
            }
        }
    }

    fun onPermissionsResult() {
        viewModelScope.launch {
            _hcState.value = _hcState.value.copy(hasPermissions = hc.hasAllPermissions())
            if (_hcState.value.hasPermissions) repo.refreshToday()
        }
    }

    fun addIntake(ml: Int) {
        if (ml <= 0) return
        viewModelScope.launch {
            try {
                val entry = repo.addIntake(ml, source = "phone")
                wearSync.pushIntakeAdd(entry)
                val t = repo.today.value
                wearSync.pushTotalUpdate(t.totalMl, t.goalMl)
                wearSync.pushEntriesSync(t.entries)
                maybeCelebrate(t)
                _lastHcEvent.value = "wrote $ml ml ok"
                if (historyLoadedOnce) refreshHistory()
            } catch (e: Exception) {
                _lastHcEvent.value = "write failed: ${e.message}"
            }
        }
    }

    fun delete(entry: WaterEntry) {
        viewModelScope.launch {
            try {
                repo.delete(entry)
                val t = repo.today.value
                wearSync.pushTotalUpdate(t.totalMl, t.goalMl)
                wearSync.pushEntriesSync(t.entries)
                _lastHcEvent.value = "deleted ok"
                if (historyLoadedOnce) refreshHistory()
            } catch (e: Exception) {
                _lastHcEvent.value = "delete failed: ${e.message}"
            }
        }
    }

    fun setGoal(ml: Int) {
        viewModelScope.launch {
            repo.setGoal(ml)
            repo.refreshToday()
            val t = repo.today.value
            wearSync.pushTotalUpdate(t.totalMl, t.goalMl)
            if (historyLoadedOnce) refreshHistory()
        }
    }

    fun setQuickAdds(quick: List<Int>) {
        viewModelScope.launch { repo.setQuickAdds(quick) }
    }

    // --- Reminder config ------------------------------------------------

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            reminderPrefs.setEnabled(enabled)
            val s = reminderPrefs.snapshot()
            if (enabled) ReminderWorker.schedule(appCtx, s.intervalMinutes.toLong())
            else ReminderWorker.cancel(appCtx)
        }
    }

    fun setReminderInterval(min: Int) {
        viewModelScope.launch {
            reminderPrefs.setInterval(min)
            val s = reminderPrefs.snapshot()
            if (s.enabled) ReminderWorker.schedule(appCtx, s.intervalMinutes.toLong())
        }
    }

    fun setQuietStart(hour: Int) {
        viewModelScope.launch { reminderPrefs.setQuietStart(hour.coerceIn(0, 23)) }
    }

    fun setQuietEnd(hour: Int) {
        viewModelScope.launch { reminderPrefs.setQuietEnd(hour.coerceIn(0, 23)) }
    }

    suspend fun loadDate(date: LocalDate): DaySummary = repo.readDate(date)

    /**
     * Trigger a history refresh. Shows the spinner only on the first
     * load; subsequent calls update the cache silently in the
     * background so the UI keeps the previous data on screen and
     * avoids the "everything blank for a beat" flash.
     */
    fun refreshHistory(daysBack: Int = 30) {
        viewModelScope.launch {
            val showSpinner = !historyLoadedOnce
            if (showSpinner) _historyLoading.value = true
            try {
                _historyDays.value = repo.loadHistory(daysBack)
                historyLoadedOnce = true
            } catch (e: Exception) {
                _lastHcEvent.value = "history read failed: ${e.message}"
            } finally {
                if (showSpinner) _historyLoading.value = false
            }
        }
    }

    private fun maybeCelebrate(day: DaySummary) {
        if (day.goalMl <= 0) return
        val hit = day.totalMl >= day.goalMl
        if (hit && celebratedForDate != day.date) {
            celebratedForDate = day.date
            HydrateNotifications.showGoalReached(appCtx, day.totalMl)
        }
        if (!hit && celebratedForDate == day.date) {
            // Edge case: user deleted entries after celebration. Reset.
            celebratedForDate = null
        }
    }

    data class HcState(
        val availability: HealthConnectManager.Availability? = null,
        val hasPermissions: Boolean = false
    )
}
