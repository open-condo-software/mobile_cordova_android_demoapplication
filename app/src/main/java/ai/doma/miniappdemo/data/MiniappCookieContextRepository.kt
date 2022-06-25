package ai.doma.miniappdemo.data

import ai.doma.miniappdemo.ext.getSerializable
import ai.doma.miniappdemo.ext.putSerializable
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep
import androidx.core.content.edit
import okhttp3.Cookie
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Singleton


const val KEY_MINIAPP_COOKIES_PREFIX = "KEY_MINIAPP_COOKIES_"

@Keep
data class MiniappCookie(val domain: String, val name: String, val value: String, val cookieString: String) : Serializable {

}

@Keep
data class MiniappCookies(val cookies: MutableSet<MiniappCookie>) : Serializable

@Singleton
class MiniappCookieContextRepository @Inject constructor(
    val prefs: SharedPreferences
) {
    var currentMiniappId: String? = null

    fun getCookies(miniappId: String): MiniappCookies {
        currentMiniappId = miniappId
        return prefs.getSerializable(KEY_MINIAPP_COOKIES_PREFIX + miniappId)
            ?: MiniappCookies(mutableSetOf())
    }

    fun putCookies(miniappId: String, miniappCookies: MiniappCookies) {
        prefs.edit {
            putSerializable(KEY_MINIAPP_COOKIES_PREFIX + miniappId, miniappCookies)
        }
    }

    fun appendCookie(cookie: Cookie) {
        val miniappId = currentMiniappId ?: return  // only when has active miniapp
        val miniappCookies = getCookies(miniappId)
        miniappCookies.cookies.removeIf { it.domain == "https://" + cookie.domain && it.name == cookie.name }
        miniappCookies.cookies.add(MiniappCookie("https://" + cookie.domain, cookie.name, cookie.value, "$cookie; SameSite=None"))
        putCookies(miniappId, miniappCookies)
    }
}