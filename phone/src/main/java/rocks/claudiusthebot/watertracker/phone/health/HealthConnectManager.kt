package rocks.claudiusthebot.watertracker.phone.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import rocks.claudiusthebot.watertracker.shared.WaterEntry

/**
 * Thin wrapper around HealthConnectClient for hydration records. Handles
 * availability, permissions, read (for history), and write (for new intakes).
 *
 * Volumes go in/out as millilitres because the rest of the app uses ml; the
 * underlying Health Connect API is unit-agnostic.
 */
class HealthConnectManager(private val context: Context) {

    companion object {
        private const val TAG = "HealthConnect"

        /** All permissions our app needs — read + write hydration. */
        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(HydrationRecord::class),
            HealthPermission.getWritePermission(HydrationRecord::class)
        )
    }

    enum class Availability {
        INSTALLED, NEEDS_UPDATE, NOT_SUPPORTED
    }

    fun availability(): Availability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> Availability.INSTALLED
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.NEEDS_UPDATE
        else -> Availability.NOT_SUPPORTED
    }

    private val client: HealthConnectClient? by lazy {
        if (availability() == Availability.INSTALLED) {
            HealthConnectClient.getOrCreate(context)
        } else null
    }

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(): Boolean {
        val c = client ?: return false
        return c.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
    }

    /**
     * Write a hydration record for a single intake event. `startMs` is wall-clock
     * UTC — the record spans startMs..startMs (Health Connect tolerates 1ms spans).
     * Returns the Health Connect record id so we can reference it later.
     */
    suspend fun writeHydration(volumeMl: Int, timestampMs: Long): String? {
        val c = client ?: return null
        val start = Instant.ofEpochMilli(timestampMs)
        val end = Instant.ofEpochMilli(timestampMs + 1)
        val record = HydrationRecord(
            startTime = start,
            endTime = end,
            startZoneOffset = null,
            endZoneOffset = null,
            volume = Volume.milliliters(volumeMl.toDouble())
        )
        return try {
            val ids = c.insertRecords(listOf(record)).recordIdsList
            ids.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "insertRecords failed", e)
            null
        }
    }

    /** Delete a hydration record by Health Connect id. */
    suspend fun deleteHydration(hcId: String) {
        val c = client ?: return
        try {
            c.deleteRecords(HydrationRecord::class, listOf(hcId), emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "deleteRecords failed", e)
        }
    }

    /** Read hydration records for the given local date. */
    suspend fun readForDate(date: LocalDate): List<WaterEntry> {
        val c = client ?: return emptyList()
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val req = ReadRecordsRequest(
            recordType = HydrationRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return try {
            c.readRecords(req).records.map { r ->
                WaterEntry(
                    id = r.metadata.id,
                    volumeMl = r.volume.inMilliliters.toInt(),
                    timestampMs = r.startTime.toEpochMilli(),
                    source = r.metadata.dataOrigin.packageName.ifBlank { "phone" }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "readRecords failed", e)
            emptyList()
        }
    }
}
