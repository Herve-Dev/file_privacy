package com.hervedev.fileprivacy.ui.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hervedev.fileprivacy.data.CategoryScanner
import com.hervedev.fileprivacy.data.CredentialStorage
import com.hervedev.fileprivacy.data.FileCategory
import com.hervedev.fileprivacy.data.PreferencesStorage
import com.hervedev.fileprivacy.data.StorageVolumeInfo
import com.hervedev.fileprivacy.data.StorageVolumesHelper
import com.hervedev.fileprivacy.data.db.AppDatabase
import com.hervedev.fileprivacy.data.db.SmbConnectionEntity
import com.hervedev.fileprivacy.domain.SmbConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _isCategoriesEnabled = MutableStateFlow(preferencesStorage.categoriesEnabled)
    val isCategoriesEnabled: StateFlow<Boolean> = _isCategoriesEnabled.asStateFlow()

    private val _isScanningCategories = MutableStateFlow(false)
    val isScanningCategories: StateFlow<Boolean> = _isScanningCategories.asStateFlow()

    private var hasScannedOnce = false

    val smbConnections: StateFlow<List<SmbConnection>> = smbConnectionDao.getAll()
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
    }

    fun refreshVolumes() {
        val volumes = StorageVolumesHelper.getExternalStorageVolumes(getApplication())
        _externalVolumes.value = volumes
        _isCategoriesEnabled.value = preferencesStorage.categoriesEnabled
    }

    fun invalidateCache() {
        hasScannedOnce = false
    }

    fun scanCategories(force: Boolean = false) {
        if (!preferencesStorage.categoriesEnabled) {
            _categoryCounts.value = emptyMap()
            return
        }

        if (!force && hasScannedOnce) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isScanningCategories.value = true
            val rootPath = Environment.getExternalStorageDirectory().absolutePath
            val map = mutableMapOf<FileCategory, Int>()

            for (category in FileCategory.entries) {
                val list = CategoryScanner.scanCategory(category, rootPath)
                map[category] = list.size
            }

            _categoryCounts.value = map
            hasScannedOnce = true
            _isScanningCategories.value = false
        }
    }

    fun deleteConnection(connection: SmbConnection) {
        viewModelScope.launch {
            smbConnectionDao.delete(SmbConnectionEntity.fromDomain(connection))
            credentialStorage.deletePassword(connection.id)
        }
    }
}
