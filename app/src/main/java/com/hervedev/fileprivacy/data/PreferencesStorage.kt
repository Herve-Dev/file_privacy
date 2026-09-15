package com.hervedev.fileprivacy.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("file_privacy_prefs", Context.MODE_PRIVATE)

    var categoriesEnabled: Boolean
        get() = prefs.getBoolean("categories_enabled", true)
        set(value) = prefs.edit().putBoolean("categories_enabled", value).apply()
}
