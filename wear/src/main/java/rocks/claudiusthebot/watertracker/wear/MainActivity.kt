package rocks.claudiusthebot.watertracker.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import rocks.claudiusthebot.watertracker.wear.ui.WearRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotifPermIfNeeded()
        val app = application as WearWaterApp

        setContent { WearRoot(app.store, app.hc) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) { app.store.refresh() }
        }
    }

    private fun requestNotifPermIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
