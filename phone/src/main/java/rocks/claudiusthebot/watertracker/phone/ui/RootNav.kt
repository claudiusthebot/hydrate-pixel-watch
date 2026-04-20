package rocks.claudiusthebot.watertracker.phone.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import rocks.claudiusthebot.watertracker.phone.WaterViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed class Dest(val route: String, val label: String, val icon: @Composable () -> Unit) {
    data object Today : Dest("today", "Today",
        { Icon(Icons.Rounded.WaterDrop, contentDescription = null) })
    data object History : Dest("history", "History",
        { Icon(Icons.Rounded.BarChart, contentDescription = null) })
    data object Settings : Dest("settings", "Settings",
        { Icon(Icons.Rounded.Settings, contentDescription = null) })
}

private val DESTS = listOf(Dest.Today, Dest.History, Dest.Settings)

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (currentRoute) {
                                Dest.Today.route -> "Hydrate"
                                Dest.History.route -> "History"
                                Dest.Settings.route -> "Settings"
                                else -> "Hydrate"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (currentRoute == Dest.Today.route) {
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                DESTS.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = {
                            nav.navigate(dest.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { dest.icon() },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Dest.Today.route,
            modifier = Modifier.fillMaxSize().padding(inner)
        ) {
            composable(Dest.Today.route) { TodayScreen(vm) }
            composable(Dest.History.route) { HistoryScreen(vm) }
            composable(Dest.Settings.route) { SettingsScreen(vm) }
        }
    }
}
