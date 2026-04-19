package rocks.claudiusthebot.watertracker.wear

import android.app.Application
import rocks.claudiusthebot.watertracker.wear.health.WearHealthConnect
import rocks.claudiusthebot.watertracker.wear.sync.PhoneSync
import rocks.claudiusthebot.watertracker.wear.WaterStore

class WearWaterApp : Application() {
    lateinit var hc: WearHealthConnect
        private set
    lateinit var store: WaterStore
        private set
    lateinit var sync: PhoneSync
        private set

    override fun onCreate() {
        super.onCreate()
        hc = WearHealthConnect(this)
        sync = PhoneSync(this)
        store = WaterStore(this, hc, sync)
    }
}
