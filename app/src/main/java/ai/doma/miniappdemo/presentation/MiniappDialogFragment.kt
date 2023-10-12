package ai.doma.feature_miniapps.presentation.view

import ai.doma.core.DI.CoreComponent
import ai.doma.core.DI.InjectHelper
import ai.doma.core.system.permissions.requestPermissions
import ai.doma.feature_miniapps.DI.DaggerMiniappsFeatureComponent
import ai.doma.feature_miniapps.DI.MiniappsFeatureComponent
import ai.doma.feature_miniapps.presentation.viewmodel.MiniappViewModel
import ai.doma.feature_miniapps.presentation.viewmodel.MiniappViewModelFactory


import ai.doma.miniappdemo.base.BaseDialog
import ai.doma.miniappdemo.databinding.FragmentFlowMiniappBinding
import android.app.AlertDialog
import android.content.DialogInterface
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
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
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
import ai.doma.miniappdemo.domain.MiniappBackStack
import ai.doma.miniappdemo.domain.MiniappEntity
import ai.doma.miniappdemo.ext.gone
import ai.doma.miniappdemo.ext.logD
import ai.doma.miniappdemo.ext.pixels
import ai.doma.miniappdemo.ext.show
import ai.doma.miniappdemo.ext.showFragment
import ai.doma.miniappdemo.ext.updatePadding
import ai.doma.miniappdemo.getViewScope
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.view.KeyEvent
import android.webkit.ValueCallback
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.math.MathUtils
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import org.apache.cordova.engine.SystemWebViewEngine
import org.intellij.lang.annotations.Language


const val DEFAULT_NAV_BAR_HEIGHT_DP = 60


class MiniappDialogFragment : BaseDialog() {

    override val layout: Int
        get() = R.layout.fragment_flow_miniapp

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            miniappEntity: MiniappEntity,
            presentationStyle: String
        ) {
            val dialog = MiniappDialogFragment().apply {
                this.miniappEntity = miniappEntity
                this.presentationStyle = presentationStyle
                this.payload = payload
            }
            fragmentManager.showFragment(dialog)
        }
    }

    val vb by lazy { FragmentFlowMiniappBinding.bind(requireView().findViewById(R.id.miniappRoot)) }
    lateinit var miniappEntity: MiniappEntity
    lateinit var presentationStyle: String
    var payload: String? = null

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

    var savedInstanceState: Bundle? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        loadConfig()
        miniappFeatureComponent = DaggerMiniappsFeatureComponent.builder()
            .coreComponent(CoreComponent.get())
            .build()
        miniappFeatureComponent.inject(this)
        model = ViewModelProvider(this, factory)[MiniappViewModel::class.java]

        super.onCreate(savedInstanceState)
        this.savedInstanceState = savedInstanceState

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setWindowAnimations(R.style.DialogAnimation)

        appView = makeWebView()
        appView.view.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        )
        appView.view.id = View.generateViewId()
        appView.view.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        vb.root.updatePadding(top = requireContext().pixels(48))
        vb.webViewContainer.addView(appView.view, 0)
        appView.view.requestFocusFromTouch()
        cordovaInterface = makeCordovaInterface()
        savedInstanceState?.let {
            miniappEntity = it.getSerializable("miniappEntity") as MiniappEntity
            presentationStyle = it.getString("presentationStyle").orEmpty()

            cordovaInterface?.restoreInstanceState(it)
            logD {
                "CORDOVA savedInstanceState callbackService: ${it.getString("callbackService")} plugin: ${
                    it.getBundle(
                        "plugin"
                    )?.toString()
                }"
            }
        }

        logD { "CORDOVA ${savedInstanceState.toString()}" }
        init()
        model.miniappName = miniappEntity.name.orEmpty()
        model.miniappId = miniappEntity.id
        model.version = miniappEntity.version
        model.loadApp()

        requireView().getViewScope().launch {
            model.miniapp.flatMapLatest { (file, config) ->
                if (config.requestedPermissions.isNotEmpty()) {
                    requestPermissions(*config.requestedPermissions.toTypedArray()).map { file }
                } else {
                    flowOf(file)
                }
            }.collectAndTrace {
                savedInstanceState?.let { return@collectAndTrace }
                appView.preferences.set("AndroidInsecureFileModeEnabled", true)
                MiniappBackStack.reset()
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

        vb.pbWait.indeterminateTintList = ColorStateList.valueOf(Color.parseColor("#000000"))
        vb.pbWait.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#82879F"))

        vb.ivClose.isVisible = presentationStyle in listOf(
            "push_fullscreen_with_navigation",
            "present_fullscreen_with_navigation"
        )


        val set = ConstraintSet()
        set.clone(vb.root)
        set.connect(
            vb.webViewContainer.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP,
            if (presentationStyle in listOf(
                    "push_fullscreen_with_navigation",
                    "present_fullscreen_with_navigation",
                    "native"
                )
            ) {
                requireContext().pixels(DEFAULT_NAV_BAR_HEIGHT_DP)
            } else 0
        )
        set.connect(
            vb.webViewContainer.id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            0
        )
        set.connect(
            vb.webViewContainer.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            0
        )
        set.connect(
            vb.webViewContainer.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            0
        )
        set.applyTo(vb.root)

        vb.ivClose.setOnClickListener {
            dismiss()
        }
        vb.ivClose2.setOnClickListener {
            dismiss()
        }
        vb.ivBack.setOnClickListener {
            appView.engine.evaluateJavascript(
                """cordova.fireDocumentEvent('backbutton');""",
                ValueCallback {
                    logD { it }
                })
            MiniappBackStack.pop()
            sendHistoryStateToJs()
        }
        ((vb.ivClose.drawable as? RippleDrawable)?.getDrawable(1) as? LayerDrawable)?.let {
            (it.getDrawable(0) as? GradientDrawable)?.setColor(Color.parseColor("#F2F3F7"))
            (it.getDrawable(1) as? Drawable)?.colorFilter =
                PorterDuffColorFilter(Color.parseColor("#222222"), PorterDuff.Mode.SRC_IN)
        }
        ((vb.ivClose2.drawable as? RippleDrawable)?.getDrawable(1) as? LayerDrawable)?.let {
            (it.getDrawable(0) as? GradientDrawable)?.setColor(Color.parseColor("#F2F3F7"))
            (it.getDrawable(1) as? Drawable)?.colorFilter =
                PorterDuffColorFilter(Color.parseColor("#222222"), PorterDuff.Mode.SRC_IN)
        }


        if (presentationStyle == "native") {
            initNativeNavigationUI()
            view.getViewScope().launch {
                MiniappBackStack.backstack.collectAndTrace {
                    vb.ivClose2.isVisible = it.isEmpty()
                    vb.ivBack.isVisible = it.isNotEmpty()
                    logD { """stack: ${it.joinToString(separator = "\n") { it.toString() }}""" }
                    vb.tvTitleColapsed.text = it.lastOrNull()?.name
                }
            }
        }
        vb.tvTitleColapsed.setTextColor(Color.parseColor("#222222"))


        dialog?.setOnKeyListener { dialog, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK &&
                event.action == KeyEvent.ACTION_UP &&
                !event.isCanceled
            ) {
                if(presentationStyle != "native" ) return@setOnKeyListener false
                if (MiniappBackStack.backstack.value.isEmpty()) {
                    dismiss()
                } else {
                    appView.engine.evaluateJavascript(
                        """cordova.fireDocumentEvent('backbutton');""",
                        ValueCallback {
                            logD { it }
                        })
                    MiniappBackStack.pop()
                    sendHistoryStateToJs()
                }
                true
            } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                presentationStyle == "native"
            } else false
        }
    }


    private fun initNativeNavigationUI() {
        vb.ivClose2.show()
        vb.shadowBar.show()
        (appView.view as? WebView)?.apply {
            //appView.view.setBackgroundResource(R.drawable.shape_white_bg)
            val scrollDistanceMax = requireContext().pixels(20).toFloat()
            this.viewTreeObserver.addOnPreDrawListener {
                val transitionProgress =
                    MathUtils.clamp(this.scrollY / scrollDistanceMax, 0.0f, 1.0f)
                vb.shadowBar.elevation = requireContext().pixels(8).toFloat() * transitionProgress

                logD { "${System.currentTimeMillis()}  shadowBar.update()" }
                true
            }

        }
    }


    private fun init() {
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
        CondoPluginState.payload = this.payload
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
                .orEmpty() //+ model.inflateGlobalVariablesJs()

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
            webview?.webViewClient = object :
                org.apache.cordova.engine.SystemWebViewClient(engine!! as SystemWebViewEngine) {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                }

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
                return this@MiniappDialogFragment.onMessage(id, data)
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
                dismiss()
            }

            "onPageFinished" -> {
                if (model.awaitLocalStorageInflate) {
                    model.awaitLocalStorageInflate = false
                } else {
                    if (model.awaitMiniappFirstLoad) {
                        model.awaitMiniappFirstLoad = false
                        appView.clearHistory()
                        appView.stopLoading()
                    }
                    vb.pbWait.gone()
                }
                logD { "CORDOVA onPageFinished" }
            }

            Condo.ACTION_CLOSE_MINIAPP -> {
                vb.root.post {
                    dismiss()
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
        outState.putSerializable("miniappEntity", miniappEntity)
        outState.putString("presentationStyle", presentationStyle)
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


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

    }

    fun sendHistoryStateToJs() {
        val state = MiniappBackStack.backstack.value.lastOrNull()?.state
        val stateStr = if (state is String) {
            """"$state""""
        } else state.toString()
        this.appView.engine.evaluateJavascript(
            """window.dispatchEvent(new PopStateEvent('condoPopstate', { 'state': $stateStr}));""",
            ValueCallback {
                logD { it }
            })
    }

}