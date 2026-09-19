package com.hervedev.fileprivacy.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface FileSystemProvider {
    suspend fun listFiles(path: String): List<FileItem>
    suspend fun deleteFile(path: String): Boolean
    suspend fun renameFile(path: String, newName: String): Boolean
    suspend fun copyFile(source: String, destination: String): Boolean
    suspend fun createFolder(path: String): Boolean

    // Trash & Cache extensions
    suspend fun moveToTrash(path: String): Boolean
    suspend fun restoreFromTrash(path: String): Boolean
    suspend fun listTrash(): List<FileItem>
    suspend fun permanentlyDelete(path: String): Boolean
    suspend fun findFirstImageThumbnail(folderPath: String, maxDepth: Int = 1): String?

    suspend fun downloadToCache(path: String, destinationFile: File): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val sourceFile = File(path)
                if (sourceFile.exists()) {
                    sourceFile.copyTo(destinationFile, overwrite = true)
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}
