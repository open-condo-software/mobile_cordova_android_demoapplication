package ai.doma.feature_miniapps.presentation.viewmodel


import ai.doma.feature_miniapps.domain.MiniappInteractor
import ai.doma.miniappdemo.collectAndTrace
import ai.doma.miniappdemo.domain.MiniappNativeConfig
import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject


class MiniappViewModelFactory @Inject constructor(
    private val intrr: MiniappInteractor,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MiniappViewModel(intrr) as T
    }
}


class MiniappViewModel(
    private val miniappInteractor: MiniappInteractor,
) : ViewModel() {
    lateinit var miniappId: String

    val miniapp = MutableSharedFlow<Pair<File, MiniappNativeConfig>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    fun loadApp(){
        CookieManager.getInstance().acceptCookie()
        miniappInteractor.getCookies(miniappId).cookies.forEach {
            CookieManager.getInstance().setCookie(it.domain, it.cookieString)
        }


        viewModelScope.launch {
            miniappInteractor.getOrDownloadMiniapp(miniappId)
                .flowOn(Dispatchers.IO)
                .collectAndTrace(onError = {

                }) { (file, config) ->

                    miniapp.tryEmit(file to config)
                }
        }
    }

    fun releaseApp(){
        //cleanup cookies from manager
        miniappInteractor.getCookies(miniappId).cookies.forEach {
            CookieManager.getInstance().setCookie(it.domain, it.cookieString.replace("=.+?;".toRegex(),"=;"))
        }
    }
}
