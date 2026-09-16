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
private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp")
private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "flac", "aac", "ogg", "m4a")
private val DOCUMENT_EXTENSIONS = setOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx", "epub")

private fun getExtension(name: String): String {
    val dotIndex = name.lastIndexOf('.')
    if (dotIndex < 0 || dotIndex == name.length - 1) return ""
    return name.substring(dotIndex + 1).lowercase(Locale.getDefault())
}

fun FileItem.isImage(): Boolean {
    if (isDirectory) return false
    return IMAGE_EXTENSIONS.contains(getExtension(name))
}

fun FileItem.isVideo(): Boolean {
    if (isDirectory) return false
    return VIDEO_EXTENSIONS.contains(getExtension(name))
}

fun FileItem.isAudio(): Boolean {
    if (isDirectory) return false
    return AUDIO_EXTENSIONS.contains(getExtension(name))
}

fun FileItem.isPdf(): Boolean {
    if (isDirectory) return false
    return getExtension(name) == "pdf"
}

fun FileItem.isDocument(): Boolean {
    if (isDirectory) return false
    return DOCUMENT_EXTENSIONS.contains(getExtension(name))
}

fun FileItem.isApk(): Boolean {
    if (isDirectory) return false
    return getExtension(name) == "apk"
}
