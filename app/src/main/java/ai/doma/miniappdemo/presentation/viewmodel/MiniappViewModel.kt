package ai.doma.feature_miniapps.presentation.viewmodel


import ai.doma.feature_miniapps.domain.MiniappInteractor
import ai.doma.miniappdemo.collectAndTrace
import ai.doma.miniappdemo.domain.MiniappNativeConfig
import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.net.URL
import javax.inject.Inject


class MiniappViewModelFactory @Inject constructor(
    private val intrr: MiniappInteractor,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MiniappViewModel(intrr) as T
    }
}


class MiniappViewModel(
    val miniappInteractor: MiniappInteractor,
) : ViewModel() {
    lateinit var miniappName: String
    lateinit var miniappId: String
    lateinit var version: String

    val indexFilePath get() =  "/www/index.html"

    val miniapp = MutableSharedFlow<Pair<File, MiniappNativeConfig>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    var onResourceLoadUrls = mutableSetOf<String>()

    private var inflatedStorageOnce = false

    @Volatile
    var awaitLocalStorageInflate = false

    @Volatile
    var awaitMiniappFirstLoad = false
    fun loadApp() {
        CookieManager.getInstance().acceptCookie()
        miniappInteractor.getCookies(miniappId).forEach {
            CookieManager.getInstance().setCookie("https://" + it.domain, it.cookieString)
        }

        viewModelScope.launch {
            miniappInteractor.getOrDownloadMiniapp(miniappId).map {
                it
            }
                .flowOn(Dispatchers.IO)
                .collectAndTrace(onError = {

                }) { (file, config) ->

                    miniapp.tryEmit(file to config)
                }
        }

    }

    fun saveLocalStorage(data: String) {
        miniappInteractor.saveLocalStorage(miniappId, data)
    }

    fun inflateLocalStorage(miniappId: String): Map<String, String>? {
        if (inflatedStorageOnce) return null
        inflatedStorageOnce = true
        return miniappInteractor.getLocalStorage(miniappId)
            ?.takeIf { it.isNotEmpty() && it != "null" }?.let {
                val jsonArray = Gson().fromJson(it, JsonArray::class.java)
                jsonArray.map { it.asString.split("--+++===+++-->>").let { it[0] to it[1] } }
                    .toMap()
            }
    }

    fun releaseApp() {
        //cleanup cookies from manager
//        miniappInteractor.getCookies(miniappId).cookies.forEach {
//            CookieManager.getInstance().setCookie(it.domain, it.cookieString.replace("=.+?;".toRegex(),"=;"))
//        }

        //save all internal webview cookies!
        onResourceLoadUrls.forEach {
            val domain = try {
                URL(it).host
            } catch (e: Exception) {
                return@forEach
            }
            val cookieString = CookieManager.getInstance().getCookie(it) ?: return@forEach
            val httpUrl = it.toHttpUrlOrNull() ?: return@forEach
            val cookie = Cookie.parse(httpUrl, cookieString) ?: return@forEach
            miniappInteractor.appendCookie(cookie)
        }


        CookieManager.getInstance().removeSessionCookies(null)
        CookieManager.getInstance().removeAllCookies(null)
        //CookieManager.getInstance().flush()


    }

    override fun onCleared() {
        super.onCleared()

    }

}
