package com.hervedev.fileprivacy.ui.viewmodel

import android.net.Uri
import android.os.Environment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hervedev.fileprivacy.data.LocalFileSource
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.FileSystemProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FileListViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val fileSystemProvider: FileSystemProvider = LocalFileSource()

    private val encodedPath: String = savedStateHandle.get<String>("encodedPath")
        ?: Uri.encode(Environment.getExternalStorageDirectory().absolutePath)

    val currentPath: String = Uri.decode(encodedPath)

    private val _fileItems = MutableStateFlow<List<FileItem>>(emptyList())
    val fileItems: StateFlow<List<FileItem>> = _fileItems.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            _fileItems.value = fileSystemProvider.listFiles(currentPath)
            _isLoading.value = false
        }
    }
}
