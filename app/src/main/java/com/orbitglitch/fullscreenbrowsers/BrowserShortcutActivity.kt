package com.orbitglitch.fullscreenbrowsers

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Trampoline activity that home-screen shortcuts point to.
 * Shortcuts carry the sub-app ID as a URI segment so Android can
 * distinguish individual shortcuts by their data URI.
 */
class BrowserShortcutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val subAppId = intent?.data?.lastPathSegment
        if (subAppId != null) {
            startActivity(SubAppActivity.buildIntent(this, subAppId))
        }
        finish()
    }

    companion object {
        const val SHORTCUT_DATA_SCHEME = "subapp"
    }
}