package ai.doma.core.DI

import android.content.Context


object InjectHelper {
    fun provideCoreComponent(applicationContext: Context): CoreComponent{
        return if (applicationContext is CoreComponentProvider) {
            (applicationContext as CoreComponentProvider).provideCoreComponent()
        } else {
            throw IllegalStateException("The application context you have passed does not implement CoreComponentProvider")
        }.apply {
            CoreComponent.initializationDeferred.complete(Unit)
        }
    }
}

