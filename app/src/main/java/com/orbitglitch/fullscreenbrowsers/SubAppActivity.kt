package com.orbitglitch.fullscreenbrowsers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.orbitglitch.fullscreenbrowsers.data.AppRepository
import com.orbitglitch.fullscreenbrowsers.data.SubApp

/**
 * Custom Display / Full-screen WebView activity.
 */
class SubAppActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var subApp: SubApp
    private lateinit var container: FrameLayout

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val subAppId = intent.getStringExtra(EXTRA_SUB_APP_ID)
            ?: intent.data?.lastPathSegment
            ?: run { finish(); return }

        subApp = AppRepository(this).getById(subAppId) ?: run { finish(); return }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        @Suppress("DEPRECATION")
        setTaskDescription(
            android.app.ActivityManager.TaskDescription(subApp.title)
        )

        container = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        webView = WebView(this).also { wv ->
            wv.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false
            }
            wv.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false
            }
            wv.webChromeClient = WebChromeClient()

            if (subApp.fixDoubleClick) {
                wv.setOnTouchListener(OnTouchListenerFixDoubleClick(subApp.doubleClickThresholdMs.toLong()))
            }
        }

        container.addView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(container)

        applyPunchHolePadding()
        applyDisplayToggles()

        val urlToLoad = subApp.url.ifBlank { "about:blank" }
        webView.loadUrl(if ("://" in urlToLoad) urlToLoad else "https://$urlToLoad")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    private fun applyPunchHolePadding() {
        val density = resources.displayMetrics.density
        val paddingPx = (subApp.punchHolePadding * density).toInt()

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val leftPx = if (isLandscape) paddingPx else 0
        val topPx = if (!isLandscape) paddingPx else 0

        container.setPadding(leftPx, topPx, 0, 0)
    }

    private fun applyDisplayToggles() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (subApp.hideNavigationBar) {
            windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
        }

        if (subApp.hideStatusBar) {
            windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyPunchHolePadding()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> subApp.volumeDownJs.takeIf { it.isNotBlank() }?.let { js ->
                webView.evaluateJavascript(js, null); return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> subApp.volumeUpJs.takeIf { it.isNotBlank() }?.let { js ->
                webView.evaluateJavascript(js, null); return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private class OnTouchListenerFixDoubleClick(private val thresholdMs: Long) : View.OnTouchListener {
        private var prevDownTime: Long = 0L
        private var prevX: Float = 0.0f
        private var prevY: Float = 0.0f

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            val action = motionEvent.action
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
                val curX = motionEvent.x
                val curY = motionEvent.y
                val curDownTime = motionEvent.downTime
                val diff = curDownTime - prevDownTime

                if (prevX == curX && prevY == curY && diff < thresholdMs) {
                    Log.d("SubAppTouch", "Ignoring duplicate touch | diff: $diff ms (threshold: $thresholdMs ms)")
                    return true
                }

                if (action == MotionEvent.ACTION_UP) {
                    prevX = curX
                    prevY = curY
                    prevDownTime = curDownTime
                }
            }
            return false
        }
    }

    companion object {
        const val EXTRA_SUB_APP_ID = "sub_app_id"

        fun buildIntent(context: Context, subAppId: String): Intent =
            Intent(context, SubAppActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("subapp://app/$subAppId")
                putExtra(EXTRA_SUB_APP_ID, subAppId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            }
    }
}