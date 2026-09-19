package com.hervedev.fileprivacy.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ImageViewerData(
    val items: List<FileItem>,
    val initialIndex: Int,
    val sourceType: String = "local",
    val connectionId: Long? = null
)

object ImageViewerSession {
    private val _data = MutableStateFlow<ImageViewerData?>(null)
    val data: StateFlow<ImageViewerData?> = _data.asStateFlow()

    fun start(items: List<FileItem>, initialIndex: Int, sourceType: String = "local", connectionId: Long? = null) {
        val imageItems = items.filter { it.isImage() }
        val validIndex = initialIndex.coerceIn(0, (imageItems.size - 1).coerceAtLeast(0))
        _data.value = ImageViewerData(
            items = imageItems,
            initialIndex = validIndex,
            sourceType = sourceType,
            connectionId = connectionId
        )
    }

    fun clear() {
        _data.value = null
    }
}
