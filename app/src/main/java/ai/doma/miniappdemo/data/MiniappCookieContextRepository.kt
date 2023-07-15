package ai.doma.miniappdemo.data

import ai.doma.miniappdemo.ext.getSerializable
import ai.doma.miniappdemo.ext.putSerializable
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import okhttp3.Cookie
import okhttp3.HttpUrl
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Singleton


const val KEY_MINIAPP_COOKIES_PREFIX = "KEY_MINIAPP_COOKIES_"

@Keep
data class MiniappCookie(
    val name: String,
    val value: String,
    val expiresAt: Long,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
    val persistent: Boolean,
    val hostOnly: Boolean,
    val cookieString: String
) : Serializable {

}

@Keep
data class MiniappCookies(var cookies: MutableSet<MiniappCookie>) : Serializable

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
        miniappCookies.cookies = miniappCookies.cookies.filter { !(it.domain == cookie.domain && it.name == cookie.name) }.toMutableSet()
        miniappCookies.cookies.add(
            MiniappCookie(
                name = cookie.name,
                value = cookie.value,
                expiresAt = cookie.expiresAt,
                domain = cookie.domain,
                path = cookie.path,
                secure = cookie.secure,
                httpOnly = cookie.httpOnly,
                persistent = cookie.persistent,
                hostOnly = cookie.hostOnly,
                "$cookie; SameSite=None"
            )
        )
        putCookies(miniappId, miniappCookies)
    }

    fun getCookiesForRedirect(url: HttpUrl): List<Cookie> {
        val miniappId = currentMiniappId ?: return listOf() // only when has active miniapp
        return getCookies(miniappId).cookies.map {cookie ->
            val okhttpCookie = Cookie.Builder()
                .name(cookie.name)
                .value(cookie.value)
                .expiresAt(cookie.expiresAt)
                .let { if(cookie.hostOnly) it.hostOnlyDomain(cookie.domain) else it.domain(cookie.domain) }
                .path(cookie.path)
                .let { if(cookie.secure) it.secure() else it }
                .let { if(cookie.httpOnly) it.httpOnly() else it }
                .build()

            okhttpCookie
        }.filter { it.matches(url) }
    }
}