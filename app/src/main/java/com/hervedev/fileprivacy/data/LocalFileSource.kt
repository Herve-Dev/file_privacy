package com.hervedev.fileprivacy.data

import android.content.Context
import android.os.Environment
import com.hervedev.fileprivacy.data.db.AppDatabase
import com.hervedev.fileprivacy.data.db.TrashEntryEntity
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.FileSystemProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "dng", "raw")

private fun isImageFileName(filename: String): Boolean {
    val dotIndex = filename.lastIndexOf('.')
    if (dotIndex <= 0 || dotIndex == filename.length - 1) return false
    val ext = filename.substring(dotIndex + 1).lowercase(Locale.getDefault())
    return IMAGE_EXTS.contains(ext)
}

class LocalFileSource(private val context: Context? = null) : FileSystemProvider {

    private val trashDao by lazy {
        context?.let { AppDatabase.getInstance(it).trashEntryDao() }
    }

    private val trashDirectory: File
        get() {
            val root = Environment.getExternalStorageDirectory()
            val trash = File(root, ".trash-storage")
            if (!trash.exists()) {
                trash.mkdirs()
            }
            return trash
        }

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

    override suspend fun moveToTrash(path: String): Boolean = withContext(Dispatchers.IO) {
        val srcFile = File(path)
        if (!srcFile.exists()) return@withContext false

        try {
            val timestamp = System.currentTimeMillis()
            val trashDir = trashDirectory
            val trashFile = File(trashDir, "${timestamp}_${srcFile.name}")

            val moved = srcFile.renameTo(trashFile)
            if (moved) {
                trashDao?.insert(
                    TrashEntryEntity(
                        originalPath = srcFile.absolutePath,
                        trashPath = trashFile.absolutePath,
                        fileName = srcFile.name,
                        isDirectory = trashFile.isDirectory,
                        sizeBytes = trashFile.length(),
                        deletedAt = timestamp
                    )
                )
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun restoreFromTrash(path: String): Boolean = withContext(Dispatchers.IO) {
        val trashFile = File(path)
        if (!trashFile.exists()) return@withContext false

        try {
            val dao = trashDao ?: return@withContext false
            val entry = dao.getByTrashPath(path) ?: return@withContext false
            val destFile = File(entry.originalPath)
            destFile.parentFile?.mkdirs()

            val restored = trashFile.renameTo(destFile)
            if (restored) {
                dao.delete(entry)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun listTrash(): List<FileItem> = withContext(Dispatchers.IO) {
        try {
            val dao = trashDao ?: return@withContext emptyList()
            val entries = dao.getAll().firstOrNull() ?: emptyList()
            entries.map { entry ->
                FileItem(
                    name = entry.fileName,
                    path = entry.trashPath,
                    isDirectory = entry.isDirectory,
                    sizeBytes = entry.sizeBytes,
                    lastModified = entry.deletedAt
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun permanentlyDelete(path: String): Boolean = withContext(Dispatchers.IO) {
        val target = File(path)
        val dao = trashDao
        try {
            if (target.exists()) {
                target.deleteRecursively()
            }
            dao?.deleteByTrashPath(path)
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun findFirstImageThumbnail(folderPath: String, maxDepth: Int): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(folderPath)
            if (!dir.exists() || !dir.isDirectory) return@withContext null
            val files = dir.listFiles() ?: return@withContext null

            // 1. Scan direct files first
            for (f in files) {
                if (f.isFile && isImageFileName(f.name)) {
                    return@withContext f.absolutePath
                }
            }

            // 2. Scan direct subdirectories if maxDepth > 1
            if (maxDepth > 1) {
                for (f in files) {
                    if (f.isDirectory) {
                        val subFiles = f.listFiles() ?: continue
                        for (sub in subFiles) {
                            if (sub.isFile && isImageFileName(sub.name)) {
                                return@withContext sub.absolutePath
                            }
                        }
                    }
                }
            }

            null
        } catch (_: Exception) {
            null
        }
    }
}
