package ai.doma.miniappdemo.workers

import ai.doma.core.DI.CoreComponent
import ai.doma.core.miniapps.data.repositories.BeaconRegionRepository
import ai.doma.core.miniapps.services.BeaconScanner
import ai.doma.miniappdemo.DI.ChildWorkerFactory
import ai.doma.miniappdemo.DI.GlobalWorkerComponent
import ai.doma.miniappdemo.ext.logD
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

const val BEACON_WORKER_ID = "BEACON_WORKER_ID_2"
const val BEACON_WORK_TAG = "BEACON_WORK_TAG_2"

class BeaconWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParameters: WorkerParameters,
    private val repository: BeaconRegionRepository
): CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        logD("BeaconWorker") { "start doWork" }
        BeaconScanner.init(repository, context.applicationContext)

        BeaconScanner.runEmitting().await()

        return Result.success()
    }

    @AssistedFactory
    interface Factory : ChildWorkerFactory<BeaconWorker>

    companion object {
        suspend fun runPeriodicWork(context: Context) {
            //await workermanager initialized!
            GlobalWorkerComponent.initializationDeferred.await()
            if( CoreComponent.get()?.isMainProcess != true) return
            logD("BeaconWorker") { "BeaconWorker runPeriodic after await" }

            val periodicRequest = PeriodicWorkRequestBuilder<BeaconWorker>(
                10,
                TimeUnit.MINUTES,
                0,
                TimeUnit.MINUTES
            )//.setConstraints(Constraints(requiresDeviceIdle = false))
                .addTag(BEACON_WORK_TAG)
                .build()
            logD("BeaconWorker") { "BeaconWorker runPeriodic after await 2" }

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                BEACON_WORKER_ID,
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicRequest
            )
            logD("BeaconWorker") { "BeaconWorker runPeriodic after await 3" }
        }
    }
}