package com.orbitglitch.fullscreenbrowsers

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.orbitglitch.fullscreenbrowsers.data.AppRepository
import com.orbitglitch.fullscreenbrowsers.data.SubApp
import com.orbitglitch.fullscreenbrowsers.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: AppRepository
    private val items = mutableListOf<SubApp>()
    private lateinit var adapter: SubAppAdapter

    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> if (result.resultCode == Activity.RESULT_OK) refresh() }

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { saveListToUri(it) }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { loadListFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        repo = AppRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "FullScreen Browsers"

        adapter = SubAppAdapter()
        binding.listView.adapter = adapter

        binding.listView.setOnItemClickListener { _, _, position, _ ->
            startActivity(SubAppActivity.buildIntent(this, items[position].id))
        }

        binding.fabCreate.setOnClickListener {
            editLauncher.launch(EditSubAppActivity.intentForCreate(this))
        }

        binding.btnSaveList.setOnClickListener {
            exportLauncher.launch("sub_apps_backup.json")
        }

        binding.btnLoadList.setOnClickListener {
            importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
        }

        refresh()
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        items.clear()
        items.addAll(repo.getAll())
        adapter.notifyDataSetChanged()
        binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun saveListToUri(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val jsonString = repo.exportJson()
                outputStream.write(jsonString.toByteArray(StandardCharsets.UTF_8))
            }
            Toast.makeText(this, "Configuration saved successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadListFromUri(uri: Uri) {
        try {
            val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
            }
            if (jsonString != null && repo.importJson(jsonString)) {
                refresh()
                Toast.makeText(this, "Configuration loaded successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Invalid JSON configuration file", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    inner class SubAppAdapter : ArrayAdapter<SubApp>(this, 0, items) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_sub_app, parent, false)
            val app = items[position]
            view.findViewById<TextView>(R.id.tvTitle).text = app.title
            view.findViewById<TextView>(R.id.tvUrl).text = app.url

            val ivIcon = view.findViewById<ImageView>(R.id.ivIcon)
            ivIcon.setImageResource(android.R.drawable.ic_menu_gallery)
            if (app.iconUrl.isNotBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val bmp = BitmapFactory.decodeStream(URL(app.iconUrl).openStream())
                        withContext(Dispatchers.Main) { if (bmp != null) ivIcon.setImageBitmap(bmp) }
                    } catch (_: Exception) {}
                }
            }

            view.findViewById<View>(R.id.btnMenu).setOnClickListener { v -> showMenu(v, app) }
            return view
        }
    }

    private fun showMenu(anchor: View, app: SubApp) {
        PopupMenu(this, anchor).apply {
            inflate(R.menu.menu_sub_app_item)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_modify -> { editLauncher.launch(EditSubAppActivity.intentForEdit(this@MainActivity, app.id)); true }
                    R.id.action_copy   -> {
                        repo.save(app.copy(id = UUID.randomUUID().toString(), title = "${app.title} (copy)"))
                        refresh(); true
                    }
                    R.id.action_add_to_home -> { addToHomeScreen(app); true }
                    R.id.action_delete -> { repo.delete(app.id); refresh(); true }
                    else -> false
                }
            }
            show()
        }
    }

    private fun addToHomeScreen(app: SubApp) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            Toast.makeText(this, "Launcher does not support pin shortcuts", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = android.content.Intent(this, BrowserShortcutActivity::class.java).apply {
            action = android.content.Intent.ACTION_VIEW
            data = Uri.parse("${BrowserShortcutActivity.SHORTCUT_DATA_SCHEME}://launch/${app.id}")
        }

        fun buildAndPin(icon: IconCompat) {
            val info = ShortcutInfoCompat.Builder(this, "shortcut_${app.id}")
                .setShortLabel(app.title)
                .setLongLabel(app.title)
                .setIcon(icon)
                .setIntent(intent)
                .build()
            ShortcutManagerCompat.requestPinShortcut(this, info, null)
        }

        if (app.iconUrl.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                val bmp = try { BitmapFactory.decodeStream(URL(app.iconUrl).openStream()) } catch (_: Exception) { null }
                withContext(Dispatchers.Main) {
                    buildAndPin(
                        if (bmp != null) IconCompat.createWithBitmap(bmp)
                        else IconCompat.createWithResource(this@MainActivity, android.R.drawable.ic_menu_gallery)
                    )
                }
            }
        } else {
            buildAndPin(IconCompat.createWithResource(this, android.R.drawable.ic_menu_gallery))
        }
    }
}