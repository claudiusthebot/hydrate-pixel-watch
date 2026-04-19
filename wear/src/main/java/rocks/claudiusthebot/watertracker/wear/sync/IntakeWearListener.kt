package rocks.claudiusthebot.watertracker.wear.sync

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import rocks.claudiusthebot.watertracker.shared.SyncConstants
import rocks.claudiusthebot.watertracker.shared.WaterEntry
import rocks.claudiusthebot.watertracker.wear.WearWaterApp

class IntakeWearListener : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(event: MessageEvent) {
        val app = applicationContext as? WearWaterApp ?: return
        when (event.path) {
            SyncConstants.PATH_INTAKE_ADD -> {
                val raw = event.data.toString(Charsets.UTF_8)
                val entry = try {
                    Json.decodeFromString<WaterEntry>(raw)
                } catch (e: Exception) {
                    Log.w(TAG, "bad payload", e); return
                }
                scope.launch {
                    // Mirror into wear Health Connect.
                    app.store.addIntakeFromPhone(entry.volumeMl)
                }
            }
            SyncConstants.PATH_TOTAL_UPDATE -> {
                val raw = event.data.toString(Charsets.UTF_8)
                val parts = raw.split(",").mapNotNull { it.toIntOrNull() }
                if (parts.size == 2) {
                    scope.launch { app.store.applyRemoteTotal(parts[0], parts[1]) }
                }
            }
        }
    }

    companion object { private const val TAG = "WearListener" }
}
