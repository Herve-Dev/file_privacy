package com.hervedev.fileprivacy.domain

import java.util.Locale

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long
)

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "dng", "raw")

fun FileItem.isImage(): Boolean {
    if (isDirectory) return false
    val dotIndex = name.lastIndexOf('.')
    if (dotIndex < 0 || dotIndex == name.length - 1) return false
    val extension = name.substring(dotIndex + 1).lowercase(Locale.getDefault())
    return IMAGE_EXTENSIONS.contains(extension)
}
