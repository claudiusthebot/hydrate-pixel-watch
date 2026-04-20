package rocks.claudiusthebot.watertracker.phone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import rocks.claudiusthebot.watertracker.phone.ui.RootNav
import rocks.claudiusthebot.watertracker.phone.ui.theme.WaterTrackerTheme

class MainActivity : ComponentActivity() {

    private val vm: WaterViewModel by viewModels()

    private val requestNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* silently ignore; reminders just won't show */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        requestNotifPermIfNeeded()

        setContent {
            WaterTrackerTheme {
                RootNav(vm = vm)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                vm.refresh()
            }
        }
    }

    private fun requestNotifPermIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
