package ai.doma.core.miniapps.domain

import ai.doma.core.system.permissions.Permission
import kotlinx.coroutines.CompletableDeferred

interface PermissionRequestGroup {
    val permissions: List<String>
    fun canRequest(): Boolean
    fun beforeRequest(): CompletableDeferred<Boolean>
    fun requestResult(
        successPermissions: Collection<String>,
        rejectedPermissions: Collection<String>
    ): CompletableDeferred<Unit>
    fun requestPermission(): CompletableDeferred<Collection<Permission>>
}
