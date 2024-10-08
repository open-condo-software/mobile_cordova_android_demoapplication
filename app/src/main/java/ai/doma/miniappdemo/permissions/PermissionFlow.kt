package ai.doma.core.system.permissions

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.flow.flow

internal object PermissionFlow {
  private val FRAGMENT_TAG = PermissionFragment::class.java.simpleName

  internal fun request(fragment: Fragment, vararg permissionsToRequest: String) =
    request(fragment.childFragmentManager, *permissionsToRequest)

  internal fun request(activity: FragmentActivity, vararg permissionsToRequest: String) =
    request(activity.supportFragmentManager, *permissionsToRequest)

  internal fun requestEach(fragment: Fragment, vararg permissionsToRequest: String) =
    requestEach(fragment.childFragmentManager, *permissionsToRequest)

  internal fun requestEach(activity: FragmentActivity, vararg permissionsToRequest: String) =
    requestEach(activity.supportFragmentManager, *permissionsToRequest)

  private fun request(fragmentManager: FragmentManager, vararg permissionsToRequest: String) = flow {
    createFragment(fragmentManager).takeIf { permissionsToRequest.isNotEmpty() }?.run {
      request(*permissionsToRequest)
      val results = completableDeferred.await()
      if (results.isNotEmpty()) {
        emit(results)
      }
    }
  }

  private fun request(
    fragmentManager: FragmentManager,
    awaitAllPermissions: Boolean = true,
    vararg permissionsToRequest: String
  ) = flow {
    createFragment(fragmentManager).takeIf { permissionsToRequest.isNotEmpty() }?.run {
      request(awaitAllPermissions, *permissionsToRequest)
      val results = completableDeferred.await()
      if (results.isNotEmpty()) {
        emit(results)
      }
    }
  }
  internal fun unsafeRequest(fragment: Fragment, vararg permissionsToRequest: String) =
    request(fragment.childFragmentManager, false, *permissionsToRequest)


  private fun requestEach(fragmentManager: FragmentManager, vararg permissionsToRequest: String) = flow {
    createFragment(fragmentManager).takeIf { permissionsToRequest.isNotEmpty() }?.run {
      request(*permissionsToRequest)
      val results = completableDeferred.await()
      results.forEach { emit(it) }
    }
  }

  private fun createFragment(fragmentManager: FragmentManager): PermissionFragment {

    //dont reuse this shit

    val oldfragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG)?.let { it as PermissionFragment }
    val new = PermissionFragment.newInstance()
    fragmentManager
      .beginTransaction()
      .apply {
        if (oldfragment != null && oldfragment.isAdded) {
          detach(oldfragment)
        }
      }
      .add(new, FRAGMENT_TAG)
      .commitNow()
    return new
  }
}

// Extensions
fun FragmentActivity.requestPermissions(vararg permissionsToRequest: String) =
  PermissionFlow.request(this, *permissionsToRequest)
fun FragmentActivity.requestEachPermissions(vararg permissionsToRequest: String) =
  PermissionFlow.requestEach(this, *permissionsToRequest)
fun Fragment.requestPermissions(vararg permissionsToRequest: String) =
  PermissionFlow.request(this, *permissionsToRequest)
fun Fragment.requestEachPermissions(vararg permissionsToRequest: String) =
  PermissionFlow.requestEach(this, *permissionsToRequest)

fun Fragment.unsafeRequestPermissions(vararg permissionsToRequest: String) =
  PermissionFlow.unsafeRequest(this, *permissionsToRequest)