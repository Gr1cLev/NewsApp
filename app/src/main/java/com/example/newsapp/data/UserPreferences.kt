package com.example.newsapp.data

import android.content.Context

object UserPreferences {

    private const val PREFS_NAME = "news_app_prefs"
    private const val KEY_NIGHT_MODE = "pref_night_mode"
    private const val KEY_NOTIFICATIONS = "pref_notifications"
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
}
