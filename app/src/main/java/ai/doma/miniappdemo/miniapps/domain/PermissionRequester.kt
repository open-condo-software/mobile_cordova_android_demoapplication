package ai.doma.core.miniapps.domain

import ai.doma.core.system.permissions.Permission
import ai.doma.core.system.permissions.unsafeRequestPermissions
import android.Manifest
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PermissionRequester(
    private val fragment: Fragment
) {
    val scope = CoroutineScope(Dispatchers.IO)

    fun requestPermission(permissions: List<String>): Flow<Unit> = flow<Unit> {
        val groups = buildGrpups(permissions)
        groups.forEach {
            if (it.canRequest()) {
                val canContinue = it.beforeRequest()
                if (canContinue.await() == false) {
                    emit(Unit)
                    return@flow
                }

                val permissionResults = it.requestPermission().await()

                val rejected = permissionResults.filter { !it.isGranted && !it.shouldShowRational }
                val granted = permissionResults - rejected.toSet()
                it.requestResult(
                    granted.map { it.permission },
                    rejected.map { it.permission }
                ).await()
            }
        }

        emit(Unit)
    }

    private fun buildGrpups(permissions: List<String>): List<PermissionRequestGroup> {
        val groups = permissions.groupBy {
            when (it) {
                in listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION) -> 2
                else -> 1
            }
        }.toSortedMap(Comparator { o1, o2 ->
            o1.compareTo(o2)
        }).mapNotNull {
            when (it.key) {
                1 -> {
                    PermissionRequestGroupCommon(
                        fragment.requireContext(),
                        it.value,
                        ::requestPermissions,
                        fragment.childFragmentManager
                    )
                }

                2 -> {
                    PermissionRequestGroupBackgroundLocation(
                        fragment.requireContext(),
                        ::requestPermissions,
                    )
                }

                else -> null
            }
        }

        return groups
    }

    private fun requestPermissions(
        permissions: Collection<String>
    ): Flow<List<Permission>> {
        return fragment.unsafeRequestPermissions(*permissions.toTypedArray())
    }


}
