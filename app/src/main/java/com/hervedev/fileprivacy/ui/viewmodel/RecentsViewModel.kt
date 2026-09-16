package com.hervedev.fileprivacy.ui.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hervedev.fileprivacy.data.LocalFileSource
import com.hervedev.fileprivacy.data.PreferencesStorage
import com.hervedev.fileprivacy.data.RecentFilesScanner
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.FileSortingPreferences.applySortPreference
import com.hervedev.fileprivacy.domain.FileSortingPreferences.getInitialIsGridMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecentsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesStorage = PreferencesStorage(application)
    private val localFileSource = LocalFileSource(application)

    private val _recentFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val recentFiles: StateFlow<List<FileItem>> = _recentFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRecentsEnabled = MutableStateFlow(preferencesStorage.recentsEnabled)
    val isRecentsEnabled: StateFlow<Boolean> = _isRecentsEnabled.asStateFlow()

    private val _isGridMode = MutableStateFlow(preferencesStorage.getInitialIsGridMode())
    val isGridMode: StateFlow<Boolean> = _isGridMode.asStateFlow()

    private val _currentSortOrder = MutableStateFlow(preferencesStorage.defaultSortOrder)
    val currentSortOrder: StateFlow<String> = _currentSortOrder.asStateFlow()

    private var rawFileItems: List<FileItem> = emptyList()

    private var hasScannedOnce = false

    init {
        scanRecents()
    }

    fun toggleViewMode() {
        _isGridMode.value = !_isGridMode.value
    }

    fun setSessionSortOrder(order: String) {
        _currentSortOrder.value = order
        _recentFiles.value = rawFileItems.applySortPreference(order)
    }

    fun invalidateCache() {
        hasScannedOnce = false
    }

    fun scanRecents(force: Boolean = false) {
        _isRecentsEnabled.value = preferencesStorage.recentsEnabled
        if (!preferencesStorage.recentsEnabled) {
            _recentFiles.value = emptyList()
            return
        }

        if (!force && hasScannedOnce) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val rootPath = Environment.getExternalStorageDirectory().absolutePath
            rawFileItems = RecentFilesScanner.scanRecentFiles(rootPath, limit = 50)
            _recentFiles.value = rawFileItems.applySortPreference(_currentSortOrder.value)
            hasScannedOnce = true
            _isLoading.value = false
        }
    }

    fun renameFile(item: FileItem, newName: String, onResult: (Boolean, String) -> Unit) {
        val name = newName.trim()
        if (name.isEmpty() || name == item.name) return
        viewModelScope.launch {
            _isLoading.value = true
            val success = localFileSource.renameFile(item.path, name)
            scanRecents(force = true)
            if (success) {
                onResult(true, "'${item.name}' renommé en '$name'")
            } else {
                onResult(false, "Échec du renommage de '${item.name}'")
            }
        }
    }

    fun deleteFile(item: FileItem, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = localFileSource.moveToTrash(item.path)
            scanRecents(force = true)
            if (success) {
                onResult(true, "'${item.name}' déplacé vers la corbeille")
            } else {
                onResult(false, "Échec de la suppression de '${item.name}'")
            }
        }
    }

    fun permanentlyDeleteFile(item: FileItem, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = localFileSource.permanentlyDelete(item.path)
            scanRecents(force = true)
            if (success) {
                onResult(true, "'${item.name}' supprimé définitivement")
            } else {
                onResult(false, "Échec de la suppression de '${item.name}'")
            }
        }
    }
}
