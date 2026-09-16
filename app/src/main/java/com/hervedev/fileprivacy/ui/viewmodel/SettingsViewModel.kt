package com.hervedev.fileprivacy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.hervedev.fileprivacy.data.PreferencesStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesStorage = PreferencesStorage(application)

    private val _trashAutoCleanEnabled = MutableStateFlow(preferencesStorage.trashAutoCleanEnabled)
    val trashAutoCleanEnabled: StateFlow<Boolean> = _trashAutoCleanEnabled.asStateFlow()

    private val _trashAutoCleanDays = MutableStateFlow(preferencesStorage.trashAutoCleanDays)
    val trashAutoCleanDays: StateFlow<Int> = _trashAutoCleanDays.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(preferencesStorage.showHiddenFiles)
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()

    private val _defaultViewMode = MutableStateFlow(preferencesStorage.defaultViewMode)
    val defaultViewMode: StateFlow<String> = _defaultViewMode.asStateFlow()

    private val _defaultSortOrder = MutableStateFlow(preferencesStorage.defaultSortOrder)
    val defaultSortOrder: StateFlow<String> = _defaultSortOrder.asStateFlow()

    private val _categoriesEnabled = MutableStateFlow(preferencesStorage.categoriesEnabled)
    val categoriesEnabled: StateFlow<Boolean> = _categoriesEnabled.asStateFlow()

    private val _recentsEnabled = MutableStateFlow(preferencesStorage.recentsEnabled)
    val recentsEnabled: StateFlow<Boolean> = _recentsEnabled.asStateFlow()

    fun setTrashAutoCleanEnabled(enabled: Boolean) {
        preferencesStorage.trashAutoCleanEnabled = enabled
        _trashAutoCleanEnabled.value = enabled
    }

    fun setTrashAutoCleanDays(days: Int) {
        preferencesStorage.trashAutoCleanDays = days
        _trashAutoCleanDays.value = days
    }

    fun setShowHiddenFiles(show: Boolean) {
        preferencesStorage.showHiddenFiles = show
        _showHiddenFiles.value = show
    }

    fun setDefaultViewMode(mode: String) {
        preferencesStorage.defaultViewMode = mode
        _defaultViewMode.value = mode
    }

    fun setDefaultSortOrder(order: String) {
        preferencesStorage.defaultSortOrder = order
        _defaultSortOrder.value = order
    }

    fun setCategoriesEnabled(enabled: Boolean) {
        preferencesStorage.categoriesEnabled = enabled
        _categoriesEnabled.value = enabled
    }

    fun setRecentsEnabled(enabled: Boolean) {
        preferencesStorage.recentsEnabled = enabled
        _recentsEnabled.value = enabled
    }
}
