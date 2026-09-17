package com.hervedev.fileprivacy.ui.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hervedev.fileprivacy.data.CategoryScanner
import com.hervedev.fileprivacy.data.FileCategory
import com.hervedev.fileprivacy.data.StorageSpaceInfo
import com.hervedev.fileprivacy.data.StorageVolumeInfo
import com.hervedev.fileprivacy.data.StorageVolumesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategorySizeInfo(
    val category: FileCategory?,
    val label: String,
    val sizeBytes: Long
)

class StorageViewModel(application: Application) : AndroidViewModel(application) {

    private val _externalVolumes = MutableStateFlow<List<StorageVolumeInfo>>(emptyList())
    val externalVolumes: StateFlow<List<StorageVolumeInfo>> = _externalVolumes.asStateFlow()

    private val _internalSpaceInfo = MutableStateFlow<StorageSpaceInfo?>(null)
    val internalSpaceInfo: StateFlow<StorageSpaceInfo?> = _internalSpaceInfo.asStateFlow()

    private val _categorySizes = MutableStateFlow<List<CategorySizeInfo>>(emptyList())
    val categorySizes: StateFlow<List<CategorySizeInfo>> = _categorySizes.asStateFlow()

    private val _isLoadingChart = MutableStateFlow(false)
    val isLoadingChart: StateFlow<Boolean> = _isLoadingChart.asStateFlow()

    private var hasScannedOnce = false

    init {
        refreshVolumes()
        loadBreakdown()
    }

    fun refreshVolumes() {
        val rootPath = Environment.getExternalStorageDirectory().absolutePath
        val volumes = StorageVolumesHelper.getExternalStorageVolumes(getApplication())
        val spaceInfo = StorageVolumesHelper.getStorageSpaceInfo(rootPath)

        _externalVolumes.value = volumes
        _internalSpaceInfo.value = spaceInfo
    }

    fun invalidateCache() {
        hasScannedOnce = false
    }

    fun loadBreakdown(force: Boolean = false) {
        if (!force && hasScannedOnce) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingChart.value = true
            val rootPath = Environment.getExternalStorageDirectory().absolutePath
            val spaceInfo = StorageVolumesHelper.getStorageSpaceInfo(rootPath)
            _internalSpaceInfo.value = spaceInfo

            val categorySizesMap = CategoryScanner.getCategorySizes(rootPath)
            val list = mutableListOf<CategorySizeInfo>()

            var sumCategoriesBytes = 0L
            for (category in FileCategory.entries) {
                val size = categorySizesMap[category] ?: 0L
                sumCategoriesBytes += size
                list.add(CategorySizeInfo(category, category.displayName, size))
            }

            val usedBytes = spaceInfo?.usedBytes ?: 0L
            val otherBytes = (usedBytes - sumCategoriesBytes).coerceAtLeast(0L)
            list.add(CategorySizeInfo(null, "Autre", otherBytes))

            _categorySizes.value = list
            hasScannedOnce = true
            _isLoadingChart.value = false
        }
    }
}
