package ai.doma.core.miniapps.domain

import ai.doma.core.system.permissions.Permission
import ai.doma.miniappdemo.collectAndTrace
import ai.doma.miniappdemo.ext.logD
import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PermissionRequestGroupBackgroundLocation(
    private val context: Context,
    private val requestPermissionDelegate: (Collection<String>) -> Flow<List<Permission>>,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : PermissionRequestGroup {
    override val permissions: List<String> = listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    override fun canRequest(): Boolean {
        val locationPermission = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)

        val bgLocationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else return true

        return locationPermission == PackageManager.PERMISSION_GRANTED
                && bgLocationPermission != PackageManager.PERMISSION_GRANTED
    }

    override fun beforeRequest(): CompletableDeferred<Boolean> {
        val completableDeferred = CompletableDeferred<Boolean>()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Can't request permission")
        builder.setMessage("This permission has been previously denied to this app.  In order to grant it now, you must go to Android Settings to enable this permission.")
        builder.setPositiveButton("OK") { _, _ ->
            completableDeferred.complete(true)
        }
        builder.setOnCancelListener {
            completableDeferred.complete(false)
        }
        builder.show()

        return completableDeferred
    }

    override fun requestResult(
        successPermissions: Collection<String>,
        rejectedPermissions: Collection<String>
    ): CompletableDeferred<Unit> {
        return CompletableDeferred(Unit)
    }

    override fun requestPermission(): CompletableDeferred<Collection<Permission>> {
        val completableDeferred = CompletableDeferred<Collection<Permission>>()
        scope.launch(Dispatchers.Main){
            requestPermissionDelegate(listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                .collectAndTrace(onError = {
                    logD("BeaconWorker") { "request permission error ($it)" }
                    completableDeferred.complete(emptyList())
                }) {
                    logD("BeaconWorker") { "permission request result(${it.size}) = $it" }
                    completableDeferred.complete(it)
                }
        }
        return completableDeferred
    }
}