package rocks.claudiusthebot.watertracker.phone.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.flow.first
import rocks.claudiusthebot.watertracker.phone.MainActivity
import rocks.claudiusthebot.watertracker.phone.R
import rocks.claudiusthebot.watertracker.phone.WaterApp
import rocks.claudiusthebot.watertracker.shared.DaySummary

/**
 * Hydrate home-screen widget.
 *
 * Layout (~4×2 cells minimum, resizable both axes):
 *   ┌──────────────────────────────┐
 *   │ 💧 Hydrate             62 % │
 *   │  1,250 ml / 2,000 ml         │
 *   │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
 *   │                  [ + 250 ml ]│
 *   └──────────────────────────────┘
 *
 * State snapshot is read at `provideGlance` time from [WaterApp.repo.today].
 * Updates are pushed by [WaterApp] collecting the same Flow and calling
 * `HydrationWidget().updateAll(context)` whenever it emits — that keeps this
 * widget package decoupled from the repository.
 *
 * The quick-add button volume defaults to the first entry in the user's
 * `quickAddsMl` setting (which is the natural "primary" slot the in-app UI
 * also exposes), falling back to 250 ml.
 */
class HydrationWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? WaterApp
        // The widget host can compose us before [WaterApp.onCreate] has run on
        // a cold-boot device — fall back to neutral defaults rather than NPE.
        val summary = app?.repo?.today?.value
            ?: DaySummary(date = "", totalMl = 0, goalMl = 2000)
        val quickAdd = app?.let {
            runCatching { it.repo.settingsFlow.first().quickAddsMl.firstOrNull() }
                .getOrNull()
        } ?: DEFAULT_QUICK_ADD

        provideContent {
            GlanceTheme {
                Content(summary = summary, quickAddMl = quickAdd)
            }
        }
    }

    @Composable
    private fun Content(summary: DaySummary, quickAddMl: Int) {
        val percent = if (summary.goalMl > 0) {
            ((summary.totalMl.toFloat() / summary.goalMl.toFloat()) * 100f)
                .coerceIn(0f, 9999f)
                .toInt()
        } else 0
        val openApp = actionStartActivity<MainActivity>()
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(20.dp)
                .padding(14.dp)
                .clickable(openApp),
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_water_drop),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                        modifier = GlanceModifier.size(18.dp),
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        text = "Hydrate",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        text = "$percent %",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    text = "${formatMl(summary.totalMl)} / ${formatMl(summary.goalMl)}",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(GlanceModifier.height(8.dp))
                ProgressBar(
                    fraction = if (summary.goalMl > 0) {
                        (summary.totalMl.toFloat() / summary.goalMl.toFloat())
                            .coerceIn(0f, 1f)
                    } else 0f,
                )
                Spacer(GlanceModifier.defaultWeight())
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuickAddPill(ml = quickAddMl)
                }
            }
        }
    }

    @Composable
    private fun ProgressBar(fraction: Float) {
        // Glance has no built-in progress component — render a rounded track
        // with a filled overlay. Width-based sizing is fine: the widget host
        // clamps bounds before composition, and the precise % is shown in the
        // header row, so we just need a visual indicator here.
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(6.dp)
                .cornerRadius(3.dp)
                .background(GlanceTheme.colors.surfaceVariant),
        ) {
            val widthDp = (fraction * NOMINAL_WIDTH_DP).toInt().coerceAtLeast(0)
            Box(
                modifier = GlanceModifier
                    .height(6.dp)
                    .width(widthDp.dp)
                    .cornerRadius(3.dp)
                    .background(GlanceTheme.colors.primary),
            ) {}
        }
    }

    @Composable
    private fun QuickAddPill(ml: Int) {
        Box(
            modifier = GlanceModifier
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(16.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clickable(
                    actionRunCallback<HydrationLogAction>(
                        actionParametersOf(ParamMl to ml)
                    )
                ),
        ) {
            Text(
                text = "+ ${formatMl(ml)}",
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }

    private fun formatMl(ml: Int): String =
        if (ml >= 1000) "%,d ml".format(ml) else "$ml ml"

    companion object {
        /**
         * Nominal body width used to size the filled portion of the progress
         * bar. Matches a typical 4-cell home-screen widget after padding.
         * The header-row percent is the authoritative readout — the bar is
         * decorative.
         */
        const val NOMINAL_WIDTH_DP: Int = 220

        const val DEFAULT_QUICK_ADD: Int = 250

        val ParamMl: ActionParameters.Key<Int> =
            ActionParameters.Key("widget.hydrate.ml")
    }
}

/**
 * Manifest hook — the framework instantiates this receiver, which in turn
 * names [HydrationWidget] as the actual Glance implementation.
 */
class HydrationWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HydrationWidget()
}
