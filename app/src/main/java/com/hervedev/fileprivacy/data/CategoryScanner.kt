package com.hervedev.fileprivacy.data

import android.os.Environment
import com.hervedev.fileprivacy.domain.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class FileCategory(val displayName: String) {
    IMAGES("Images"),
    VIDEOS("Vidéos"),
    AUDIO("Audio"),
    DOCUMENTS("Documents"),
    DOWNLOADS("Téléchargements"),
    APK("APK")
}

private val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "flv")
private val AUDIO_EXTS = setOf("mp3", "wav", "ogg", "m4a", "flac", "aac")
private val DOCUMENT_EXTS = setOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx", "epub")
private val APK_EXTS = setOf("apk")

object CategoryScanner {

    suspend fun getCategorySizes(rootPath: String): Map<FileCategory, Long> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<FileCategory, Long>()
        for (category in FileCategory.entries) {
            val files = scanCategory(category, rootPath)
            map[category] = files.sumOf { it.sizeBytes }
        }
        map
    }

    suspend fun scanCategory(category: FileCategory, rootPath: String): List<FileItem> = withContext(Dispatchers.IO) {
        val root = File(rootPath)
        if (!root.exists() || !root.isDirectory) return@withContext emptyList()

        if (category == FileCategory.DOWNLOADS) {
            val downloadDir = File(rootPath, Environment.DIRECTORY_DOWNLOADS)
            val dirToScan = if (downloadDir.exists()) downloadDir else File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath)
            if (!dirToScan.exists() || !dirToScan.isDirectory) return@withContext emptyList()

            val files = dirToScan.listFiles() ?: return@withContext emptyList()
            return@withContext files.map { f ->
                FileItem(
                    name = f.name,
                    path = f.absolutePath,
                    isDirectory = f.isDirectory,
                    sizeBytes = f.length(),
                    lastModified = f.lastModified()
                )
            }.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
        }

        val results = mutableListOf<FileItem>()
        scanDirectory(root, category, results, depth = 6)
        results.sortedByDescending { it.lastModified }
    }

    private fun scanDirectory(dir: File, category: FileCategory, results: MutableList<FileItem>, depth: Int) {
        if (depth <= 0) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            val name = file.name
            if (name.startsWith(".")) continue
            if (file.isDirectory) {
                if (name.equals("Android", ignoreCase = true)) continue
                scanDirectory(file, category, results, depth - 1)
            } else {
                if (matchesCategory(file, category)) {
                    results.add(
                        FileItem(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = false,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified()
                        )
                    )
                }
            }
        }
    }

    private fun matchesCategory(file: File, category: FileCategory): Boolean {
        val name = file.name
        val dotIndex = name.lastIndexOf('.')
        if (dotIndex <= 0 || dotIndex == name.length - 1) return false
        val ext = name.substring(dotIndex + 1).lowercase(Locale.getDefault())

        return when (category) {
            FileCategory.IMAGES -> isImageExtension(ext)
            FileCategory.VIDEOS -> VIDEO_EXTS.contains(ext)
            FileCategory.AUDIO -> AUDIO_EXTS.contains(ext)
            FileCategory.DOCUMENTS -> DOCUMENT_EXTS.contains(ext)
            FileCategory.DOWNLOADS -> true
            FileCategory.APK -> APK_EXTS.contains(ext)
        }
    }

    private fun isImageExtension(ext: String): Boolean {
        return setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "dng", "raw").contains(ext)
    }
}
