package com.hervedev.fileprivacy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.hervedev.fileprivacy.data.StorageVolumeInfo
import com.hervedev.fileprivacy.data.StorageVolumesHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _externalVolumes = MutableStateFlow<List<StorageVolumeInfo>>(emptyList())
    val externalVolumes: StateFlow<List<StorageVolumeInfo>> = _externalVolumes.asStateFlow()

    init {
        refreshVolumes()
    }

    fun refreshVolumes() {
        val volumes = StorageVolumesHelper.getExternalStorageVolumes(getApplication())
        _externalVolumes.value = volumes
    }
}
