package ai.doma.miniappdemo.data

import ai.doma.miniappdemo.ext.getSerializable
import ai.doma.miniappdemo.ext.putSerializable
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.webkit.CookieManager
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import okhttp3.Cookie
import okhttp3.HttpUrl
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Singleton


const val KEY_MINIAPP_COOKIES_PREFIX = "KEY_MINIAPP_COOKIES_"
const val KEY_MINIAPP_LOCALSTORAGE_PREFIX = "KEY_MINIAPP_LOCALSTORAGE_"
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

    private var authCookies = mutableListOf<MiniappCookie>()

    @Volatile
    private var isInAuthProcess = false

    fun startAuth() {
        isInAuthProcess = true
    }

    fun stopAuth() {
        val (validPersistentCookies, cookiesToDelete) = authCookies
            .partition {
                !it.domain.contains("doma.ai") ||
                        it.domain.contains("miniapp")
            }

        validPersistentCookies.forEach {
            applyValidCookie(it, true)
        }

        cookiesToDelete.forEach {
            removeNonValidCookie(it, true)
        }

        isInAuthProcess = false
        authCookies.clear()
    }

    fun getPersistentCookies(miniappId: String): MiniappCookies {
        currentMiniappId = miniappId
        return prefs.getSerializable(KEY_MINIAPP_COOKIES_PREFIX + miniappId)
            ?: MiniappCookies(mutableSetOf())
    }

    fun getPersistentLocalStorage(residentId: String, miniappId: String): String? {
        currentMiniappId = miniappId
        return prefs.getString(KEY_MINIAPP_LOCALSTORAGE_PREFIX + "|" + residentId + "|"+ miniappId, null)
    }


    fun getCookiesForRedirect(url: HttpUrl): List<Cookie> {
        val miniappId = currentMiniappId ?: return listOf() // only when has active miniapp

        val cookies =
            if (isInAuthProcess) authCookies + getPersistentCookies(miniappId).cookies else getPersistentCookies(
                miniappId
            ).cookies
        return cookies.map { cookie ->
            val okhttpCookie = Cookie.Builder()
                .name(cookie.name)
                .value(cookie.value)
                .expiresAt(cookie.expiresAt)
                .let { if (cookie.hostOnly) it.hostOnlyDomain(cookie.domain) else it.domain(cookie.domain) }
                .path(cookie.path)
                .let { if (cookie.secure) it.secure() else it }
                .let { if (cookie.httpOnly) it.httpOnly() else it }
                .build()

            okhttpCookie
        }.filter { it.matches(url) }
    }

    private fun putPersistentCookies(miniappId: String, miniappCookies: MiniappCookies) {
        prefs.edit(commit = true) {
            putSerializable(KEY_MINIAPP_COOKIES_PREFIX + miniappId, miniappCookies)
        }
    }

    fun putPersistentLocalStorage(residentId: String, miniappId: String, data: String) {
        prefs.edit(commit = true) {
            putString(KEY_MINIAPP_LOCALSTORAGE_PREFIX + "|" + residentId + "|"+ miniappId, data)
        }
    }

    fun appendCookie(cookie: Cookie, apply: Boolean) {
        val miniappId = currentMiniappId ?: return  // only when has active miniapp
        val miniappCookie = MiniappCookie(
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

        if (isInAuthProcess) {
            authCookies.add(miniappCookie)
        } else {
            val miniappCookies = getPersistentCookies(miniappId)
            miniappCookies.cookies.removeIf { it.domain == cookie.domain && it.name == cookie.name }
            miniappCookies.cookies.add(
                miniappCookie
            )
            putPersistentCookies(miniappId, miniappCookies)
        }

        if (apply) {
            CookieManager.getInstance()
                .setCookie("https://" + cookie.domain, "$cookie; SameSite=None")
        }
    }

    private fun applyValidCookie(miniappCookie: MiniappCookie, apply: Boolean) {
        val miniappId = currentMiniappId ?: return  // only when has active miniapp
        val miniappCookies = getPersistentCookies(miniappId)
        miniappCookies.cookies.removeIf { it.domain == miniappCookie.domain && it.name == miniappCookie.name }
        miniappCookies.cookies.add(
            miniappCookie
        )
        putPersistentCookies(miniappId, miniappCookies)
        if (apply) {
            CookieManager.getInstance()
                .setCookie("https://" + miniappCookie.domain, miniappCookie.cookieString)
        }
    }

    private fun removeNonValidCookie(miniappCookie: MiniappCookie, apply: Boolean) {
        val miniappId = currentMiniappId ?: return  // only when has active miniapp
        val miniappCookies = getPersistentCookies(miniappId)
        miniappCookies.cookies.removeIf { it.domain == miniappCookie.domain && it.name == miniappCookie.name }
        putPersistentCookies(miniappId, miniappCookies)
        if (apply) {
            CookieManager.getInstance().setCookie(
                miniappCookie.domain,
                miniappCookie.cookieString.replace("=.+?;".toRegex(), "=;")
            )
        }
    }
}