package rocks.claudiusthebot.watertracker.wear

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import rocks.claudiusthebot.watertracker.wear.sync.PhoneSync

/**
 * The watch intentionally does **not** talk to Health Connect itself —
 * Wear OS's HC availability is patchy per-device, and the companion-app
 * pattern is: **phone is the HC gateway, watch is a thin Data Layer
 * client**. See WaterStore for the sync protocol.
 */
class WearWaterApp : Application() {
    lateinit var store: WaterStore
        private set
    lateinit var sync: PhoneSync
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        sync = PhoneSync(this)
        store = WaterStore(this, sync)

        // Register our wearable capability so the phone can discover us.
        scope.launch { sync.ensureLocalCapability() }
    }
}
