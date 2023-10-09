package ai.doma.feature_miniapps.presentation.view

import ai.doma.core.DI.InjectHelper
import ai.doma.core.system.permissions.requestPermissions
import ai.doma.feature_miniapps.DI.DaggerMiniappsFeatureComponent
import ai.doma.feature_miniapps.DI.MiniappsFeatureComponent
import ai.doma.feature_miniapps.presentation.viewmodel.MiniappViewModel
import ai.doma.feature_miniapps.presentation.viewmodel.MiniappViewModelFactory
import ai.doma.miniappdemo.base.BaseFragment
import ai.doma.miniappdemo.databinding.FragmentFlowMiniappBinding
import ai.doma.miniappdemo.ext.logD
import ai.doma.miniappdemo.ext.viewBinding
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import org.apache.cordova.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import ai.doma.miniappdemo.R
import ai.doma.miniappdemo.collectAndTrace
import ai.doma.miniappdemo.data.MiniappRepository
import ai.doma.miniappdemo.domain.JavaScriptInterface
import ai.doma.miniappdemo.domain.MiniappEntity
import ai.doma.miniappdemo.ext.gone
import ai.doma.miniappdemo.getViewScope
import android.graphics.Color
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import org.intellij.lang.annotations.Language

class MiniappFragment : BaseFragment() {

    override val layout: Int
        get() = R.layout.fragment_flow_miniapp
    override val vb by viewBinding(FragmentFlowMiniappBinding::bind)

    lateinit var preferences: CordovaPreferences
    lateinit var launchUrl: String
    lateinit var pluginEntries: ArrayList<PluginEntry>
    lateinit var appView: CordovaWebView
    var cordovaInterface: CordovaFragmentInterfaceImpl? = null

    @Inject
    lateinit var factory: MiniappViewModelFactory
    lateinit var model: MiniappViewModel

    private lateinit var miniappFeatureComponent: MiniappsFeatureComponent

    private var keepRunning = true
    lateinit var miniappEntity: MiniappEntity

    var savedInstanceState: Bundle? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        loadConfig()
        miniappFeatureComponent = DaggerMiniappsFeatureComponent.builder()
            .coreComponent(InjectHelper.provideCoreComponent(requireContext().applicationContext))
            .build()
        miniappFeatureComponent.inject(this)
        model = ViewModelProvider(this, factory)[MiniappViewModel::class.java]

        super.onCreate(savedInstanceState)
        this.savedInstanceState = savedInstanceState

    }
    override fun onViewInit(view: View) {

        miniappEntity = arguments?.getSerializable("miniappEntity") as? MiniappEntity ?: return
        val style = arguments?.getString("presentationStyle") ?: return

        appView = makeWebView()
        appView.view.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        appView.view.requestFocusFromTouch()
        vb.webViewContainer.addView(appView.view, 0)
        cordovaInterface = makeCordovaInterface()
        savedInstanceState?.let {
            cordovaInterface?.restoreInstanceState(it)
            logD{"CORDOVA savedInstanceState callbackService: ${it.getString("callbackService")} plugin: ${it.getBundle("plugin")?.toString()}"}
        }

        logD { "CORDOVA ${savedInstanceState.toString()}" }



        init()


        model.miniappName = miniappEntity.name.orEmpty()
        model.miniappId = miniappEntity.id
        model.version = miniappEntity.version
        model.loadApp()

        viewScope.launch {
            model.miniapp.flatMapLatest {(file, config) ->
                if(config.requestedPermissions.isNotEmpty()){
                    requestPermissions(*config.requestedPermissions.toTypedArray()).map { file }
                } else {
                    flowOf(file)
                }
            }.collectAndTrace {
                savedInstanceState?.let { return@collectAndTrace }
                appView.preferences.set("AndroidInsecureFileModeEnabled", true)

                inflateLocalStorage(it.toUri().toString() + model.indexFilePath) {
                    appView.loadUrlIntoView(it.toUri().toString() + model.indexFilePath, true)
                }
            }
        }
        savedInstanceState?.let {
            MiniappRepository.getMiniapp(requireContext(), model.miniappId)?.let {
                init()
                appView.preferences.set("AndroidInsecureFileModeEnabled", true)
                inflateLocalStorage(it.toUri().toString() + model.indexFilePath) {
                    appView.loadUrlIntoView(it.toUri().toString() + model.indexFilePath, false)
                }
            }
        }

        vb.pbWait.indeterminateTintList = ColorStateList.valueOf(Color.WHITE)
        vb.pbWait.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E6E8F1"))

        vb.ivClose.isVisible = style in listOf("push_fullscreen_with_navigation", "present_fullscreen_with_navigation")

        //example

//        requireView().postDelayed({
//            val am = requireContext().getSystemService(AUDIO_SERVICE) as AudioManager
//            am.mode = AudioManager.MODE_IN_COMMUNICATION
//            val audioDevices: List<AudioDeviceInfo> =
//                am.availableCommunicationDevices
//
//            var calmSpeakerAudioDevice: AudioDeviceInfo? = null
//            for (audioDevice in audioDevices) {
//                if (audioDevice.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) {
//                    calmSpeakerAudioDevice = audioDevice
//                }
//            }
//            am.setCommunicationDevice(calmSpeakerAudioDevice!!)
//            am.isSpeakerphoneOn=false
//        }, 20_000L)

    }


    private fun init(){
        if (!appView.isInitialized) {
            appView.init(cordovaInterface, pluginEntries, preferences)
        }
        appView.clearCache()
        cordovaInterface!!.onCordovaInit(appView.pluginManager)

        // Wire the hardware volume controls to control media if desired.
        val volumePref = preferences.getString("DefaultVolumeStream", "")
        if ("media" == volumePref.lowercase(Locale.ENGLISH)) {
            requireActivity().volumeControlStream = AudioManager.STREAM_MUSIC
        }
    }

    private fun loadConfig() {
        CondoPluginState.payload = arguments?.getString("payload")
        val parser = ConfigXmlParser()
        parser.parse(requireContext())
        preferences = parser.preferences;
        preferences.setPreferencesBundle(requireActivity().intent.extras)
        launchUrl = parser.launchUrl
        pluginEntries = parser.pluginEntries
        //Config.parser = parser
    }

    private fun inflateLocalStorage(url: String, onInflated: () -> Unit) {
        view?.getViewScope()?.launch {
            val inflateJs = model.inflateLocalStorage(miniappEntity.id)?.let { map ->
                map.map {

                    "window.localStorage.setItem(\"${it.key}\", ${
                        it.value.let {
                            if (it.getOrNull(0) in listOf(
                                    '\'', '"', '{', '['
                                ) || it.toFloatOrNull() != null
                            ) "JSON.stringify($it)" else "\"$it\""
                        }
                    });"
                }
            }?.joinToString(separator = "") { it }
                ?.let { it + "console.log(\"injected token!!\");" }

            inflateJs?.let {
                logD { "localStorage test restore: $it" }
                val mimeType = "text/html"
                val encoding = "utf-8"
                val injection = "<script type='text/javascript'>$it </script>"
                model.awaitLocalStorageInflate = true
                (appView.view as? WebView)?.loadDataWithBaseURL(
                    url,
                    injection,
                    mimeType,
                    encoding,
                    null
                )
                while (isActive && model.awaitLocalStorageInflate) {
                    delay(50)
                }
            }

            model.awaitMiniappFirstLoad = true
            view?.post(onInflated)
        }

    }

    private fun makeWebView(): CordovaWebView {
        val engine = makeWebViewEngine()
        return CordovaWebViewImpl(engine).apply {
            val webview = (this.getView() as? WebView)
            webview?.webViewClient = object: org.apache.cordova.engine.SystemWebViewClient(engine!! as org.apache.cordova.engine.SystemWebViewEngine) {
                override fun onLoadResource(view: WebView?, url: String?) {
                    super.onLoadResource(view, url)
                    if (url != null) {
                        model.onResourceLoadUrls.add(url)
                    }
                }
            }
            webview?.addJavascriptInterface(
                JavaScriptInterface(model.miniappInteractor),
                "jsInterface"
            )
        }
    }

    private fun makeWebViewEngine(): CordovaWebViewEngine? {
        return CordovaWebViewImpl.createEngine(requireContext(), preferences)
    }

    private fun makeCordovaInterface(): CordovaFragmentInterfaceImpl {
        return object : CordovaFragmentInterfaceImpl(requireActivity() as AppCompatActivity, this) {
            override fun onMessage(id: String, data: Any?): Any? {
                // Plumb this to CordovaActivity.onMessage for backwards compatibility
                return this@MiniappFragment.onMessage(id, data)
            }
        }
    }

    fun onMessage(id: String, data: Any?): Any? {
        when (id) {
            "onReceivedError" -> {
                val d = data as JSONObject
                try {
                    this.onReceivedError(
                        d.getInt("errorCode"),
                        d.getString("description"),
                        d.getString("url")
                    )
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            "exit" -> {
                findNavController().popBackStack()
            }
            "onPageFinished" -> {
                if (model.awaitLocalStorageInflate) {
                    model.awaitLocalStorageInflate = false
                } else {
                    if (model.awaitMiniappFirstLoad) {
                        model.awaitMiniappFirstLoad = false
                        appView.clearHistory()
                    }
                    vb.pbWait.gone()
                }
                logD { "CORDOVA onPageFinished" }
            }
            Condo.ACTION_CLOSE_MINIAPP -> {
                vb.root.post {
                    findNavController().popBackStack()
                }
            }
        }
        return null
    }

    fun onReceivedError(errorCode: Int, description: String, failingUrl: String) {

        // If errorUrl specified, then load it
        val errorUrl = preferences.getString("errorUrl", null)
        if (errorUrl != null && failingUrl != errorUrl && appView != null) {
            // Load URL on UI thread
            requireActivity().runOnUiThread {
                appView.showWebPage(errorUrl, false, true, null)
            }
        } else {
            val exit = errorCode != WebViewClient.ERROR_HOST_LOOKUP
            requireActivity().runOnUiThread {
                if (exit) {
                    appView.view.visibility = View.GONE
                    displayError("Application Error", "$description ($failingUrl)", "OK", exit)
                }
            }
        }
    }

    fun displayError(title: String?, message: String?, button: String?, exit: Boolean) {
        requireActivity().runOnUiThread {
            try {
                val dlg = AlertDialog.Builder(requireContext())
                dlg.setMessage(message)
                dlg.setTitle(title)
                dlg.setCancelable(false)
                dlg.setPositiveButton(
                    button
                ) { dialog, which ->
                    dialog.dismiss()
                    if (exit) {
                        findNavController().popBackStack()
                    }
                }
                dlg.create()
                dlg.show()
            } catch (e: Exception) {
                findNavController().popBackStack()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun startActivityForResult(intent: Intent?, requestCode: Int, options: Bundle?) {

        // Capture requestCode here so that it is captured in the setActivityResultCallback() case.
        cordovaInterface?.setActivityResultRequestCode(requestCode)

        super.startActivityForResult(intent, requestCode, options)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        logD { "CORDOVA onActivityResult $requestCode $resultCode" }
        cordovaInterface?.onActivityResult(requestCode, resultCode, data)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        try {
            cordovaInterface?.onRequestPermissionResult(requestCode, permissions, grantResults)
        } catch (e: JSONException) {
            LOG.d(
                CordovaActivity.TAG,
                "JSONException: Parameters fed into the method are not valid"
            )
            e.printStackTrace()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {

        super.onConfigurationChanged(newConfig)
        val pm = appView.pluginManager
        pm?.onConfigurationChanged(newConfig)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        cordovaInterface?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        // CB-9382 If there is an activity that started for result and main activity is waiting for callback
        // result, we shoudn't stop WebView Javascript timers, as activity for result might be using them
        val keepRunning = keepRunning || cordovaInterface!!.activityResultCallback != null
        appView.handlePause(keepRunning)
    }

    override fun onResume() {
        appView.handleResume(keepRunning)
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        appView.handleStop()
    }

    override fun onStart() {
        super.onStart()
        appView.handleStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        @Language("js")
        val js = """
            function allStorage() {

                var values = [],
                    keys = Object.keys(window.localStorage),
                    i = keys.length;

                while ( i-- ) {
                    values.push( keys[i] + "--+++===+++-->>" +  window.localStorage.getItem(keys[i])  );
                }

                return values;
            }
            allStorage();
        """.trimIndent()
        (appView.view as? WebView)?.evaluateJavascript(js) {
            logD { "localStorage test: $it" }
            model.saveLocalStorage(it)
            WebStorage.getInstance().deleteAllData()
        }
        appView.handleDestroy()
        model.releaseApp()
    }


}