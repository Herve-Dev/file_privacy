package com.hervedev.fileprivacy.data

import com.hervedev.fileprivacy.domain.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object RecentFilesScanner {

    suspend fun scanRecentFiles(rootPath: String, limit: Int = 50): List<FileItem> = withContext(Dispatchers.IO) {
        val root = File(rootPath)
        if (!root.exists() || !root.isDirectory) return@withContext emptyList()

        val results = mutableListOf<FileItem>()
        scanDirectoryForFiles(root, results, depth = 5)

        results.sortedByDescending { it.lastModified }.take(limit)
    }

    private fun scanDirectoryForFiles(dir: File, results: MutableList<FileItem>, depth: Int) {
        if (depth <= 0) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            val name = file.name
            if (name.startsWith(".")) continue
            if (file.isDirectory) {
                if (name.equals("Android", ignoreCase = true) || name.equals(".trash-storage", ignoreCase = true)) continue
                scanDirectoryForFiles(file, results, depth - 1)
            } else {
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
