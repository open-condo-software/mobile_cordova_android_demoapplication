package ai.doma.miniappdemo.DI

import ai.doma.core.DI.CoreComponent
import ai.doma.core.DI.FeatureComponent
import ai.doma.core.miniapps.data.repositories.BeaconRegionRepository
import ai.doma.miniappdemo.workers.BeaconWorker
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.dwsh.storonnik.DI.AppWorkerScope
import com.dwsh.storonnik.DI.GlobalWorkerScope
import dagger.Binds
import dagger.Component
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject
import javax.inject.Provider
import kotlin.reflect.KClass


@Component(dependencies = [CoreComponent::class], modules = [GlobalWorkerModule::class])
@GlobalWorkerScope
abstract class GlobalWorkerComponent: FeatureComponent() {
//    abstract val roomDB: RoomDB
//    abstract val api: Api
    abstract val appContext: Context
    abstract val repository: BeaconRegionRepository
//    abstract val contextRepository: ContextRepository
//    abstract val dbProvider: DBProvider
//    abstract val userComponentHolder: CurrentUserComponentWeakHolder

    abstract val globalFactories: GlobalFactories

    companion object {
        val initializationDeferred = CompletableDeferred<Unit>(null)
    }
}


@Module
abstract class GlobalWorkerModule {

//    @Binds
//    @IntoMap
//    @WorkerKey(PushDirectDeliveryWorker::class)
//    abstract fun bindPushDirectDeliveryWorker(factory: PushDirectDeliveryWorker.Factory): ChildWorkerFactory<*>
//
//    @Binds
//    @IntoMap
//    @WorkerKey(BLEScannerPeriodicReinitWorker::class)
//    abstract fun bindBLEScannerPeriodicReinitWorker(factory: BLEScannerPeriodicReinitWorker.Factory): ChildWorkerFactory<*>

    @Binds
    @IntoMap
    @WorkerKey(BeaconWorker::class)
    abstract fun bindBeaconWorker(factory: BeaconWorker.Factory): ChildWorkerFactory<*>

}


@MapKey
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WorkerKey(val value: KClass<out ListenableWorker>)


interface ChildWorkerFactory<T: ListenableWorker> {
    fun create(appContext: Context, params: WorkerParameters): T
}

@GlobalWorkerScope
class GlobalFactories @Inject constructor(val workerFactories: Map<Class<out ListenableWorker>,
        @JvmSuppressWildcards Provider<ChildWorkerFactory<*>>>)

@AppWorkerScope
class AppWorkerFactory @Inject constructor(
    private val globalFactories: GlobalFactories,
    private val workerFactories: Map<Class<out ListenableWorker>,
            @JvmSuppressWildcards Provider<ChildWorkerFactory<*>>>,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        val foundEntry =
            try {(workerFactories + globalFactories.workerFactories).entries.find { Class.forName(workerClassName).isAssignableFrom(it.key) } } catch (e: Exception){ null }

        val factoryProvider = foundEntry?.value

        factoryProvider?.let{
            val provider = factoryProvider.get().create(appContext, workerParameters)
            return provider
        } ?:run {
            val kClass = try { Class.forName(workerClassName).kotlin } catch (e: java.lang.Exception) { return null}
            val instance = kClass.constructors.first().call(appContext, workerParameters)
            return instance as ListenableWorker
        }
    }
}