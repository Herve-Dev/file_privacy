package com.hervedev.fileprivacy.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hervedev.fileprivacy.data.CredentialStorage
import com.hervedev.fileprivacy.data.FtpFileSource
import com.hervedev.fileprivacy.data.LocalFileSource
import com.hervedev.fileprivacy.data.SmbFileSource
import com.hervedev.fileprivacy.data.WebDavFileSource
import com.hervedev.fileprivacy.data.db.AppDatabase
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
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    val connectionId: Long? = savedStateHandle.get<Long>("connectionId")
    val sourceType: String = savedStateHandle.get<String>("sourceType")
        ?: if (connectionId != null) "smb" else "local"

    private val encodedPath: String = savedStateHandle.get<String>("encodedPath")
        ?: if (connectionId == null) Uri.encode(Environment.getExternalStorageDirectory().absolutePath) else ""

    val currentPath: String = Uri.decode(encodedPath)

    private val _fileItems = MutableStateFlow<List<FileItem>>(emptyList())
    val fileItems: StateFlow<List<FileItem>> = _fileItems.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isStorageAccessible = MutableStateFlow(true)
    val isStorageAccessible: StateFlow<Boolean> = _isStorageAccessible.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths: StateFlow<Set<String>> = _selectedPaths.asStateFlow()

    val clipboardState = FileClipboard.state

    private var fileSystemProvider: FileSystemProvider? = null

    init {
        initProviderAndLoad()
    }

    private fun initProviderAndLoad() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (sourceType == "smb" && connectionId != null) {
                    val db = AppDatabase.getInstance(getApplication())
                    val dao = db.smbConnectionDao()
                    val entity = dao.getById(connectionId)
                    val credStorage = CredentialStorage(getApplication())
                    val pwd = credStorage.getPassword(connectionId) ?: ""

                    if (entity == null) {
                        _isStorageAccessible.value = false
                        _errorMessage.value = "La connexion SMB n'existe plus."
                        _isLoading.value = false
                        return@launch
                    }

                    fileSystemProvider = SmbFileSource(
                        serverAddress = entity.serverAddress,
                        shareName = entity.shareName,
                        username = entity.username,
                        password = pwd,
                        port = entity.port
                    )
                } else {
                    fileSystemProvider = when (sourceType) {
                        "local", "external" -> LocalFileSource(getApplication())
                        "ftp" -> FtpFileSource()
                        "webdav" -> WebDavFileSource()
                        else -> LocalFileSource(getApplication())
                    }
                }

                loadFilesInternal()
            } catch (e: Exception) {
                _isStorageAccessible.value = false
                _errorMessage.value = "Erreur d'initialisation de la source : ${e.localizedMessage}"
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadFilesInternal() {
        val provider = fileSystemProvider
        if (provider == null) {
            _isStorageAccessible.value = false
            _isLoading.value = false
            return
        }

        if (sourceType == "local" || sourceType == "external") {
            val dir = File(currentPath)
            if (!dir.exists()) {
                _isStorageAccessible.value = false
                _fileItems.value = emptyList()
                _isLoading.value = false
                return
            }
        }

        _isStorageAccessible.value = true
        _fileItems.value = provider.listFiles(currentPath)
        _isLoading.value = false
    }

    fun loadFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            loadFilesInternal()
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
        val provider = fileSystemProvider ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val success = provider.renameFile(item.path, name)
            loadFilesInternal()
            if (success) {
                onResult(true, "'${item.name}' renommé en '$name'")
            } else {
                onResult(false, "Échec du renommage de '${item.name}'")
            }
        }
    }

    fun deleteFile(item: FileItem, onResult: (Boolean, String) -> Unit) {
        val provider = fileSystemProvider ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val success = if (sourceType == "local") {
                provider.moveToTrash(item.path)
            } else {
                provider.deleteFile(item.path)
            }
            loadFilesInternal()
            if (success) {
                val message = if (sourceType == "local") "'${item.name}' déplacé vers la corbeille" else "'${item.name}' supprimé"
                onResult(true, message)
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
        val provider = fileSystemProvider ?: return
        val targetPath = if (currentPath.isEmpty()) name else "$currentPath/$name"
        viewModelScope.launch {
            _isLoading.value = true
            val success = provider.createFolder(targetPath)
            loadFilesInternal()
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
        val provider = fileSystemProvider ?: return

        viewModelScope.launch {
            _isLoading.value = true
            var countSuccess = 0
            val total = clipboard.items.size

            for (item in clipboard.items) {
                val destPath = if (currentPath.isEmpty()) item.name else "$currentPath/${item.name}"
                val success = provider.copyFile(item.path, destPath)
                if (success) {
                    countSuccess++
                    if (clipboard.mode == ClipboardMode.CUT) {
                        provider.deleteFile(item.path)
                    }
                }
            }

            FileClipboard.clear()
            loadFilesInternal()

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
