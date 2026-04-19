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

/**
 * Receives intake events from the watch via the Data Layer and mirrors them
 * into Health Connect on the phone side, so the phone DB stays in sync.
 */
class IntakeWearListener : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(event: MessageEvent) {
        val app = applicationContext as? WaterApp ?: return
        when (event.path) {
            SyncConstants.PATH_INTAKE_ADD -> {
                val raw = event.data.toString(Charsets.UTF_8)
                val entry = try {
                    Json.decodeFromString<WaterEntry>(raw)
                } catch (e: Exception) {
                    Log.w(TAG, "bad payload", e)
                    return
                }
                scope.launch {
                    app.repo.addIntake(entry.volumeMl, source = "wear")
                }
            }
            SyncConstants.PATH_INTAKE_DELETE -> {
                val id = event.data.toString(Charsets.UTF_8)
                scope.launch {
                    app.repo.readDate(java.time.LocalDate.now()).entries
                        .firstOrNull { it.id == id }
                        ?.let { app.repo.delete(it) }
                }
            }
        }
    }

    companion object {
        private const val TAG = "IntakeWearListener"
    }
}
