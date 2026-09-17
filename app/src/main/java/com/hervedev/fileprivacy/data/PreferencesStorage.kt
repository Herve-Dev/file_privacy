package com.hervedev.fileprivacy.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("file_privacy_prefs", Context.MODE_PRIVATE)

    var categoriesEnabled: Boolean
        get() = prefs.getBoolean("categories_enabled", true)
        set(value) = prefs.edit().putBoolean("categories_enabled", value).apply()

    var recentsEnabled: Boolean
        get() = prefs.getBoolean("recents_enabled", true)
        set(value) = prefs.edit().putBoolean("recents_enabled", value).apply()

    var trashAutoCleanEnabled: Boolean
        get() = prefs.getBoolean("trash_auto_clean_enabled", false)
        set(value) = prefs.edit().putBoolean("trash_auto_clean_enabled", value).apply()

    var trashAutoCleanDays: Int
        get() = prefs.getInt("trash_auto_clean_days", 30)
        set(value) = prefs.edit().putInt("trash_auto_clean_days", value).apply()

    var showHiddenFiles: Boolean
        get() = prefs.getBoolean("show_hidden_files", false)
        set(value) = prefs.edit().putBoolean("show_hidden_files", value).apply()

    var defaultViewMode: String
        get() = prefs.getString("default_view_mode", "LIST") ?: "LIST"
        set(value) = prefs.edit().putString("default_view_mode", value).apply()

    var defaultSortOrder: String
        get() = prefs.getString("default_sort_order", "NAME_ASC") ?: "NAME_ASC"
        set(value) = prefs.edit().putString("default_sort_order", value).apply()
}
