package com.hervedev.fileprivacy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hervedev.fileprivacy.data.CredentialStorage
import com.hervedev.fileprivacy.data.FtpFileSource
import com.hervedev.fileprivacy.data.LocalFileSource
import com.hervedev.fileprivacy.data.RemoteFileCache
import com.hervedev.fileprivacy.data.SmbFileSource
import com.hervedev.fileprivacy.data.WebDavFileSource
import com.hervedev.fileprivacy.data.db.AppDatabase
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.FileSystemProvider
import com.hervedev.fileprivacy.domain.ImageViewerSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ImageViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionData = ImageViewerSession.data.value

    private val _imagesList = MutableStateFlow<List<FileItem>>(sessionData?.items ?: emptyList())
    val imagesList: StateFlow<List<FileItem>> = _imagesList.asStateFlow()

    val initialIndex: Int = sessionData?.initialIndex ?: 0
    val sourceType: String = sessionData?.sourceType ?: "local"
    val connectionId: Long? = sessionData?.connectionId

    private val _cachedImageMap = MutableStateFlow<Map<String, File>>(emptyMap())
    val cachedImageMap: StateFlow<Map<String, File>> = _cachedImageMap.asStateFlow()

    private val _loadingPages = MutableStateFlow<Set<String>>(emptySet())
    val loadingPages: StateFlow<Set<String>> = _loadingPages.asStateFlow()

    private var remoteProvider: FileSystemProvider? = null

    init {
        if (sourceType !in listOf("local", "external") && connectionId != null) {
            initRemoteProvider(sourceType, connectionId)
        }
    }

    private fun initRemoteProvider(type: String, id: Long) {
        viewModelScope.launch {
            val db = AppDatabase.getInstance(getApplication())
            val credStorage = CredentialStorage(getApplication())
            when (type) {
                "smb" -> {
                    val entity = db.smbConnectionDao().getById(id)
                    if (entity != null) {
                        val pwd = credStorage.getPassword(id, type = "smb") ?: ""
                        remoteProvider = SmbFileSource(
                            serverAddress = entity.serverAddress,
                            shareName = entity.shareName,
                            username = entity.username,
                            password = pwd,
                            port = entity.port
                        )
                    }
                }
                "ftp" -> {
                    val entity = db.ftpConnectionDao().getById(id)
                    if (entity != null) {
                        val pwd = credStorage.getPassword(id, type = "ftp") ?: ""
                        remoteProvider = FtpFileSource(
                            serverAddress = entity.serverAddress,
                            port = entity.port,
                            username = entity.username,
                            password = pwd,
                            useFtps = entity.useFtps
                        )
                    }
                }
                "webdav" -> {
                    val entity = db.webDavConnectionDao().getById(id)
                    if (entity != null) {
                        val pwd = credStorage.getPassword(id, type = "webdav") ?: ""
                        remoteProvider = WebDavFileSource(
                            serverUrl = entity.serverUrl,
                            port = entity.port,
                            basePath = entity.basePath,
                            username = entity.username,
                            password = pwd
                        )
                    }
                }
            }
        }
    }

    fun ensureImageCached(item: FileItem) {
        if (sourceType in listOf("local", "external")) return
        if (_cachedImageMap.value.containsKey(item.path)) return
        if (_loadingPages.value.contains(item.path)) return

        viewModelScope.launch {
            _loadingPages.value = _loadingPages.value + item.path
            var provider = remoteProvider
            if (provider == null) {
                delay(300)
                provider = remoteProvider
            }

            if (provider != null) {
                val destination = RemoteFileCache.getCacheFileFor(
                    getApplication(),
                    connectionId ?: 0L,
                    item.name
                )
                val success = provider.downloadToCache(item.path, destination)
                if (success && destination.exists() && destination.length() > 0) {
                    _cachedImageMap.value = _cachedImageMap.value + (item.path to destination)
                }
            }
            _loadingPages.value = _loadingPages.value - item.path
        }
    }

    fun renameFile(item: FileItem, newName: String, onResult: (Boolean, String) -> Unit) {
        val name = newName.trim()
        if (name.isEmpty() || name == item.name) return
        val provider = remoteProvider ?: LocalFileSource(getApplication())
        viewModelScope.launch {
            val success = provider.renameFile(item.path, name)
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
        val provider = remoteProvider ?: LocalFileSource(getApplication())
        viewModelScope.launch {
            val success = if (sourceType == "local") {
                provider.moveToTrash(item.path)
            } else {
                provider.deleteFile(item.path)
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
        val provider = remoteProvider ?: LocalFileSource(getApplication())
        viewModelScope.launch {
            val success = if (sourceType == "local") {
                provider.permanentlyDelete(item.path)
            } else {
                provider.deleteFile(item.path)
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
