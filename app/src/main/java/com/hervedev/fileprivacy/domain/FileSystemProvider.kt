package com.hervedev.fileprivacy.domain

interface FileSystemProvider {
    suspend fun listFiles(path: String): List<FileItem>
    suspend fun deleteFile(path: String): Boolean
    suspend fun renameFile(path: String, newName: String): Boolean
    suspend fun copyFile(source: String, destination: String): Boolean
    suspend fun createFolder(path: String): Boolean

    // Trash operations
    suspend fun moveToTrash(path: String): Boolean = false
    suspend fun restoreFromTrash(path: String): Boolean = false
    suspend fun listTrash(): List<FileItem> = emptyList()
    suspend fun permanentlyDelete(path: String): Boolean = deleteFile(path)

    // Thumbnail preview
    suspend fun findFirstImageThumbnail(folderPath: String, maxDepth: Int = 1): String? = null
}
