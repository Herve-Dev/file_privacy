package com.hervedev.fileprivacy.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ClipboardMode {
    COPY,
    CUT
}

data class ClipboardState(
    val items: List<FileItem>,
    val mode: ClipboardMode
)

object FileClipboard {
    private val _state = MutableStateFlow<ClipboardState?>(null)
    val state: StateFlow<ClipboardState?> = _state.asStateFlow()

    fun set(items: List<FileItem>, mode: ClipboardMode) {
        _state.value = ClipboardState(items, mode)
    }

    fun clear() {
        _state.value = null
    }

    val hasItems: Boolean
        get() = _state.value?.items?.isNotEmpty() == true
}
