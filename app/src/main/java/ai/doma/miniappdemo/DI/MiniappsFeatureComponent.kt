package ai.doma.feature_miniapps.DI

import ai.doma.core.DI.CoreComponent
import ai.doma.core.DI.FeatureComponent
import ai.doma.feature_miniapps.presentation.view.MiniappDialogFragment
import ai.doma.feature_miniapps.presentation.view.MiniappFragment
import com.dwsh.storonnik.DI.FeatureScope
import dagger.Component
import dagger.Module


@Component(dependencies = [CoreComponent::class], modules = [MiniappsModule::class])
@FeatureScope
abstract class MiniappsFeatureComponent: FeatureComponent() {
    abstract fun inject(miniappFragment: MiniappFragment)
    abstract fun inject(miniappFragment: MiniappDialogFragment)


}

@Module
class MiniappsModule{

}