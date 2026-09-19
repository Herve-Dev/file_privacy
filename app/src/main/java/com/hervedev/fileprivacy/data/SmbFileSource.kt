package com.hervedev.fileprivacy.data

import android.util.Log
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.FileSystemProvider
import java.io.File
import java.util.Locale
import jcifs.CIFSContext
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implémentation du [FileSystemProvider] pour le protocole SMB (partages réseau).
 *
 * CONVENTION DE CHEMIN (PATH) :
 * Les paramètres [path] passés aux méthodes de cette classe représentent le chemin
 * RELATIF à l'intérieur du partage SMB (ex: "" pour la racine du partage,
 * "Documents/Factures.pdf" pour un fichier).
 * [SmbFileSource] reconstruit l'URL SMB complète ("smb://serveur:port/partage/chemin")
 * en interne avant chaque appel à jcifs-ng.
 */
class SmbFileSource(
    private val serverAddress: String,
    private val shareName: String,
    private val username: String,
    private val password: String,
    private val port: Int = 445
) : FileSystemProvider {

    private val tag = "SmbFileSource"

    private val cifsContext: CIFSContext by lazy {
        SmbUtils.createCifsContext(username, password)
    }

    private fun getUrl(relativePath: String, isDirectory: Boolean = true): String {
        return SmbUtils.buildSmbUrl(
            serverAddress = serverAddress,
            shareName = shareName,
            relativePath = relativePath,
            port = port,
            isDirectory = isDirectory
        )
    }

    override suspend fun listFiles(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        try {
            val url = getUrl(relativePath = path, isDirectory = true)
            val dir = SmbFile(url, cifsContext)

            if (!dir.exists() || !dir.isDirectory) {
                return@withContext emptyList()
            }

            val smbFiles = dir.listFiles() ?: return@withContext emptyList()

            val cleanRelativeParent = path.trim().removePrefix("/").removeSuffix("/")

            smbFiles.map { f ->
                val cleanName = f.name.removeSuffix("/")
                val isDir = f.isDirectory
                val childRelativePath = if (cleanRelativeParent.isEmpty()) {
                    cleanName
                } else {
                    "$cleanRelativeParent/$cleanName"
                }

                FileItem(
                    name = cleanName,
                    path = childRelativePath,
                    isDirectory = isDir,
                    sizeBytes = try { f.length() } catch (_: Exception) { 0L },
                    lastModified = try { f.lastModified() } catch (_: Exception) { 0L }
                )
            }.sortedWith(
                compareByDescending<FileItem> { it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
        } catch (e: Exception) {
            Log.e(tag, "Échec du listage SMB pour le chemin '$path'", e)
            emptyList()
        }
    }

    override suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = getUrl(relativePath = path, isDirectory = false)
            val smbFile = SmbFile(url, cifsContext)
            if (!smbFile.exists()) {
                return@withContext false
            }
            smbFile.delete()
            true
        } catch (e: Exception) {
            Log.e(tag, "Échec de la suppression SMB pour '$path'", e)
            false
        }
    }

    override suspend fun renameFile(path: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanPath = path.trim().removePrefix("/").removeSuffix("/")
            val srcUrl = getUrl(relativePath = cleanPath, isDirectory = false)
            val srcFile = SmbFile(srcUrl, cifsContext)

            if (!srcFile.exists()) {
                return@withContext false
            }

            val lastSlash = cleanPath.lastIndexOf('/')
            val parentPath = if (lastSlash >= 0) cleanPath.substring(0, lastSlash) else ""
            val destRelative = if (parentPath.isEmpty()) newName else "$parentPath/$newName"
            val destUrl = getUrl(relativePath = destRelative, isDirectory = srcFile.isDirectory)
            val destFile = SmbFile(destUrl, cifsContext)

            srcFile.renameTo(destFile)
            true
        } catch (e: Exception) {
            Log.e(tag, "Échec du renommage SMB de '$path' vers '$newName'", e)
            false
        }
    }

    override suspend fun copyFile(source: String, destination: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val srcUrl = getUrl(relativePath = source, isDirectory = false)
            val srcFile = SmbFile(srcUrl, cifsContext)

            if (!srcFile.exists()) {
                return@withContext false
            }

            val destUrl = getUrl(relativePath = destination, isDirectory = srcFile.isDirectory)
            val destFile = SmbFile(destUrl, cifsContext)

            srcFile.copyTo(destFile)
            true
        } catch (e: Exception) {
            Log.e(tag, "Échec de la copie SMB de '$source' vers '$destination'", e)
            false
        }
    }

    override suspend fun createFolder(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = getUrl(relativePath = path, isDirectory = true)
            val dir = SmbFile(url, cifsContext)
            dir.mkdirs()
            true
        } catch (e: Exception) {
            Log.e(tag, "Échec de la création de dossier SMB pour '$path'", e)
            false
        }
    }

    override suspend fun downloadToCache(path: String, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = getUrl(relativePath = path, isDirectory = false)
            val smbFile = SmbFile(url, cifsContext)
            if (!smbFile.exists()) return@withContext false

            smbFile.inputStream.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "Échec du téléchargement SMB vers le cache pour '$path'", e)
            false
        }
    }

    override suspend fun moveToTrash(path: String): Boolean {
        return deleteFile(path)
    }

    override suspend fun restoreFromTrash(path: String): Boolean {
        return false
    }

    override suspend fun listTrash(): List<FileItem> {
        return emptyList()
    }

    override suspend fun permanentlyDelete(path: String): Boolean {
        return deleteFile(path)
    }

    override suspend fun findFirstImageThumbnail(folderPath: String, maxDepth: Int): String? {
        return try {
            val items = listFiles(folderPath)
            items.firstOrNull { !it.isDirectory && isImageFileName(it.name) }?.path
        } catch (_: Exception) {
            null
        }
    }

    private fun isImageFileName(filename: String): Boolean {
        val dotIndex = filename.lastIndexOf('.')
        if (dotIndex <= 0 || dotIndex == filename.length - 1) return false
        val ext = filename.substring(dotIndex + 1).lowercase(Locale.getDefault())
        return setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic").contains(ext)
    }
}
