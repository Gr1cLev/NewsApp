package com.example.newsapp.data

import android.content.Context

object UserPreferences {

    private const val PREFS_NAME = "news_app_prefs"
    private const val KEY_NIGHT_MODE = "pref_night_mode"
    private const val KEY_NOTIFICATIONS = "pref_notifications"
    private const val KEY_BG_URI = "pref_bg_uri"
    private const val KEY_BG_ALPHA_LEVEL = "pref_bg_alpha_level" // 0=low,1=medium,2=high
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isNightModeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NIGHT_MODE, false)

    fun setNightModeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NIGHT_MODE, enabled).apply()
    }

    fun isNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFICATIONS, true)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun getBackgroundUri(context: Context): String? =
        prefs(context).getString(KEY_BG_URI, null)

    fun setBackgroundUri(context: Context, uri: String?) {
        prefs(context).edit().apply {
            if (uri.isNullOrBlank()) remove(KEY_BG_URI) else putString(KEY_BG_URI, uri)
        }.apply()
    }

    fun getBackgroundAlphaLevel(context: Context): Int =
        prefs(context).getInt(KEY_BG_ALPHA_LEVEL, 1).coerceIn(0, 2)

    fun setBackgroundAlphaLevel(context: Context, level: Int) {
        prefs(context).edit().putInt(KEY_BG_ALPHA_LEVEL, level.coerceIn(0, 2)).apply()
    }
}
