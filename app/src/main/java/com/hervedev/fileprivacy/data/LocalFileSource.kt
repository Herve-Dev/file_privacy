package com.hervedev.fileprivacy.data

import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.FileSystemProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalFileSource : FileSystemProvider {

    override suspend fun listFiles(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            return@withContext emptyList()
        }
        val files = dir.listFiles() ?: return@withContext emptyList()
        files.map { f ->
            FileItem(
                name = f.name,
                path = f.absolutePath,
                isDirectory = f.isDirectory,
                sizeBytes = f.length(),
                lastModified = f.lastModified()
            )
        }.sortedWith(
            compareByDescending<FileItem> { it.isDirectory }
                .thenBy { it.name.lowercase() }
        )
    }

    override suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        val target = File(path)
        if (!target.exists()) return@withContext false
        target.deleteRecursively()
    }

    override suspend fun renameFile(path: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val target = File(path)
        if (!target.exists()) return@withContext false
        val parent = target.parentFile ?: return@withContext false
        val dest = File(parent, newName)
        target.renameTo(dest)
    }

    override suspend fun copyFile(source: String, destination: String): Boolean = withContext(Dispatchers.IO) {
        val srcFile = File(source)
        val destFile = File(destination)
        if (!srcFile.exists()) return@withContext false
        try {
            if (srcFile.isDirectory) {
                srcFile.copyRecursively(destFile, overwrite = true)
            } else {
                srcFile.copyTo(destFile, overwrite = true)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun createFolder(path: String): Boolean = withContext(Dispatchers.IO) {
        val dir = File(path)
        dir.mkdirs()
    }
}
