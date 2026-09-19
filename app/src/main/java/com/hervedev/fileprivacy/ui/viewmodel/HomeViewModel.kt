package com.hervedev.fileprivacy.ui.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hervedev.fileprivacy.data.CategoryScanner
import com.hervedev.fileprivacy.data.CredentialStorage
import com.hervedev.fileprivacy.data.DateFilter
import com.hervedev.fileprivacy.data.FileCategory
import com.hervedev.fileprivacy.data.PreferencesStorage
import com.hervedev.fileprivacy.data.RecentFilesScanner
import com.hervedev.fileprivacy.data.SearchScanner
import com.hervedev.fileprivacy.data.StorageVolumeInfo
import com.hervedev.fileprivacy.data.StorageVolumesHelper
import com.hervedev.fileprivacy.data.db.AppDatabase
import com.hervedev.fileprivacy.data.db.SmbConnectionEntity
import com.hervedev.fileprivacy.data.db.FtpConnectionEntity
import com.hervedev.fileprivacy.data.db.WebDavConnectionEntity
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.FtpConnection
import com.hervedev.fileprivacy.domain.SmbConnection
import com.hervedev.fileprivacy.domain.WebDavConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val smbConnectionDao = db.smbConnectionDao()
    private val trashDao = db.trashEntryDao()
    private val credentialStorage = CredentialStorage(application)
    private val preferencesStorage = PreferencesStorage(application)

    private val _externalVolumes = MutableStateFlow<List<StorageVolumeInfo>>(emptyList())
    val externalVolumes: StateFlow<List<StorageVolumeInfo>> = _externalVolumes.asStateFlow()

    private val _categoryCounts = MutableStateFlow<Map<FileCategory, Int>>(emptyMap())
    val categoryCounts: StateFlow<Map<FileCategory, Int>> = _categoryCounts.asStateFlow()

    private val _recentFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val recentFiles: StateFlow<List<FileItem>> = _recentFiles.asStateFlow()

    private val _isCategoriesEnabled = MutableStateFlow(preferencesStorage.categoriesEnabled)
    val isCategoriesEnabled: StateFlow<Boolean> = _isCategoriesEnabled.asStateFlow()

    private val _isRecentsEnabled = MutableStateFlow(preferencesStorage.recentsEnabled)
    val isRecentsEnabled: StateFlow<Boolean> = _isRecentsEnabled.asStateFlow()

    private val _isScanningCategories = MutableStateFlow(false)
    val isScanningCategories: StateFlow<Boolean> = _isScanningCategories.asStateFlow()

    // Search state
    val searchQuery = MutableStateFlow("")
    val searchTypeFilter = MutableStateFlow<FileCategory?>(null)
    val searchDateFilter = MutableStateFlow<DateFilter?>(null)

    private val _searchResults = MutableStateFlow<List<FileItem>>(emptyList())
    val searchResults: StateFlow<List<FileItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var hasScannedOnce = false

    private val ftpConnectionDao = db.ftpConnectionDao()
    private val webDavConnectionDao = db.webDavConnectionDao()

    val smbConnections: StateFlow<List<SmbConnection>> = smbConnectionDao.getAll()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val ftpConnections: StateFlow<List<FtpConnection>> = ftpConnectionDao.getAll()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val webDavConnections: StateFlow<List<WebDavConnection>> = webDavConnectionDao.getAll()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val trashCount: StateFlow<Int> = trashDao.getCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    init {
        refreshVolumes()
        scanCategories()
        setupSearchDebounce()
    }

    @OptIn(FlowPreview::class)
    private fun setupSearchDebounce() {
        viewModelScope.launch {
            combine(searchQuery, searchTypeFilter, searchDateFilter) { query, type, date ->
                Triple(query, type, date)
            }
                .debounce(400)
                .collect { (query, type, date) ->
                    if (query.trim().length >= 2) {
                        _isSearching.value = true
                        val rootPath = Environment.getExternalStorageDirectory().absolutePath
                        val results = SearchScanner.searchFiles(query, rootPath, type, date)
                        _searchResults.value = results
                        _isSearching.value = false
                    } else {
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                    }
                }
        }
    }

    fun clearSearch() {
        searchQuery.value = ""
        searchTypeFilter.value = null
        searchDateFilter.value = null
        _searchResults.value = emptyList()
    }

    fun refreshVolumes() {
        val volumes = StorageVolumesHelper.getExternalStorageVolumes(getApplication())
        _externalVolumes.value = volumes
        _isCategoriesEnabled.value = preferencesStorage.categoriesEnabled
        _isRecentsEnabled.value = preferencesStorage.recentsEnabled
    }

    fun invalidateCache() {
        hasScannedOnce = false
    }

    fun scanCategories(force: Boolean = false) {
        val catEnabled = preferencesStorage.categoriesEnabled
        val recEnabled = preferencesStorage.recentsEnabled

        if (!catEnabled && !recEnabled) {
            _categoryCounts.value = emptyMap()
            _recentFiles.value = emptyList()
            return
        }

        if (!force && hasScannedOnce) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isScanningCategories.value = true
            val rootPath = Environment.getExternalStorageDirectory().absolutePath

            if (catEnabled) {
                val map = mutableMapOf<FileCategory, Int>()
                for (category in FileCategory.entries) {
                    val list = CategoryScanner.scanCategory(category, rootPath)
                    map[category] = list.size
                }
                _categoryCounts.value = map
            } else {
                _categoryCounts.value = emptyMap()
            }

            if (recEnabled) {
                val recents = RecentFilesScanner.scanRecentFiles(rootPath, limit = 3)
                _recentFiles.value = recents
            } else {
                _recentFiles.value = emptyList()
            }

            hasScannedOnce = true
            _isScanningCategories.value = false
        }
    }

    fun deleteConnection(connection: SmbConnection) {
        viewModelScope.launch {
            smbConnectionDao.delete(SmbConnectionEntity.fromDomain(connection))
            credentialStorage.deletePassword(connection.id, type = "smb")
        }
    }

    fun deleteFtpConnection(connection: FtpConnection) {
        viewModelScope.launch {
            ftpConnectionDao.delete(FtpConnectionEntity.fromDomain(connection))
            credentialStorage.deletePassword(connection.id, type = "ftp")
        }
    }

    fun deleteWebDavConnection(connection: WebDavConnection) {
        viewModelScope.launch {
            webDavConnectionDao.delete(WebDavConnectionEntity.fromDomain(connection))
            credentialStorage.deletePassword(connection.id, type = "webdav")
        }
    }
}
