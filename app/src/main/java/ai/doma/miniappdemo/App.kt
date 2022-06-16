package ai.doma.miniappdemo

import ai.doma.core.DI.CoreComponent
import ai.doma.core.DI.CoreComponentProvider
import ai.doma.core.DI.CoreModule
import ai.doma.core.DI.DaggerCoreComponent
import android.app.Application

class App: Application(), CoreComponentProvider {
    override val appBundleId: String
        get() = "ai.doma.miniappdemo"
    @Volatile
    private lateinit var coreComponent: CoreComponent

    override fun provideCoreComponent(): CoreComponent {
        if (!this::coreComponent.isInitialized) {
            coreComponent = DaggerCoreComponent
                .builder()
                .coreModule(CoreModule(this))
                .build()
        }
        return coreComponent
    }
}