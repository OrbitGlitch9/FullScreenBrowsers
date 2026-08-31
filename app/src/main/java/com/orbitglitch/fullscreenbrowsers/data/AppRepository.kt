package com.orbitglitch.fullscreenbrowsers.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists [SubApp] list as JSON in SharedPreferences.
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

    fun exportJson(): String {
        val arr = JSONArray()
        getAll().forEach { arr.put(it.toJson()) }
        return JSONObject().put("sub_apps", arr).toString(2)
    }

    fun importJson(json: String): Boolean {
        return try {
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("sub_apps")
            val list = (0 until arr.length()).map { arr.getJSONObject(it).toSubApp() }
            persist(list)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun SubApp.toJson() = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("url", url)
        put("iconUrl", iconUrl)
        put("hideNavigationBar", hideNavigationBar)
        put("hideStatusBar", hideStatusBar)
        put("punchHolePadding", punchHolePadding)
        put("fixDoubleClick", fixDoubleClick)
        put("doubleClickThresholdMs", doubleClickThresholdMs)
        put("volumeDownJs", volumeDownJs)
        put("volumeUpJs", volumeUpJs)
    }

    private fun JSONObject.toSubApp() = SubApp(
        id = optString("id"),
        title = optString("title"),
        url = optString("url"),
        iconUrl = optString("iconUrl"),
        hideNavigationBar = optBoolean("hideNavigationBar", true),
        hideStatusBar = optBoolean("hideStatusBar", true),
        punchHolePadding = if (has("punchHolePadding")) optInt("punchHolePadding", 0) else if (optBoolean("fillPunchHole", true)) 0 else 24,
        fixDoubleClick = optBoolean("fixDoubleClick", true),
        doubleClickThresholdMs = optInt("doubleClickThresholdMs", 150),
        volumeDownJs = optString("volumeDownJs"),
        volumeUpJs = optString("volumeUpJs")
    )

    companion object { private const val KEY_LIST = "list" }
}