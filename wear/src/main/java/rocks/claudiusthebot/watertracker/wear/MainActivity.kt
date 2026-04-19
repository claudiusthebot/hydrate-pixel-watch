package rocks.claudiusthebot.watertracker.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import rocks.claudiusthebot.watertracker.wear.ui.WearRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as WearWaterApp

        setContent { WearRoot(app.store, app.hc) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) { app.store.refresh() }
        }
    }
}
