package com.hervedev.fileprivacy.domain

import com.hervedev.fileprivacy.data.PreferencesStorage

object FileSortingPreferences {

    fun List<FileItem>.applySortPreference(sortOrder: String): List<FileItem> {
        return when (sortOrder) {
            "NAME_DESC" -> this.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenByDescending { it.name.lowercase() })
            "DATE_DESC" -> this.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenByDescending { it.lastModified })
            "DATE_ASC" -> this.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.lastModified })
            "SIZE_DESC" -> this.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenByDescending { it.sizeBytes })
            "SIZE_ASC" -> this.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.sizeBytes })
            else -> this.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
        }
    }

    fun PreferencesStorage.getInitialIsGridMode(categoryDefaultGrid: Boolean = false): Boolean {
        val prefMode = this.defaultViewMode
        return if (prefMode == "GRID") {
            true
        } else if (prefMode == "LIST") {
            false
        } else {
            categoryDefaultGrid
        }
    }
}
