package com.hervedev.fileprivacy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hervedev.fileprivacy.data.CredentialStorage
import com.hervedev.fileprivacy.data.StorageVolumeInfo
import com.hervedev.fileprivacy.data.StorageVolumesHelper
import com.hervedev.fileprivacy.data.db.AppDatabase
import com.hervedev.fileprivacy.data.db.SmbConnectionEntity
import com.hervedev.fileprivacy.domain.SmbConnection
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
    private val credentialStorage = CredentialStorage(application)

    private val _externalVolumes = MutableStateFlow<List<StorageVolumeInfo>>(emptyList())
    val externalVolumes: StateFlow<List<StorageVolumeInfo>> = _externalVolumes.asStateFlow()

    val smbConnections: StateFlow<List<SmbConnection>> = smbConnectionDao.getAll()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        refreshVolumes()
    }

    fun refreshVolumes() {
        val volumes = StorageVolumesHelper.getExternalStorageVolumes(getApplication())
        _externalVolumes.value = volumes
    }

    fun deleteConnection(connection: SmbConnection) {
        viewModelScope.launch {
            smbConnectionDao.delete(SmbConnectionEntity.fromDomain(connection))
            credentialStorage.deletePassword(connection.id)
        }
    }
}
