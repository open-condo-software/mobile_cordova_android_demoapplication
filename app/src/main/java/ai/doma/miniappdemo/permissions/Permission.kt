package ai.doma.core.system.permissions

data class Permission(
  val permission: String,
  val isGranted: Boolean,
  val shouldShowRational: Boolean = false
)
