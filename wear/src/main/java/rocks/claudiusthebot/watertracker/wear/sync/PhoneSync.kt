package rocks.claudiusthebot.watertracker.wear.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rocks.claudiusthebot.watertracker.shared.SyncConstants
import rocks.claudiusthebot.watertracker.shared.WaterEntry

class PhoneSync(private val context: Context) {

    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)

    private suspend fun phoneNodes(): List<String> = try {
        val caps = capabilityClient
            .getCapability(SyncConstants.CAPABILITY_HANDHELD, 1).await()
        caps.nodes.map { it.id }
    } catch (e: Exception) {
        try { nodeClient.connectedNodes.await().map { it.id } } catch (_: Exception) { emptyList() }
    }

    /** Returns true iff the message was acknowledged by at least one node. */
    suspend fun pushIntakeAdd(entry: WaterEntry): Boolean {
        val payload = Json.encodeToString(entry).toByteArray(Charsets.UTF_8)
        val nodes = phoneNodes()
        if (nodes.isEmpty()) {
            Log.w(TAG, "no phone nodes reachable for intake")
            return false
        }
        var anyOk = false
        for (nodeId in nodes) {
            try {
                messageClient.sendMessage(nodeId, SyncConstants.PATH_INTAKE_ADD, payload).await()
                anyOk = true
            } catch (e: Exception) {
                Log.w(TAG, "sendMessage to $nodeId failed", e)
            }
        }
        return anyOk
    }

    /** Ask the phone for a fresh total + entries list. */
    suspend fun requestRefresh(): Boolean {
        val nodes = phoneNodes()
        if (nodes.isEmpty()) return false
        var anyOk = false
        for (nodeId in nodes) {
            try {
                messageClient.sendMessage(
                    nodeId, SyncConstants.PATH_REQUEST_REFRESH, ByteArray(0)
                ).await()
                anyOk = true
            } catch (e: Exception) {
                Log.w(TAG, "requestRefresh to $nodeId failed", e)
            }
        }
        return anyOk
    }

    companion object { private const val TAG = "PhoneSync" }
}
