package rocks.claudiusthebot.watertracker.phone.sync

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import rocks.claudiusthebot.watertracker.phone.WaterApp
import rocks.claudiusthebot.watertracker.shared.SyncConstants
import rocks.claudiusthebot.watertracker.shared.WaterEntry

class IntakeWearListener : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(event: MessageEvent) {
        val app = applicationContext as? WaterApp ?: return
        val sync = WearSync(this)
        when (event.path) {
            SyncConstants.PATH_INTAKE_ADD -> {
                val raw = event.data.toString(Charsets.UTF_8)
                val entry = try {
                    Json.decodeFromString<WaterEntry>(raw)
                } catch (e: Exception) {
                    Log.w(TAG, "bad intake payload", e); return
                }
                scope.launch {
                    app.repo.addIntake(entry.volumeMl, source = "wear")
                    val t = app.repo.today.value
                    sync.pushTotalUpdate(t.totalMl, t.goalMl)
                    sync.pushEntriesSync(t.entries)
                }
            }
            SyncConstants.PATH_REQUEST_REFRESH -> {
                scope.launch {
                    app.repo.refreshToday()
                    val t = app.repo.today.value
                    sync.pushTotalUpdate(t.totalMl, t.goalMl)
                    sync.pushEntriesSync(t.entries)
                }
            }
        }
    }

    companion object { private const val TAG = "IntakeWearListener" }
}
