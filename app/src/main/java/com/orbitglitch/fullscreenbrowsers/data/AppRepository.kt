package com.orbitglitch.fullscreenbrowsers.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists [SubApp] list as JSON in SharedPreferences.
 * Export/import stubs are ready for future cloud or file-based backup.
 */
class AppRepository(context: Context) {

    private val prefs = context.getSharedPreferences("sub_apps", Context.MODE_PRIVATE)

    fun getAll(): List<SubApp> {
        val json = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getJSONObject(it).toSubApp() }
        } catch (_: Exception) { emptyList() }
    }

    fun save(app: SubApp) {
        val list = getAll().toMutableList()
        val idx = list.indexOfFirst { it.id == app.id }
        if (idx >= 0) list[idx] = app else list.add(app)
        persist(list)
    }

    fun delete(id: String) = persist(getAll().filterNot { it.id == id })

    fun getById(id: String): SubApp? = getAll().firstOrNull { it.id == id }

    private fun persist(list: List<SubApp>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_LIST, arr.toString()).apply()
    }

    /** Serialises the full configuration to a JSON string for backup. */
    fun exportJson(): String {
        val arr = JSONArray()
        getAll().forEach { arr.put(it.toJson()) }
        return JSONObject().put("sub_apps", arr).toString(2)
    }

    /** Restores the full configuration from a previously exported JSON string. */
    fun importJson(json: String) {
        try {
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("sub_apps")
            persist((0 until arr.length()).map { arr.getJSONObject(it).toSubApp() })
        } catch (_: Exception) { /* surface to UI later */ }
    }

    private fun SubApp.toJson() = JSONObject().apply {
        put("id", id); put("title", title); put("url", url)
        put("iconUrl", iconUrl); put("volumeDownJs", volumeDownJs); put("volumeUpJs", volumeUpJs)
    }

    private fun JSONObject.toSubApp() = SubApp(
        id = optString("id"), title = optString("title"), url = optString("url"),
        iconUrl = optString("iconUrl"), volumeDownJs = optString("volumeDownJs"),
        volumeUpJs = optString("volumeUpJs")
    )

    companion object { private const val KEY_LIST = "list" }
}