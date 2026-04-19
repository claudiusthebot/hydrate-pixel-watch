package rocks.claudiusthebot.watertracker.wear.health

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
 * Wear-side Health Connect wrapper. Wear OS 5+ ships native Health Connect on
 * the watch — same API as the phone.
 */
class WearHealthConnect(private val context: Context) {
    companion object {
        private const val TAG = "WearHC"
        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(HydrationRecord::class),
            HealthPermission.getWritePermission(HydrationRecord::class)
        )
    }

    enum class Availability { INSTALLED, NEEDS_UPDATE, NOT_SUPPORTED }

    fun availability(): Availability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> Availability.INSTALLED
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.NEEDS_UPDATE
        else -> Availability.NOT_SUPPORTED
    }

    private val client: HealthConnectClient? by lazy {
        if (availability() == Availability.INSTALLED) HealthConnectClient.getOrCreate(context) else null
    }

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(): Boolean {
        val c = client ?: return false
        return c.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
    }

    suspend fun writeHydration(volumeMl: Int, timestampMs: Long): String? {
        val c = client ?: return null
        val record = HydrationRecord(
            startTime = Instant.ofEpochMilli(timestampMs),
            endTime = Instant.ofEpochMilli(timestampMs + 1),
            startZoneOffset = null,
            endZoneOffset = null,
            volume = Volume.milliliters(volumeMl.toDouble())
        )
        return try {
            c.insertRecords(listOf(record)).recordIdsList.firstOrNull()
        } catch (e: Exception) { Log.e(TAG, "insert failed", e); null }
    }

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
                    source = r.metadata.dataOrigin.packageName.ifBlank { "wear" }
                )
            }
        } catch (e: Exception) { Log.e(TAG, "read failed", e); emptyList() }
    }
}
