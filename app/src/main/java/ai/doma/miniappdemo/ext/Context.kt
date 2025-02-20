package ai.doma.miniappdemo.ext

import ai.doma.core.system.permissions.Permission
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.getSystemService
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicInteger

inline val Context.inputMethodManager: InputMethodManager?
    get() = getSystemService<InputMethodManager>()

fun Context.showToast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

suspend fun Context.getPermissionGroups(
    permissions: List<String>,
    testOldApi: Boolean = false
): List<String> {
    val gNames = mutableSetOf<String>()
    val requestCompletable = CompletableDeferred<Unit>()
    val callbackCount = AtomicInteger()

    permissions.forEach { permission ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !testOldApi) {
            packageManager.getGroupOfPlatformPermission(
                permission, mainExecutor
            ) { groupName ->
                try {
                    gNames.add(groupName)
                } catch (_: Exception) {
                } finally {
                    if (callbackCount.incrementAndGet() == permissions.size) {
                        requestCompletable.complete(Unit)
                    }
                }
            }
        } else {
            val permissionGroups = packageManager.getAllPermissionGroups(
                PackageManager.GET_META_DATA
            )
            permissionGroups.forEach {
                val permissionsByGroup = packageManager.queryPermissionsByGroup(
                    it.name, PackageManager.GET_META_DATA
                )
                if (permissionsByGroup.any { permissions.contains(it.name) }) {
                    it.name.takeIf { it != "android.permission-group.UNDEFINED" }?.let {
                        gNames.add(it)
                    }
                }
            }

            requestCompletable.complete(Unit)
        }
    }

    requestCompletable.await()
    return gNames.toList()
}


suspend fun Context.getPermissionGroups(
    permissions: List<Permission>,
): List<String> = getPermissionGroups(permissions.map { it.permission })