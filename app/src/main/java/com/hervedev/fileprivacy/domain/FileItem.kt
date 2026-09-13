package com.hervedev.fileprivacy.domain

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long
)
