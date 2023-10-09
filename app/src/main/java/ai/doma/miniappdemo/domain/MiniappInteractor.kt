package ai.doma.feature_miniapps.domain

import ai.doma.core.DI.CoreModule.Companion.API_URL_DEBUG
import ai.doma.miniappdemo.R
import ai.doma.miniappdemo.data.MiniappCookieContextRepository
import ai.doma.miniappdemo.data.MiniappRepository
import ai.doma.miniappdemo.data.TEST_MINIAPP_URL
import ai.doma.miniappdemo.domain.MiniappNativeConfig
import android.content.Context
import com.dwsh.storonnik.DI.FeatureScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import okhttp3.Cookie
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


const val MINIAPP_SERVER_AUTH_ID = "serverAuthTest"
const val MINIAPP_SERVER_AUTH_BY_URL_ID = "serverAuthByUrlTest"

@Singleton
class MiniappInteractor @Inject constructor(
    val context: Context,
    val miniappRepository: MiniappRepository,
    val miniappCookieContextRepository: MiniappCookieContextRepository
) {


    fun getOrDownloadMiniapp(miniappId: String) = flow {
        miniappRepository.downloadMiniappFromUrl(miniappId, TEST_MINIAPP_URL)
        val newLocalAppFile = MiniappRepository.getMiniapp(context, miniappId)

        val meta = File(newLocalAppFile, "www" + File.separator + "native_config.json")
        val config = if (meta.exists()) {
            val json = org.json.JSONObject(meta.readText())
            MiniappNativeConfig.createFromJson(json)
        } else MiniappNativeConfig.createFromJson(null)


        emit(newLocalAppFile to config)
    }

    private val updateDebugMiniappsRelay =
        MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST).apply {
            tryEmit(Unit)
        }

    fun getCookies(miniappId: String) =
        miniappCookieContextRepository.getPersistentCookies(miniappId).cookies.filter {
            it.domain != API_URL_DEBUG
        }



    fun clearDebug(miniappId: String) {
        val localAppFile = File(MiniappRepository.getMiniapp(context, miniappId), "www")
        if (!localAppFile.exists()) return
        val vconsole = File(localAppFile, "vconsole.js")
        if (vconsole.exists()) vconsole.delete()
        val indexDebug = File(localAppFile, "index-debug.html")
        if (indexDebug.exists()) indexDebug.delete()
    }

    fun appendCookie(cookie: Cookie) = miniappCookieContextRepository.appendCookie(cookie, false)


    fun saveLocalStorage(miniappId: String, data: String) {
        miniappCookieContextRepository.putPersistentLocalStorage("0", miniappId, data)

    }

    fun getLocalStorage(miniappId: String) =
        miniappCookieContextRepository.getPersistentLocalStorage(
            "0", miniappId
        )


}