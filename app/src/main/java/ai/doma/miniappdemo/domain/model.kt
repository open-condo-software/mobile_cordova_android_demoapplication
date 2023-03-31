package ai.doma.miniappdemo.domain

import android.Manifest
import androidx.annotation.Keep
import org.json.JSONObject

fun String.toNullIfEmpty() = if (this.isEmpty()) null else this

@Keep
data class MiniappNativeConfig(
    val presentationStyle: String,
    val redirectToRelatedApplication: Boolean,
    val relatedBundleId: String?,
    val androidCustomSchemeRedirectURL: String?,
    val hostAppMinimalVersion: String?,
    val requestedPermissions: List<String>
) {

    val isModal = presentationStyle in listOf(
        "push",
        "present",
        "present_fullscreen",
        "present_fullscreen_with_navigation"
    )

    companion object {
        fun createFromJson(json: JSONObject?): MiniappNativeConfig {
            return MiniappNativeConfig(
                presentationStyle = json?.optString("presentationStyle")?.toNullIfEmpty()
                    ?: json?.optString("presentation-style")?.toNullIfEmpty()
                    ?: "push_with_navigation",
                redirectToRelatedApplication = json?.optBoolean(
                    "redirectToRelatedApplication",
                    false
                ) ?: false,
                relatedBundleId = json?.optJSONObject("relatedApplicationConfig")
                    ?.optString("androidBundleId", ""),
                androidCustomSchemeRedirectURL = json?.optJSONObject("relatedApplicationConfig")
                    ?.optString("androidCustomSchemeRedirectURL", ""),
                hostAppMinimalVersion = json?.optJSONObject("hostApplication")
                    ?.optJSONObject("android")?.optString("minimumSupportedVersion", "")
                    ?.toNullIfEmpty(),
                requestedPermissions = json?.optJSONArray("mobile_permissions")?.let {
                    val perms = mutableListOf<String>()
                    for(idx in 0..it.length()){
                        it.optString(idx)?.toNullIfEmpty()?.let{
                            perms.add(it)
                        }
                    }
                    mapWebPermissionsToAndroid(perms)
                }.orEmpty()
            )
        }


        fun mapWebPermissionsToAndroid(list: List<String>) = list.mapNotNull {
            when(it){
                "record_audio" -> Manifest.permission.RECORD_AUDIO
                "audio_settings" -> Manifest.permission.MODIFY_AUDIO_SETTINGS
                "camera" -> Manifest.permission.CAMERA
                else -> null
            }
        }
    }
}