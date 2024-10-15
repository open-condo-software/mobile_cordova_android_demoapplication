package ai.doma.miniappdemo.DI

import ai.doma.core.DI.*
import androidx.work.ListenableWorker
import com.dwsh.storonnik.DI.AppWorkerScope
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Provider


@Component(dependencies = [GlobalWorkerComponent::class], modules = [WorkerModule::class])
@AppWorkerScope
abstract class ClientWorkerComponent: FeatureComponent() {
    abstract val workerFactory: AppWorkerFactory
}


@Module
class WorkerModule {


    @Provides
    @AppWorkerScope
    fun bindChildeWorkerStub() = mapOf<Class<out ListenableWorker>,
            @JvmSuppressWildcards Provider<ChildWorkerFactory<*>>>()
}