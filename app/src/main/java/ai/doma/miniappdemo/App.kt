package ai.doma.miniappdemo

import ai.doma.core.DI.CoreComponent
import ai.doma.core.DI.CoreComponentProvider
import ai.doma.core.DI.CoreModule
import ai.doma.core.DI.DaggerCoreComponent
import ai.doma.core.DI.InjectHelper
import ai.doma.core.miniapps.services.BeaconScanner
import ai.doma.miniappdemo.DI.ClientWorkerComponent
import ai.doma.miniappdemo.DI.DaggerClientWorkerComponent
import ai.doma.miniappdemo.DI.DaggerGlobalWorkerComponent
import ai.doma.miniappdemo.DI.GlobalWorkerComponent
import ai.doma.miniappdemo.ext.logD
import ai.doma.miniappdemo.workers.BeaconWorker
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.Configuration
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class App : Application(), CoreComponentProvider {
    override val appBundleId: String
        get() = "ai.doma.miniappdemo"

    @Volatile
    private lateinit var coreComponent: CoreComponent
    val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private lateinit var workerComponent: ClientWorkerComponent

    override fun provideCoreComponent(): CoreComponent {
        if (!this::coreComponent.isInitialized) {
            coreComponent = DaggerCoreComponent
                .builder()
                .coreModule(CoreModule(this))
                .build()
            coreComponent.isMainProcess = isMainProcess()
        }
        return coreComponent
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        InjectHelper.provideCoreComponent(this)
        initWorkerScope()

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

        scope.launch {
            logD("BeaconWorker") { "runPeriodicWork" }
            BeaconWorker.runPeriodicWork(this@App.applicationContext)
        }
    }

    private fun initWorkerScope() {
        workerComponent = DaggerClientWorkerComponent.builder()
            .globalWorkerComponent(
                DaggerGlobalWorkerComponent.builder()
                    .coreComponent(coreComponent)
                    .build()
            )
            .build()

        val workManagerConfiguration = Configuration.Builder()
            .setWorkerFactory(workerComponent.workerFactory)
            .build()

        try {
            WorkManager.initialize(this, workManagerConfiguration)
            WorkManager.getInstance(this).pruneWork()
            scope.launch {
                while (isActive && !WorkManager.isInitialized()) {
                    delay(50)
                }
                GlobalWorkerComponent.initializationDeferred.complete(Unit)
            }
        } catch (e: Throwable) {
            if (WorkManager.isInitialized()) {
                WorkManager.getInstance(this).pruneWork()
                GlobalWorkerComponent.initializationDeferred.complete(Unit)
            }
            logD { "WorkManager isn't initialized" }
        }
    }

    private fun isMainProcess(): Boolean {
        return packageName == getCurrentProcessName()
    }

    private fun getCurrentProcessName(): String? {
        val mypid = Process.myPid()
        val manager: ActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val infos: List<ActivityManager.RunningAppProcessInfo> = manager.getRunningAppProcesses()
        for (info in infos) {
            if (info.pid == mypid) {
                return info.processName
            }
        }
        // may never return null
        return null
    }
}