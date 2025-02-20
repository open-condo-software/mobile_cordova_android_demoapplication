package ai.doma.miniappdemo

import ai.doma.core.DI.CoreComponent
import ai.doma.core.DI.CoreComponentProvider
import ai.doma.core.DI.CoreModule
import ai.doma.core.DI.DaggerCoreComponent
import ai.doma.core.DI.InjectHelper
import ai.doma.core.miniapps.services.BeaconScanner
import ai.doma.miniappdemo.ext.logD
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application(), CoreComponentProvider {
    override val appBundleId: String
        get() = "ai.doma.miniappdemo"

    @Volatile
    private lateinit var coreComponent: CoreComponent
    val scope = CoroutineScope(Dispatchers.IO)

    override fun provideCoreComponent(): CoreComponent {
        if (!this::coreComponent.isInitialized) {
            coreComponent = DaggerCoreComponent
                .builder()
                .coreModule(CoreModule(this))
                .build()
        }
        return coreComponent
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        InjectHelper.provideCoreComponent(this)

        scope.launch {
            BeaconScanner.stopScanning()
            BeaconScanner.init(provideCoreComponent().beaconRegionRepository, this@App)
            logD("BeaconWorker") { "run emitting" }
            try {
                BeaconScanner.runEmitting().await()
            } catch (e: Exception) {
                logD("BeaconWorker") { "error: $e" }
            }
        }
    }



}