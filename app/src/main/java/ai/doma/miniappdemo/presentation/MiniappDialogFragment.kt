package ai.doma.feature_miniapps.presentation.view

import ai.doma.core.DI.CoreComponent
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
import ai.doma.miniappdemo.ext.logD
import ai.doma.miniappdemo.getViewScope
import android.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class MiniappDialogFragment : BaseDialog() {

    override val layout: Int
        get() = R.layout.fragment_flow_miniapp

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            miniappId: String,
            presentationStyle: String
        ) {
            val dialog = MiniappDialogFragment().apply {
                this.miniappId = miniappId
                this.presentationStyle = presentationStyle
            }
            //fragmentManager.showFragment(dialog)
        }
    }

    val vb by lazy { FragmentFlowMiniappBinding.bind(requireView().findViewById(R.id.miniappRoot)) }
    lateinit var miniappId: String
    lateinit var presentationStyle: String

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
        //dialog?.window?.setWindowAnimations(R.style.DialogAnimation)

        appView = makeWebView()
        appView.view.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        )
        vb.root.addView( appView.view, 0)
        appView.view.requestFocusFromTouch()
        cordovaInterface = makeCordovaInterface()
        savedInstanceState?.let {
            cordovaInterface?.restoreInstanceState(it)
            logD{"CORDOVA savedInstanceState callbackService: ${it.getString("callbackService")} plugin: ${it.getBundle("plugin")?.toString()}"}
        }

        logD { "CORDOVA ${savedInstanceState.toString()}" }
        init()
        model.miniappId = miniappId
        model.loadApp()

        requireView().getViewScope().launch {
            model.miniapp.flatMapLatest {(file, config) ->
                requestPermissions(*config.requestedPermissions.toTypedArray()).map { file }
            }.collectAndTrace {
                savedInstanceState?.let { return@collectAndTrace }
                appView.preferences.set("AndroidInsecureFileModeEnabled", true)

                appView.loadUrlIntoView(it.path + "/www/index.html",true)
            }
        }
        savedInstanceState?.let {
            MiniappRepository.getMiniapp(requireContext(), model.miniappId)?.let {
                init()
                appView.preferences.set("AndroidInsecureFileModeEnabled", true)
                appView.loadUrlIntoView(it.path + "/www/index.html",false)
            }
        }

        vb.pbWait.indeterminateTintList = ColorStateList.valueOf(Color.parseColor("#000000"))
        vb.pbWait.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#82879F"))

        vb.ivClose2.isVisible = presentationStyle in listOf("push_fullscreen_with_navigation", "present_fullscreen_with_navigation")

        val set = ConstraintSet()
        set.clone(vb.root)
        set.connect(
            appView.view.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP,
            if (vb.ivClose2.isVisible) {
                (requireContext().resources.displayMetrics.density * 60 + 0.5f).toInt()
            } else 0
        )
        set.connect(
            appView.view.id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            0
        )
        set.connect(
            appView.view.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            0
        )
        set.connect(
            appView.view.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            0
        )
        set.applyTo(vb.root)

        vb.ivClose2.setOnClickListener {
            dismiss()
        }
        vb.ivClose2.imageTintList = ColorStateList.valueOf(Color.parseColor("#000000"))
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
        val parser = ConfigXmlParser()
        parser.parse(requireContext())
        preferences = parser.preferences;
        preferences.setPreferencesBundle(requireActivity().intent.extras)
        launchUrl = parser.launchUrl
        pluginEntries = parser.pluginEntries
        //Config.parser = parser
    }

    private fun makeWebView(): CordovaWebView {
        return CordovaWebViewImpl(makeWebViewEngine())
    }

    private fun makeWebViewEngine(): CordovaWebViewEngine? {
        return CordovaWebViewImpl.createEngine(requireContext(), preferences)
    }

    private fun makeCordovaInterface(): CordovaFragmentInterfaceImpl {
        return object : CordovaFragmentInterfaceImpl(requireActivity() as AppCompatActivity, this) {
            override fun onMessage(id: String, data: Any): Any? {
                // Plumb this to CordovaActivity.onMessage for backwards compatibility
                return this@MiniappDialogFragment.onMessage(id, data)
            }
        }
    }

    fun onMessage(id: String, data: Any): Any? {
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
                vb.pbWait.isVisible = false
                logD { "CORDOVA onPageFinished" }
            }
            Condo.ACTION_CLOSE_MINIAPP -> {
                dismiss()
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

    override fun startActivityForResult(intent: Intent?, requestCode: Int, options: Bundle?) {

        // Capture requestCode here so that it is captured in the setActivityResultCallback() case.
        cordovaInterface?.setActivityResultRequestCode(requestCode)

        super.startActivityForResult(intent, requestCode, options)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        logD { "CORDOVA onActivityResult $requestCode $resultCode" }
        cordovaInterface?.onActivityResult(requestCode, resultCode, data)
    }

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
        appView.handleDestroy()
        model.releaseApp()
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        //(activity as BottomNavViewContainer).showBottomMenu()
    }

}