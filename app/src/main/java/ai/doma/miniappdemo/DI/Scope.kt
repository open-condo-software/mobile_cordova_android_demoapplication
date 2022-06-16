package com.dwsh.storonnik.DI

import java.lang.annotation.RetentionPolicy
import javax.inject.Scope



@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class FeatureScope

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class FilterScope

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class UserScope