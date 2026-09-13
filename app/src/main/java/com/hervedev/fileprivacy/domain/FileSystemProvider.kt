package com.hervedev.fileprivacy.domain

interface FileSystemProvider {
    suspend fun listFiles(path: String): List<FileItem>
    suspend fun deleteFile(path: String): Boolean
    suspend fun renameFile(path: String, newName: String): Boolean
    suspend fun copyFile(source: String, destination: String): Boolean
    suspend fun createFolder(path: String): Boolean
}
