package rocks.claudiusthebot.watertracker.phone.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rocks.claudiusthebot.watertracker.shared.SyncConstants
import rocks.claudiusthebot.watertracker.shared.WaterEntry

/**
 * Push events to any connected wear nodes. Uses Google Play services Wearable
 * MessageClient — fire-and-forget, best-effort delivery.
 */
class WearSync(private val context: Context) {

    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    private suspend fun wearNodes(): List<String> = try {
        val caps = Wearable.getCapabilityClient(context)
            .getCapability(SyncConstants.CAPABILITY_WEAR, 1 /* FILTER_REACHABLE */).await()
        caps.nodes.map { it.id }
    } catch (e: Exception) {
        Log.w(TAG, "getCapability failed, falling back to all connected nodes", e)
        try { nodeClient.connectedNodes.await().map { it.id } } catch (_: Exception) { emptyList() }
    }

    suspend fun pushIntakeAdd(entry: WaterEntry) {
        val payload = Json.encodeToString(entry).toByteArray(Charsets.UTF_8)
        val nodes = wearNodes()
        for (nodeId in nodes) {
            try {
                messageClient.sendMessage(nodeId, SyncConstants.PATH_INTAKE_ADD, payload).await()
            } catch (e: Exception) {
                Log.w(TAG, "sendMessage to $nodeId failed", e)
            }
        }
    }

    suspend fun pushTotalUpdate(totalMl: Int, goalMl: Int) {
        val payload = "$totalMl,$goalMl".toByteArray(Charsets.UTF_8)
        val nodes = wearNodes()
        for (nodeId in nodes) {
            try {
                messageClient.sendMessage(nodeId, SyncConstants.PATH_TOTAL_UPDATE, payload).await()
            } catch (e: Exception) {
                Log.w(TAG, "sendMessage(total) to $nodeId failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "WearSync"
    }
}
