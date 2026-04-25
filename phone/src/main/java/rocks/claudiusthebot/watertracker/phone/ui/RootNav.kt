package rocks.claudiusthebot.watertracker.phone.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import rocks.claudiusthebot.watertracker.phone.WaterViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private object Routes {
    const val TODAY = "today"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"
}

private data class TabDest(
    val route: String,
    val label: String,
    val outlined: ImageVector,
    val filled: ImageVector
)

private val TABS = listOf(
    TabDest(Routes.TODAY, "Today",
        outlined = Icons.Outlined.WaterDrop, filled = Icons.Filled.WaterDrop),
    TabDest(Routes.HISTORY, "History",
        outlined = Icons.Outlined.BarChart, filled = Icons.Filled.BarChart),
    TabDest(Routes.SETTINGS, "Settings",
        outlined = Icons.Outlined.Settings, filled = Icons.Filled.Settings),
)

// Push/pop motion modeled on PixelPlayer: incoming slides in from the
// right while the outgoing slides part-way left and fades. Pop reverses
// it and adds a subtle scale-out on the leaving screen for depth.
private const val NAV_DURATION_MS = 350
private val NAV_EASING = FastOutSlowInEasing

/**
 * Bottom inset that screens should add to their LazyColumn contentPadding
 * so the last items can scroll past the floating nav pill without getting
 * permanently hidden behind it.
 */
val LocalFloatingNavInset = compositionLocalOf { 0.dp }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RootNav(vm: WaterViewModel) {
    val nav = rememberNavController()
    val backEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val today = remember { LocalDate.now() }
    val dateStr = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
    }

    val isSubScreen = currentRoute == Routes.DIAGNOSTICS

    val systemBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Toolbar visible height (~56dp button + small slack) + top/bottom margins
    // around the pill + system bars padding so content can scroll past.
    val floatingNavInset: Dp = 64.dp + 12.dp + 12.dp + systemBarsBottom

    CompositionLocalProvider(LocalFloatingNavInset provides floatingNavInset) {
    Box(Modifier.fillMaxSize()) {
        Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (currentRoute) {
                                Routes.TODAY -> "Hydrate"
                                Routes.HISTORY -> "History"
                                Routes.SETTINGS -> "Settings"
                                Routes.DIAGNOSTICS -> "Diagnostics"
                                else -> "Hydrate"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (currentRoute == Routes.TODAY) {
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (isSubScreen) {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
    ) { inner ->
        val floatTween = tween<Float>(NAV_DURATION_MS, easing = NAV_EASING)
        val intTween = tween<androidx.compose.ui.unit.IntOffset>(NAV_DURATION_MS, easing = NAV_EASING)

        // Two distinct motion patterns:
        //
        // (A) Peer tab nav (Today ↔ History ↔ Settings) — both screens
        //     slide horizontally based on tab order. Direction comes
        //     from `routeOrder` so we work the same regardless of
        //     whether Navigation calls it a push or a pop.
        //
        // (B) Parent ↔ child nav (Settings ↔ Diagnostics) — proper
        //     stack motion: pushing the child slides it over the parent
        //     from the right; popping back slides the child off to the
        //     right while the parent stays put with a small fade+scale
        //     reveal. This is what predictive-back lerps along the
        //     swipe gesture, so on Android 14+ the back gesture and
        //     the in-app `popBackStack()` use the same animation.
        val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition = {
            val from = initialState.destination.route
            val to = targetState.destination.route
            if (isSubScreenNav(from, to)) {
                if (routeOrder(to) > routeOrder(from)) {
                    // Forward: child slides in from the right.
                    slideInHorizontally(animationSpec = intTween) { full -> full } +
                        fadeIn(animationSpec = floatTween)
                } else {
                    // Back: parent was beneath, fades+scales in place.
                    fadeIn(animationSpec = floatTween) +
                        scaleIn(initialScale = 0.96f, animationSpec = floatTween)
                }
            } else {
                val dir = navDirection(from, to)
                slideInHorizontally(animationSpec = intTween) { full -> dir * full } +
                    fadeIn(animationSpec = floatTween)
            }
        }
        val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition = {
            val from = initialState.destination.route
            val to = targetState.destination.route
            if (isSubScreenNav(from, to)) {
                if (routeOrder(to) > routeOrder(from)) {
                    // Forward: parent stays put underneath, soft fade.
                    fadeOut(animationSpec = floatTween) +
                        scaleOut(targetScale = 0.96f, animationSpec = floatTween)
                } else {
                    // Back: child slides full off to the right.
                    slideOutHorizontally(animationSpec = intTween) { full -> full } +
                        fadeOut(animationSpec = floatTween)
                }
            } else {
                val dir = navDirection(from, to)
                slideOutHorizontally(animationSpec = intTween) { full -> -dir * full / 3 } +
                    fadeOut(animationSpec = floatTween)
            }
        }

        NavHost(
            navController = nav,
            startDestination = Routes.TODAY,
            modifier = Modifier.fillMaxSize().padding(top = inner.calculateTopPadding()),
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = enter,
            popExitTransition = exit
        ) {
            composable(Routes.TODAY) { ScreenSurface { TodayScreen(vm) } }
            composable(Routes.HISTORY) { ScreenSurface { HistoryScreen(vm) } }
            composable(Routes.SETTINGS) {
                ScreenSurface {
                    SettingsScreen(
                        vm = vm,
                        onOpenDiagnostics = { nav.navigate(Routes.DIAGNOSTICS) }
                    )
                }
            }
            composable(Routes.DIAGNOSTICS) {
                ScreenSurface { DiagnosticsScreen(vm) }
            }
        }
    }

    // Floating nav overlay — sits ABOVE content (which scrolls behind it)
    // and ABOVE the system gesture bar via navigationBars insets.
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(bottom = systemBarsBottom + 12.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                toolbarContainerColor = MaterialTheme.colorScheme.primaryContainer,
                toolbarContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.zIndex(1f)
        ) {
            TABS.forEach { dest ->
                val selected = currentRoute == dest.route ||
                    (dest.route == Routes.SETTINGS &&
                        currentRoute == Routes.DIAGNOSTICS)
                ToggleButton(
                    checked = selected,
                    onCheckedChange = {
                        if (!selected) {
                            nav.navigate(dest.route) {
                                popUpTo(nav.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shapes = ToggleButtonDefaults.shapes(
                        CircleShape, CircleShape, CircleShape
                    ),
                    modifier = Modifier.height(56.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Crossfade(targetState = selected, label = "navIcon") { sel ->
                            Icon(
                                if (sel) dest.filled else dest.outlined,
                                contentDescription = dest.label
                            )
                        }
                        AnimatedVisibility(
                            visible = selected,
                            enter = expandHorizontally(),
                            exit = shrinkHorizontally()
                        ) {
                            Text(
                                text = dest.label,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier.padding(
                                    start = ButtonDefaults.IconSpacing
                                )
                            )
                        }
                    }
                }
            }
        }
    }
    }
    }
}

/**
 * Opaque background per destination so the leaving screen never bleeds
 * through during the slide+fade.
 */
@Composable
private fun ScreenSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        content = { content() }
    )
}


private fun routeOrder(route: String?): Int = when (route) {
    Routes.TODAY -> 0
    Routes.HISTORY -> 1
    Routes.SETTINGS -> 2
    Routes.DIAGNOSTICS -> 3   // sub-screen, sits "to the right" of Settings
    else -> 0
}

private val PEER_TAB_ROUTES = setOf(Routes.TODAY, Routes.HISTORY, Routes.SETTINGS)

/** True if either side of the transition is a sub-screen (Diagnostics). */
private fun isSubScreenNav(from: String?, to: String?): Boolean =
    from !in PEER_TAB_ROUTES || to !in PEER_TAB_ROUTES

/** +1 → entering screen slides in from the right; -1 → from the left. */
private fun navDirection(from: String?, to: String?): Int {
    val a = routeOrder(from); val b = routeOrder(to)
    return if (b >= a) 1 else -1
}
