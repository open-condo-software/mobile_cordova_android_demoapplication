package ai.doma.core.miniapps.domain

import ai.doma.core.system.permissions.Permission
import ai.doma.miniappdemo.collectAndTrace
import ai.doma.miniappdemo.ext.getPermissionGroups
import ai.doma.miniappdemo.ext.logD
import ai.doma.miniappdemo.presentation.QuestionDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PermissionRequestGroupCommon(
    private val context: Context,
    permissions: List<String>,
    private val requestPermissionDelegate: (Collection<String>) -> Flow<List<Permission>>,
    private val childFragmentManager: FragmentManager
): PermissionRequestGroup {
    val scope = CoroutineScope(Dispatchers.IO)
    override val permissions: List<String> = permissions

    override fun canRequest(): Boolean {
        return true
    }

    override fun beforeRequest(): CompletableDeferred<Boolean> {
        return CompletableDeferred(true)
    }

    override fun requestResult(
        successPermissions: Collection<String>,
        rejectedPermissions: Collection<String>
    ): CompletableDeferred<Unit> {
        val completableDeferred = CompletableDeferred<Unit>()
        scope.launch {
            if (rejectedPermissions.isNotEmpty()) {
                val groupNames = context.getPermissionGroups(rejectedPermissions.toList())

                val groupLabels = groupNames.mapNotNull {
                    context.packageManager
                        ?.getPermissionGroupInfo(it, PackageManager.GET_META_DATA)
                        ?.loadLabel(context.packageManager)
                        ?.let { " - $it" }

                }.toMutableSet()

                val permissionLabels = rejectedPermissions.mapNotNull {
                    context.packageManager
                        ?.getPermissionInfo(it, PackageManager.GET_META_DATA)
                        ?.loadLabel(context.packageManager)
                        ?.let { " - $it" }
                }

                QuestionDialog.show(
                    childFragmentManager,
                    title = "Provide the application with the necessary permissions",
                    message = "To access all features of the app, grant access to your device. These are the accesses that are missing: \n${
                        groupLabels.takeIf { it.isNotEmpty() }.let {
                            it ?: permissionLabels
                        }.joinToString(
                            separator = "\n"
                        )
                    }",
                    positiveButtonName = "Go to settings",
                    positiveFunc = {
                        context.let {
                            val uri = Uri.fromParts("package", it.packageName, null)
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
//                        dismiss()
                            completableDeferred.complete(Unit)
                        }
                    },
                    neutralButtonName = "Continue",
                    neutralFunc = {
                        completableDeferred.complete(Unit)
                    },
                    onCancel = {
                        completableDeferred.complete(Unit)
                    }
                )
            } else {
                completableDeferred.complete(Unit)
            }
        }

        return completableDeferred
    }

    override fun requestPermission(): CompletableDeferred<Collection<Permission>> {
        val completableDeferred = CompletableDeferred<Collection<Permission>>()

        scope.launch(Dispatchers.Main) {
            requestPermissionDelegate(permissions).collectAndTrace(onError = {
                logD("BeaconWorker") { "request permission error ($it)" }
                completableDeferred.complete(listOf())
            }) {
                completableDeferred.complete(it)
            }
        }

        return completableDeferred
    }


}