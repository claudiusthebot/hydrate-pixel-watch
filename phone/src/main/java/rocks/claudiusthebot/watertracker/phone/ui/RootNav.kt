package rocks.claudiusthebot.watertracker.phone.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import rocks.claudiusthebot.watertracker.phone.WaterViewModel

sealed class Dest(val route: String, val label: String, val icon: @Composable () -> Unit) {
    data object Today : Dest("today", "Today",
        { Icon(Icons.Rounded.WaterDrop, contentDescription = null) })
    data object History : Dest("history", "History",
        { Icon(Icons.Rounded.BarChart, contentDescription = null) })
    data object Settings : Dest("settings", "Settings",
        { Icon(Icons.Rounded.Settings, contentDescription = null) })
}

private val DESTS = listOf(Dest.Today, Dest.History, Dest.Settings)

@Composable
fun RootNav(vm: WaterViewModel) {
    val nav = rememberNavController()
    val backEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route

    Scaffold(
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
