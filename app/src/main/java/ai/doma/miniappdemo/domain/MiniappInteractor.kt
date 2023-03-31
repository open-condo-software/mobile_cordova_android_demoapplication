package ai.doma.feature_miniapps.domain

import ai.doma.miniappdemo.data.MiniappCookieContextRepository
import ai.doma.miniappdemo.data.MiniappRepository
import ai.doma.miniappdemo.data.TEST_MINIAPP_URL
import ai.doma.miniappdemo.domain.MiniappNativeConfig
import android.content.Context
import com.dwsh.storonnik.DI.FeatureScope
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject


const val MINIAPP_SERVER_AUTH_ID = "serverAuthTest"
const val MINIAPP_SERVER_AUTH_BY_URL_ID = "serverAuthByUrlTest"

@FeatureScope
class MiniappInteractor @Inject constructor(
    val context: Context,
    val miniappRepository: MiniappRepository,
    val miniappCookieContextRepository: MiniappCookieContextRepository
) {

    fun getCookies(miniappId: String) = miniappCookieContextRepository.getCookies(miniappId)

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

}