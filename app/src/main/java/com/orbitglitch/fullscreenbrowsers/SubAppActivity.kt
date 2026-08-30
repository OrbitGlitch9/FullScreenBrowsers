package com.orbitglitch.fullscreenbrowsers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.orbitglitch.fullscreenbrowsers.data.AppRepository
import com.orbitglitch.fullscreenbrowsers.data.SubApp

/**
 * Full-screen WebView activity.
 *
 * Each sub-app is launched with [FLAG_ACTIVITY_NEW_DOCUMENT] + a unique data URI,
 * so every sub-app gets its own entry in the system Recent Tasks screen.
 * [documentLaunchMode="intoExisting"] in the manifest ensures re-launching the
 * same sub-app re-enters the existing task instead of creating a duplicate.
 *
 * Volume keys trigger optional JavaScript injection if configured.
 */
class SubAppActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var subApp: SubApp

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ID is carried both in the data URI and as an extra (extra is the reliable source).
        val subAppId = intent.getStringExtra(EXTRA_SUB_APP_ID)
            ?: intent.data?.lastPathSegment   // fallback: read from URI
            ?: run { finish(); return }

        subApp = AppRepository(this).getById(subAppId) ?: run { finish(); return }

        // Set the task description (label shown in Recents) to the sub-app's title.
        setTaskDescription(
            @Suppress("DEPRECATION")
            android.app.ActivityManager.TaskDescription(subApp.title)
        )

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

            @Suppress("DEPRECATION")
            wv.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }

        setContentView(webView)

        val urlToLoad = subApp.url.ifBlank { "about:blank" }
        webView.loadUrl(if ("://" in urlToLoad) urlToLoad else "https://$urlToLoad")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
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

    companion object {
        const val EXTRA_SUB_APP_ID = "sub_app_id"

        /**
         * Builds an intent that opens (or re-focuses) the sub-app in its own
         * Recent Tasks entry via [Intent.FLAG_ACTIVITY_NEW_DOCUMENT].
         *
         * The unique data URI `subapp://app/<id>` is what Android uses to key
         * the document task — each distinct URI = distinct Recents entry.
         */
        fun buildIntent(context: Context, subAppId: String): Intent =
            Intent(context, SubAppActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("subapp://app/$subAppId")
                putExtra(EXTRA_SUB_APP_ID, subAppId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            }
    }
}