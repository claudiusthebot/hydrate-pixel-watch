package rocks.claudiusthebot.watertracker.phone

import android.app.Application
import rocks.claudiusthebot.watertracker.phone.data.WaterRepository
import rocks.claudiusthebot.watertracker.phone.health.HealthConnectManager

class WaterApp : Application() {
    lateinit var hc: HealthConnectManager
        private set
    lateinit var repo: WaterRepository
        private set

    override fun onCreate() {
        super.onCreate()
        hc = HealthConnectManager(this)
        repo = WaterRepository(this, hc)
    }
}
