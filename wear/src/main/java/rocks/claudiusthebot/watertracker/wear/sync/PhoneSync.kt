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

    private suspend fun phoneNodes(): List<String> = try {
        val caps = Wearable.getCapabilityClient(context)
            .getCapability(SyncConstants.CAPABILITY_HANDHELD, 1).await()
        caps.nodes.map { it.id }
    } catch (e: Exception) {
        Log.w(TAG, "no handheld capability, falling back to connectedNodes", e)
        try { nodeClient.connectedNodes.await().map { it.id } } catch (_: Exception) { emptyList() }
    }

    suspend fun pushIntakeAdd(entry: WaterEntry) {
        val payload = Json.encodeToString(entry).toByteArray(Charsets.UTF_8)
        for (nodeId in phoneNodes()) {
            try {
                messageClient.sendMessage(nodeId, SyncConstants.PATH_INTAKE_ADD, payload).await()
            } catch (e: Exception) {
                Log.w(TAG, "sendMessage failed to $nodeId", e)
            }
        }
    }

    companion object { private const val TAG = "PhoneSync" }
}
