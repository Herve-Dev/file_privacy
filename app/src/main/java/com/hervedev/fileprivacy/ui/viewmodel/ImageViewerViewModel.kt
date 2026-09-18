package com.hervedev.fileprivacy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hervedev.fileprivacy.data.LocalFileSource
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.ImageViewerSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImageViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val localFileSource = LocalFileSource(application)

    private val sessionData = ImageViewerSession.data.value

    private val _imagesList = MutableStateFlow<List<FileItem>>(sessionData?.items ?: emptyList())
    val imagesList: StateFlow<List<FileItem>> = _imagesList.asStateFlow()

    val initialIndex: Int = sessionData?.initialIndex ?: 0
    val sourceType: String = sessionData?.sourceType ?: "local"

    fun renameFile(item: FileItem, newName: String, onResult: (Boolean, String) -> Unit) {
        val name = newName.trim()
        if (name.isEmpty() || name == item.name) return
        viewModelScope.launch {
            val success = localFileSource.renameFile(item.path, name)
            if (success) {
                val updatedList = _imagesList.value.map {
                    if (it.path == item.path) it.copy(name = name) else it
                }
                _imagesList.value = updatedList
                onResult(true, "'${item.name}' renommé en '$name'")
            } else {
                onResult(false, "Échec du renommage de '${item.name}'")
            }
        }
    }

    fun deleteFile(item: FileItem, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val success = if (sourceType == "local") {
                localFileSource.moveToTrash(item.path)
            } else {
                localFileSource.deleteFile(item.path)
            }
            if (success) {
                val updatedList = _imagesList.value.filter { it.path != item.path }
                _imagesList.value = updatedList
                val message = if (sourceType == "local") "'${item.name}' déplacé vers la corbeille" else "'${item.name}' supprimé"
                onResult(true, message)
            } else {
                onResult(false, "Échec de la suppression de '${item.name}'")
            }
        }
    }

    fun permanentlyDeleteFile(item: FileItem, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val success = if (sourceType == "local") {
                localFileSource.permanentlyDelete(item.path)
            } else {
                localFileSource.deleteFile(item.path)
            }
            if (success) {
                val updatedList = _imagesList.value.filter { it.path != item.path }
                _imagesList.value = updatedList
                onResult(true, "'${item.name}' supprimé définitivement")
            } else {
                onResult(false, "Échec de la suppression de '${item.name}'")
            }
        }
    }
}
