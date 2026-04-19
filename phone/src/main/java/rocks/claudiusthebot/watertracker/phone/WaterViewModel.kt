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
import rocks.claudiusthebot.watertracker.phone.sync.WearSync
import rocks.claudiusthebot.watertracker.shared.DaySummary
import rocks.claudiusthebot.watertracker.shared.UserSettings
import rocks.claudiusthebot.watertracker.shared.WaterEntry
import java.time.LocalDate

class WaterViewModel(app: Application) : AndroidViewModel(app) {

    private val appCtx = getApplication<WaterApp>()
    private val repo: WaterRepository = appCtx.repo
    private val hc: HealthConnectManager = appCtx.hc
    private val wearSync = WearSync(app)

    private val _hcState = MutableStateFlow(HcState())
    val hcState: StateFlow<HcState> = _hcState.asStateFlow()

    val today: StateFlow<DaySummary> = repo.today
    val settings: StateFlow<UserSettings> = repo.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserSettings()
    )

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
                repo.refreshToday()
                val t = repo.today.value
                wearSync.pushTotalUpdate(t.totalMl, t.goalMl)
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
            val entry = repo.addIntake(ml, source = "phone")
            wearSync.pushIntakeAdd(entry)
            val t = repo.today.value
            wearSync.pushTotalUpdate(t.totalMl, t.goalMl)
        }
    }

    fun delete(entry: WaterEntry) {
        viewModelScope.launch {
            repo.delete(entry)
            val t = repo.today.value
            wearSync.pushTotalUpdate(t.totalMl, t.goalMl)
        }
    }

    fun setGoal(ml: Int) {
        viewModelScope.launch {
            repo.setGoal(ml)
            repo.refreshToday()
            val t = repo.today.value
            wearSync.pushTotalUpdate(t.totalMl, t.goalMl)
        }
    }

    fun setQuickAdds(quick: List<Int>) {
        viewModelScope.launch { repo.setQuickAdds(quick) }
    }

    suspend fun loadDate(date: LocalDate): DaySummary = repo.readDate(date)

    data class HcState(
        val availability: HealthConnectManager.Availability? = null,
        val hasPermissions: Boolean = false
    )
}
