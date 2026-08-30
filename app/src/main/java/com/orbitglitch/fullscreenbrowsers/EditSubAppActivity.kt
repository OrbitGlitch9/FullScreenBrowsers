package com.orbitglitch.fullscreenbrowsers

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.orbitglitch.fullscreenbrowsers.data.AppRepository
import com.orbitglitch.fullscreenbrowsers.data.SubApp
import com.orbitglitch.fullscreenbrowsers.databinding.ActivityEditSubAppBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.UUID

/**
 * Shared activity for creating and modifying a [SubApp].
 * Launch without [EXTRA_SUB_APP_ID] for create mode; with an ID for edit mode.
 */
class EditSubAppActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditSubAppBinding
    private lateinit var repo: AppRepository
    private var existingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditSubAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = AppRepository(this)
        existingId = intent.getStringExtra(EXTRA_SUB_APP_ID)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = if (existingId == null) "Create Sub-App" else "Edit Sub-App"
        }

        existingId?.let { id ->
            repo.getById(id)?.let { app ->
                binding.etTitle.setText(app.title)
                binding.etUrl.setText(app.url)
                binding.etIconUrl.setText(app.iconUrl)
                binding.etVolumeDownJs.setText(app.volumeDownJs)
                binding.etVolumeUpJs.setText(app.volumeUpJs)
                if (app.iconUrl.isNotBlank()) loadIconFromUrl(app.iconUrl)
            }
        }

        binding.btnFetchIcon.setOnClickListener { fetchIcon() }
        binding.btnSave.setOnClickListener { save() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { onBackPressedDispatcher.onBackPressed(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun fetchIcon() {
        val iconUrl = binding.etIconUrl.text.toString().trim()
        if (iconUrl.isNotBlank()) {
            loadIconFromUrl(iconUrl)
            return
        }
        // Auto-derive favicon from page URL
        val pageUrl = binding.etUrl.text.toString().trim()
        if (pageUrl.isBlank()) { Toast.makeText(this, "Enter a URL first", Toast.LENGTH_SHORT).show(); return }
        val host = try { URL(if ("://" in pageUrl) pageUrl else "https://$pageUrl").host } catch (_: Exception) { null }
        if (host == null) { Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show(); return }
        val faviconUrl = "https://www.google.com/s2/favicons?domain=$host&sz=128"
        binding.etIconUrl.setText(faviconUrl)
        loadIconFromUrl(faviconUrl)
    }

    private fun loadIconFromUrl(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bmp = BitmapFactory.decodeStream(URL(url).openStream())
                withContext(Dispatchers.Main) {
                    if (bmp != null) binding.imgIconPreview.setImageBitmap(bmp)
                    else Toast.makeText(this@EditSubAppActivity, "Could not load icon", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditSubAppActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun save() {
        val title = binding.etTitle.text.toString().trim()
        val url = binding.etUrl.text.toString().trim()
        if (title.isBlank()) { binding.etTitle.error = "Required"; return }
        if (url.isBlank()) { binding.etUrl.error = "Required"; return }

        val app = SubApp(
            id = existingId ?: UUID.randomUUID().toString(),
            title = title,
            url = url,
            iconUrl = binding.etIconUrl.text.toString().trim(),
            volumeDownJs = binding.etVolumeDownJs.text.toString().trim(),
            volumeUpJs = binding.etVolumeUpJs.text.toString().trim()
        )
        repo.save(app)
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        const val EXTRA_SUB_APP_ID = "sub_app_id"
        fun intentForCreate(context: android.content.Context) =
            Intent(context, EditSubAppActivity::class.java)
        fun intentForEdit(context: android.content.Context, subAppId: String) =
            Intent(context, EditSubAppActivity::class.java).putExtra(EXTRA_SUB_APP_ID, subAppId)
    }
}