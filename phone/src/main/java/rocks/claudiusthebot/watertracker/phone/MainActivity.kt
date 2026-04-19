package rocks.claudiusthebot.watertracker.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import rocks.claudiusthebot.watertracker.phone.ui.RootNav
import rocks.claudiusthebot.watertracker.phone.ui.theme.WaterTrackerTheme

class MainActivity : ComponentActivity() {

    private val vm: WaterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            WaterTrackerTheme {
                RootNav(vm = vm)
            }
        }

        // Refresh on resume so we pick up changes written by the wear listener.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                vm.refresh()
            }
        }
    }
}
