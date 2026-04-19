package rocks.claudiusthebot.watertracker.shared

import kotlinx.serialization.Serializable

/**
 * A single water intake log. `id` is a stable client-generated id so we can
 * deduplicate across the wear/phone data-layer sync path. `volumeMl` is
 * always millilitres — the UI can display in oz etc but we store ml.
 */
@Serializable
data class WaterEntry(
    val id: String,
    val volumeMl: Int,
    /** Unix millis — wall-clock UTC. */
    val timestampMs: Long,
    /** Where it came from: "phone", "wear", or "import". */
    val source: String = "phone"
)

/** Daily aggregates used for history lists. */
@Serializable
data class DaySummary(
    /** ISO local date, e.g. "2026-04-19". */
    val date: String,
    val totalMl: Int,
    val goalMl: Int,
    val entries: List<WaterEntry> = emptyList()
)

/** User preferences persisted on each device (not synced). */
@Serializable
data class UserSettings(
    val dailyGoalMl: Int = 2000,
    val quickAddsMl: List<Int> = listOf(150, 250, 330, 500),
    /** Unit shown in the UI; intake is always stored ml regardless. */
    val displayUnit: DisplayUnit = DisplayUnit.ML
)

@Serializable
enum class DisplayUnit { ML, FL_OZ }

object SyncConstants {
    /** Wearable Data Layer message path for a new intake event. */
    const val PATH_INTAKE_ADD = "/water/intake/add"
    /** Wearable Data Layer message path for delete. */
    const val PATH_INTAKE_DELETE = "/water/intake/delete"
    /** Wearable Data Layer message path for "today's running total changed". */
    const val PATH_TOTAL_UPDATE = "/water/total/update"
    /** Capability for the handheld app — used by the watch to find a peer. */
    const val CAPABILITY_HANDHELD = "water_tracker_handheld"
    /** Capability for the wear app — used by the phone to find a peer. */
    const val CAPABILITY_WEAR = "water_tracker_wear"
}

object UnitConversion {
    private const val FL_OZ_TO_ML = 29.5735
    fun mlToFlOz(ml: Int): Double = ml / FL_OZ_TO_ML
    fun flOzToMl(oz: Double): Int = (oz * FL_OZ_TO_ML).toInt()
}
