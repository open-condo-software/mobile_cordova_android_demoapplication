package ai.doma.miniappdemo.presentation

import ai.doma.core.DI.CoreComponent

import ai.doma.core.system.permissions.requestPermissions

import ai.doma.miniappdemo.collectAndTrace
import ai.doma.miniappdemo.databinding.ActivitySimplePdfViewerBinding
import ai.doma.miniappdemo.ext.viewBinding
import ai.doma.miniappdemo.getViewScope

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.app.DownloadManager.Request
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import java.io.File

internal fun Context.pixels(dp: Int) = (dp * this.resources.displayMetrics.density + 0.5f).toInt()

data class PdfItem(
    var id: String,
    var title: String,
    val uri: String,
    var isDownloadable: Boolean,
)
class SimplePDFViewActivity : AppCompatActivity() {
    companion object {
        private const val UI_ANIMATION_DELAY = 300

        var onDownloadItem: ((activity: AppCompatActivity, item: PdfItem) -> Unit)? = null

        var currentItem: PdfItem? = null
        fun openViewer(
            activity: Activity,
            item: PdfItem,
            onDownloadItem: ((activity: AppCompatActivity, item: PdfItem) -> Unit)? = null
        ) {
            val intent = Intent(activity, SimplePDFViewActivity::class.java).apply {
                Companion.currentItem = item
                Companion.onDownloadItem = onDownloadItem
            }
            activity.startActivity(intent)
        }
    }


    val vb by viewBinding(ActivitySimplePdfViewerBinding::inflate)

    private val hideHandler = Handler(Looper.getMainLooper())

    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)



    }
    private val showPart2Runnable = Runnable {
        // Delayed display of UI elements
        actionBar?.show()
        vb.footer.isVisible = true
        vb.header.isVisible = true
    }
    private var isFullscreen: Boolean = false
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(vb.root)
        vb.container.isFocusableInTouchMode = true
        vb.container.isClickable = true
        vb.pdfView.setOnClickListener{
            toggle()
        }


        window.decorView.setOnApplyWindowInsetsListener { v, insets ->
            vb.header.layoutParams =
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    pixels(56) + insets.systemWindowInsetTop
                )
                    .apply {
                        gravity = Gravity.TOP
                    }
            vb.header.setPadding(0, insets.systemWindowInsetTop, 0, 0)

            vb.footer.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM
            }
            vb.footer.setPadding(0, 0, 0, insets.systemWindowInsetBottom)


            insets
        }

        initPdfViewer()

        with(vb.tvTitle) {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP,16f)
            setLineSpacing(4f, 1f)
            //translationY = - Resources.context.pixels(1.95f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lineHeight = pixels(23)
                firstBaselineToTopHeight = pixels(0)
                lastBaselineToBottomHeight = pixels(0)
            }
            setPadding(0, 0, 0, 0)
        }
        vb.tvTitle.setTextColor(Color.parseColor("#F2F3F7"))
        vb.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        show()
    }

    private fun toggle() {
        if (isFullscreen) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        actionBar?.hide()
        vb.footer.isVisible = false
        vb.header.isVisible = false
        isFullscreen = false

        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        isFullscreen = true
        hideHandler.removeCallbacks(hidePart2Runnable)
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun initPdfViewer() {

        val viewScope = vb.root.getViewScope()
        viewScope.launch {
            (flowOf(currentItem?.uri).filterNotNull())
                .flowOn(Dispatchers.IO)
                .collectAndTrace(onError = {
                    /* no-op */
                }) {
                    Glide.with(vb.pdfView.context).asFile()
                        .load(it)
                        .listener(object: RequestListener<File> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: com.bumptech.glide.request.target.Target<File>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                vb.pbWait.isVisible = false
                                return false
                            }

                            override fun onResourceReady(
                                resource: File?,
                                model: Any?,
                                target: com.bumptech.glide.request.target.Target<File>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                vb.pbWait.isVisible = false
                                if (resource != null) {
                                    vb.pdfView.fromFile(resource).show()
                                }
                                return false
                            }
                        })
                        .preload()
                }
        }
        initTransluentViews()
    }

    private fun initTransluentViews() {
        with(currentItem) {
            when (this) {
                is PdfItem -> {
                    vb.tvTitle.text = this.title
                }
            }
            vb.btnSave.isVisible = this?.isDownloadable == true

            vb.btnSave.setOnClickListener {
                this?.let {
                    onDownloadItem?.invoke(this@SimplePDFViewActivity, it)
                    downloadFile(it)
                }
            }
        }
    }

    private fun downloadFile(item: PdfItem) {
        vb.root.getViewScope().launch {
            if (Build.VERSION.SDK_INT <= 28) {
                requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).mapLatest {
                    it.getOrNull(0)?.isGranted == true
                }.flowOn(Dispatchers.Main)
            } else {
                flowOf(true)
            }.flatMapLatest {
                if(it){
                    flowOf(item.uri)
                } else {
                    flowOf("")
                }
            }
                .flowOn(Dispatchers.IO)
                .collectAndTrace { uri ->
                    if (uri.isNullOrBlank()) return@collectAndTrace
                    val mgr = getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                        ?: return@collectAndTrace
                    val request = DownloadManager.Request(Uri.parse(uri))
                    request.setAllowedNetworkTypes(Request.NETWORK_WIFI or Request.NETWORK_MOBILE)
                    request.setTitle(item.title)
                    request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, item.title)
                    val downloadID = mgr.enqueue(request)
                }
        }
    }
}