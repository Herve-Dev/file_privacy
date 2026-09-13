package com.hervedev.fileprivacy.ui.viewmodel

import android.net.Uri
import android.os.Environment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hervedev.fileprivacy.data.FtpFileSource
import com.hervedev.fileprivacy.data.LocalFileSource
import com.hervedev.fileprivacy.data.SmbFileSource
import com.hervedev.fileprivacy.data.WebDavFileSource
import com.hervedev.fileprivacy.domain.ClipboardMode
import com.hervedev.fileprivacy.domain.FileClipboard
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.FileSystemProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class FileListViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val sourceType: String = savedStateHandle.get<String>("sourceType") ?: "local"

    private val encodedPath: String = savedStateHandle.get<String>("encodedPath")
        ?: Uri.encode(Environment.getExternalStorageDirectory().absolutePath)

    val currentPath: String = Uri.decode(encodedPath)

    private val fileSystemProvider: FileSystemProvider = when (sourceType) {
        "local", "external" -> LocalFileSource()
        "smb" -> SmbFileSource(serverAddress = "", shareName = "", username = "", password = "")
        "ftp" -> FtpFileSource()
        "webdav" -> WebDavFileSource()
        else -> LocalFileSource()
    }

    private val _fileItems = MutableStateFlow<List<FileItem>>(emptyList())
    val fileItems: StateFlow<List<FileItem>> = _fileItems.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isStorageAccessible = MutableStateFlow(true)
    val isStorageAccessible: StateFlow<Boolean> = _isStorageAccessible.asStateFlow()

    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths: StateFlow<Set<String>> = _selectedPaths.asStateFlow()

    val clipboardState = FileClipboard.state

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            val dir = File(currentPath)
            if (!dir.exists()) {
                _isStorageAccessible.value = false
                _fileItems.value = emptyList()
            } else {
                _isStorageAccessible.value = true
                _fileItems.value = fileSystemProvider.listFiles(currentPath)
            }
            _isLoading.value = false
        }
    }

    fun toggleSelection(path: String) {
        val current = _selectedPaths.value.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _selectedPaths.value = current
    }

    fun selectAll() {
        _selectedPaths.value = _fileItems.value.map { it.path }.toSet()
    }

    fun clearSelection() {
        _selectedPaths.value = emptySet()
    }

    fun copySelected() {
        val itemsToCopy = _fileItems.value.filter { _selectedPaths.value.contains(it.path) }
        if (itemsToCopy.isNotEmpty()) {
            FileClipboard.set(itemsToCopy, ClipboardMode.COPY)
        }
        clearSelection()
    }

    fun cutSelected() {
        val itemsToCut = _fileItems.value.filter { _selectedPaths.value.contains(it.path) }
        if (itemsToCut.isNotEmpty()) {
            FileClipboard.set(itemsToCut, ClipboardMode.CUT)
        }
        clearSelection()
    }

    fun copyItem(item: FileItem) {
        FileClipboard.set(listOf(item), ClipboardMode.COPY)
    }

    fun cutItem(item: FileItem) {
        FileClipboard.set(listOf(item), ClipboardMode.CUT)
    }

    fun renameFile(item: FileItem, newName: String, onResult: (Boolean, String) -> Unit) {
        val name = newName.trim()
        if (name.isEmpty() || name == item.name) return
        viewModelScope.launch {
            _isLoading.value = true
            val success = fileSystemProvider.renameFile(item.path, name)
            loadFiles()
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
            val success = fileSystemProvider.deleteFile(item.path)
            loadFiles()
            if (success) {
                onResult(true, "'${item.name}' supprimé")
            } else {
                onResult(false, "Échec de la suppression de '${item.name}'")
            }
        }
    }

    fun createFolder(folderName: String, onResult: (Boolean, String) -> Unit) {
        val name = folderName.trim()
        if (name.isEmpty()) {
            onResult(false, "Le nom du dossier ne peut pas être vide")
            return
        }
        val targetPath = "$currentPath/$name"
        viewModelScope.launch {
            _isLoading.value = true
            val success = fileSystemProvider.createFolder(targetPath)
            loadFiles()
            if (success) {
                onResult(true, "Dossier '$name' créé")
            } else {
                onResult(false, "Échec de la création du dossier '$name'")
            }
        }
    }

    fun pasteClipboard(onResult: (Boolean, String) -> Unit) {
        val clipboard = FileClipboard.state.value ?: return
        if (clipboard.items.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            var countSuccess = 0
            val total = clipboard.items.size

            for (item in clipboard.items) {
                val destPath = "$currentPath/${item.name}"
                val success = fileSystemProvider.copyFile(item.path, destPath)
                if (success) {
                    countSuccess++
                    if (clipboard.mode == ClipboardMode.CUT) {
                        fileSystemProvider.deleteFile(item.path)
                    }
                }
            }

            FileClipboard.clear()
            loadFiles()

            if (countSuccess == total) {
                onResult(true, "$countSuccess élément(s) collé(s) avec succès")
            } else if (countSuccess > 0) {
                onResult(false, "$countSuccess/$total élément(s) collé(s)")
            } else {
                onResult(false, "Échec du collage")
            }
        }
    }
}
