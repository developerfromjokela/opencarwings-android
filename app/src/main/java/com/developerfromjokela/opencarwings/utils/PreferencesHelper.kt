package com.developerfromjokela.opencarwings.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USERNAME = "username"
        private const val KEY_SERVER = "server"
        private const val KEY_ACTIVE_CAR_VIN = "active_car_vin"
        private const val KEY_WHATS_NEW = ""
    }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_ACCESS_TOKEN, value) }

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_REFRESH_TOKEN, value) }

    var server: String?
        get() = prefs.getString(KEY_SERVER, null)
        set(value) = prefs.edit { putString(KEY_SERVER, value) }

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit { putString(KEY_USERNAME, value) }

    var activeCarVin: String?
        get() = prefs.getString(KEY_ACTIVE_CAR_VIN, null)
        set(value) = prefs.edit { putString(KEY_ACTIVE_CAR_VIN, value) }

    var whatsNew: String?
        get() = prefs.getString(KEY_WHATS_NEW, "")
        set(value) = prefs.edit { putString(KEY_WHATS_NEW, value) }

    fun clearAll() {
        prefs.edit { clear() }
    }
}