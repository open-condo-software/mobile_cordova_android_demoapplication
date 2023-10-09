package ai.doma.miniappdemo.domain

import ai.doma.feature_miniapps.domain.MiniappInteractor
import android.Manifest
import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import org.json.JSONObject
import java.io.Serializable

fun String.toNullIfEmpty() = if (this.isEmpty()) null else this

data class MiniappEntity(
    val id: String,
    val name: String?,
    val shortDescription: String?,
    val version: String,
    val updatedAt: Long,
    val colorPrimary: String?,
    val colorSecondary: String?,
    val logoUrl: String?,
    val publicUrl: String?
) : Serializable


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
        "native",
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
                    for (idx in 0..it.length()) {
                        it.optString(idx)?.toNullIfEmpty()?.let {
                            perms.add(it)
                        }
                    }
                    mapWebPermissionsToAndroid(perms)
                }.orEmpty()
            )
        }


        fun mapWebPermissionsToAndroid(list: List<String>) = list.mapNotNull {
            when (it) {
                "record_audio" -> Manifest.permission.RECORD_AUDIO
                "audio_settings" -> Manifest.permission.MODIFY_AUDIO_SETTINGS
                "camera" -> Manifest.permission.CAMERA
                else -> null
            }
        }
    }
}


class JavaScriptInterface constructor(var miniappInteractor: MiniappInteractor) {
    @JavascriptInterface
    fun condoHostApplicationIsDemo() = true

    @JavascriptInterface
    fun condoHostApplicationBaseURL() = "https://condo.d.doma.ai"

    @JavascriptInterface
    fun condoHostApplicationInstallationID() = "b8f73d1c-158a-4507-8b9d-379220c49e3b"

    @JavascriptInterface
    fun condoHostApplicationDeviceID() = "kFKmHGlQlf5KjeLldmFSpq"

    @JavascriptInterface
    fun condoHostApplicationLocale() = "ru-RU"
}