package com.hervedev.fileprivacy.ui.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hervedev.fileprivacy.data.CategoryScanner
import com.hervedev.fileprivacy.data.FileCategory
import com.hervedev.fileprivacy.data.LocalFileSource
import com.hervedev.fileprivacy.domain.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CategoryResultViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    val categoryName: String = savedStateHandle.get<String>("categoryName") ?: "IMAGES"
    val category: FileCategory = try {
        FileCategory.valueOf(categoryName)
    } catch (_: Exception) {
        FileCategory.IMAGES
    }

    private val _fileItems = MutableStateFlow<List<FileItem>>(emptyList())
    val fileItems: StateFlow<List<FileItem>> = _fileItems.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isGridMode = MutableStateFlow(category == FileCategory.IMAGES)
    val isGridMode: StateFlow<Boolean> = _isGridMode.asStateFlow()

    private val localFileSource = LocalFileSource(application)

    init {
        loadCategoryFiles()
    }

    fun toggleViewMode() {
        _isGridMode.value = !_isGridMode.value
    }

    fun loadCategoryFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            val rootPath = Environment.getExternalStorageDirectory().absolutePath
            val results = CategoryScanner.scanCategory(category, rootPath)
            _fileItems.value = results
            _isLoading.value = false
        }
    }

    fun renameFile(item: FileItem, newName: String, onResult: (Boolean, String) -> Unit) {
        val name = newName.trim()
        if (name.isEmpty() || name == item.name) return
        viewModelScope.launch {
            _isLoading.value = true
            val success = localFileSource.renameFile(item.path, name)
            loadCategoryFiles()
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
            loadCategoryFiles()
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
            loadCategoryFiles()
            if (success) {
                onResult(true, "'${item.name}' supprimé définitivement")
            } else {
                onResult(false, "Échec de la suppression de '${item.name}'")
            }
        }
    }
}
