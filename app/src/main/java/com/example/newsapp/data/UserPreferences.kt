package com.example.newsapp.data

import android.content.Context

object UserPreferences {

    private const val PREFS_NAME = "news_app_prefs"
    private const val KEY_NIGHT_MODE = "pref_night_mode"
    private const val KEY_NOTIFICATIONS = "pref_notifications"
    private const val KEY_FIRST_NAME = "pref_first_name"
    private const val KEY_LAST_NAME = "pref_last_name"
    private const val KEY_EMAIL = "pref_email"
    private const val KEY_PASSWORD = "pref_password"

    data class Profile(
        val firstName: String,
        val lastName: String,
        val email: String,
        val password: String
    )

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

    fun saveProfile(context: Context, profile: Profile) {
        prefs(context).edit()
            .putString(KEY_FIRST_NAME, profile.firstName)
            .putString(KEY_LAST_NAME, profile.lastName)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_PASSWORD, profile.password)
            .apply()
    }

    fun getProfile(context: Context, defaults: Profile): Profile {
        val shared = prefs(context)
        return Profile(
            firstName = shared.getString(KEY_FIRST_NAME, defaults.firstName) ?: defaults.firstName,
            lastName = shared.getString(KEY_LAST_NAME, defaults.lastName) ?: defaults.lastName,
            email = shared.getString(KEY_EMAIL, defaults.email) ?: defaults.email,
            password = shared.getString(KEY_PASSWORD, defaults.password) ?: defaults.password
        )
    }
}
