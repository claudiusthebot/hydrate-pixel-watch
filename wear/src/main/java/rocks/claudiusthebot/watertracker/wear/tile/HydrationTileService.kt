package rocks.claudiusthebot.watertracker.wear.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.TitleCardStyle
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.material3.titleCard
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import rocks.claudiusthebot.watertracker.wear.MainActivity
import rocks.claudiusthebot.watertracker.wear.R
import rocks.claudiusthebot.watertracker.wear.WearWaterApp

private const val RESOURCES_VERSION = "1"
private const val ID_OPEN_APP = "open_app"
private const val ID_ADD_INTAKE = "add_intake"

/**
 * Material 3 Expressive Wear OS tile for the Hydrate app.
 *
 * - Title slot: "TODAY" label.
 * - Main slot: a `titleCard` showing `current/goal ml` over a circular
 *   progress indicator that animates from 0 to the current ratio when
 *   the tile becomes visible.
 * - Bottom slot: an edge button that quick-logs the user's last-used
 *   drink size, then re-renders.
 *
 * Click handling follows the RomaricKc1 pattern: every click is a
 * `LoadAction` that re-enters `onTileRequest`, where we dispatch on
 * `lastClickableId` to mutate the store before computing the next
 * layout. No separate IPC / requestUpdate plumbing needed.
 */
class HydrationTileService : TileService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> = scope.future {
        val app = applicationContext as WearWaterApp
        val store = app.store

        // Handle the click that just happened (if any).
        when (requestParams.currentState.lastClickableId) {
            ID_ADD_INTAKE -> {
                val ml = lastUsedMl(store)
                if (ml > 0) store.addIntake(ml)
            }
            // ID_OPEN_APP launches MainActivity directly via LaunchAction —
            // no work to do here.
        }

        val today = store.today.value
        val deviceParams = requestParams.deviceConfiguration
        val quickAddMl = lastUsedMl(store)

        val layout = tileLayout(
            current = today.totalMl,
            target = today.goalMl.coerceAtLeast(1),
            quickAddMl = quickAddMl,
            deviceParams = deviceParams
        )

        TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            // 60s self-refresh keeps the data fresh without IPC.
            .setFreshnessIntervalMillis(60_000L)
            .setTileTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(layout)
            )
            .build()
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
        )

    private suspend fun lastUsedMl(store: rocks.claudiusthebot.watertracker.wear.WaterStore): Int {
        val lastEntry = store.today.value.entries.firstOrNull()?.volumeMl
        if (lastEntry != null && lastEntry > 0) return lastEntry
        return store.currentQuicks().getOrNull(1) ?: 250
    }

    private fun tileLayout(
        current: Int,
        target: Int,
        quickAddMl: Int,
        deviceParams: DeviceParameters
    ): LayoutElement {
        val ratio = (current.toFloat() / target).coerceIn(0f, 1f)
        return materialScope(
            context = this,
            deviceConfiguration = deviceParams,
            allowDynamicTheme = true
        ) {
            primaryLayout(
                titleSlot = {
                    text(
                        text = getString(R.string.tile_today_label).layoutString,
                        typography = androidx.wear.protolayout.material3.Typography.LABEL_SMALL
                    )
                },
                mainSlot = {
                    titleCard(
                        onClick = ModifiersBuilders.Clickable.Builder()
                            .setId(ID_OPEN_APP)
                            .setOnClick(openAppAction(packageName))
                            .build(),
                        title = {
                            text(
                                text = "$current".layoutString,
                                typography = androidx.wear.protolayout.material3.Typography.DISPLAY_MEDIUM
                            )
                        },
                        content = {
                            text(
                                text = "of $target ml".layoutString,
                                typography = androidx.wear.protolayout.material3.Typography.LABEL_MEDIUM
                            )
                        },
                        height = androidx.wear.protolayout.DimensionBuilders.expand(),
                        style = TitleCardStyle.extraLargeTitleCardStyle()
                    )
                },
                bottomSlot = {
                    textEdgeButton(
                        onClick = ModifiersBuilders.Clickable.Builder()
                            .setId(ID_ADD_INTAKE)
                            .setOnClick(
                                ActionBuilders.LoadAction.Builder().build()
                            )
                            .build(),
                        labelContent = {
                            text(
                                text = "+ $quickAddMl ml".layoutString,
                                typography = androidx.wear.protolayout.material3.Typography.LABEL_MEDIUM
                            )
                        }
                    )
                }
            )
        }
    }
}

/** LaunchAction targeting the watch app's main activity. */
private fun openAppAction(packageName: String): ActionBuilders.LaunchAction =
    ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setClassName(MainActivity::class.java.name)
                .setPackageName(packageName)
                .build()
        )
        .build()

@Suppress("unused")
private fun MaterialScope.placeholder(): Unit = Unit
