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
 * Pushes events + totals to any connected wear node. Fire-and-forget.
 */
class WearSync(private val context: Context) {

    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)

    /**
     * Register the handheld's capability at runtime as a belt-and-braces
     * alongside res/values/wear.xml. Idempotent, cheap to call repeatedly.
     */
    suspend fun ensureLocalCapability() {
        try {
            capabilityClient.addLocalCapability(SyncConstants.CAPABILITY_HANDHELD).await()
        } catch (e: Exception) {
            Log.w(TAG, "addLocalCapability failed", e)
        }
    }

    /** Returns true iff at least one watch node with our capability is reachable. */
    suspend fun isWearReachable(): Boolean = wearNodes().isNotEmpty()

    /** Returns the set of node ids that currently expose the wear capability. */
    suspend fun wearNodeIds(): List<String> = wearNodes()

    private suspend fun wearNodes(): List<String> = try {
        val caps = capabilityClient
            .getCapability(SyncConstants.CAPABILITY_WEAR, 1).await()
        caps.nodes.map { it.id }
    } catch (e: Exception) {
        Log.w(TAG, "getCapability failed, falling back to connectedNodes", e)
        try { nodeClient.connectedNodes.await().map { it.id } } catch (_: Exception) { emptyList() }
    }

    suspend fun pushIntakeAdd(entry: WaterEntry) {
        val payload = Json.encodeToString(entry).toByteArray(Charsets.UTF_8)
        for (nodeId in wearNodes()) {
            try {
                messageClient.sendMessage(nodeId, SyncConstants.PATH_INTAKE_ADD, payload).await()
            } catch (e: Exception) { Log.w(TAG, "intake send failed", e) }
        }
    }

    suspend fun pushTotalUpdate(totalMl: Int, goalMl: Int) {
        val payload = "$totalMl,$goalMl".toByteArray(Charsets.UTF_8)
        for (nodeId in wearNodes()) {
            try {
                messageClient.sendMessage(nodeId, SyncConstants.PATH_TOTAL_UPDATE, payload).await()
            } catch (e: Exception) { Log.w(TAG, "total send failed", e) }
        }
    }

    /** Push the full list of today's entries — watch uses this to reconcile. */
    suspend fun pushEntriesSync(entries: List<WaterEntry>) {
        val payload = Json.encodeToString(entries).toByteArray(Charsets.UTF_8)
        for (nodeId in wearNodes()) {
            try {
                messageClient.sendMessage(nodeId, SyncConstants.PATH_ENTRIES_SYNC, payload).await()
            } catch (e: Exception) { Log.w(TAG, "entries send failed", e) }
        }
    }

    companion object { private const val TAG = "WearSync" }
}
