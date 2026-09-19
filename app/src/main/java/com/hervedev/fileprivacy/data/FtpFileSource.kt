package com.hervedev.fileprivacy.data

import android.util.Log
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.FileSystemProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient
import java.io.File
import java.io.IOException
import java.util.Locale

/**
 * FtpFileSource - Apache Commons Net FTP/FTPS File System Provider.
 *
 * PATH CONVENTION:
 * 'path' represents a relative path from the FTP root directory (e.g. "" or "documents/reports").
 * Root is represented by "" or "/".
 *
 * Each operation connects on-demand via [withFtpConnection] and disconnects cleanly.
 */
class FtpFileSource(
    private val serverAddress: String,
    private val port: Int,
    private val username: String,
    private val password: String,
    private val useFtps: Boolean
) : FileSystemProvider {

    companion object {
        private const val TAG = "FtpFileSource"
        private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")

        private fun isImageFileName(filename: String): Boolean {
            val dotIndex = filename.lastIndexOf('.')
            if (dotIndex <= 0 || dotIndex == filename.length - 1) return false
            val ext = filename.substring(dotIndex + 1).lowercase(Locale.getDefault())
            return IMAGE_EXTS.contains(ext)
        }
    }

    private suspend fun <T> withFtpConnection(block: suspend (FTPClient) -> T): T = withContext(Dispatchers.IO) {
        val client: FTPClient = if (useFtps) {
            FTPSClient()
        } else {
            FTPClient()
        }

        try {
            client.connectTimeout = 10000
            client.setDefaultTimeout(10000)
            client.connect(serverAddress, port)

            val replyCode = client.replyCode
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                throw IOException("Échec de connexion FTP (Code $replyCode)")
            }

            val loggedIn = client.login(username, password)
            if (!loggedIn) {
                throw IOException("Identifiants FTP incorrects")
            }

            client.enterLocalPassiveMode()
            client.setFileType(FTP.BINARY_FILE_TYPE)

            block(client)
        } finally {
            if (client.isConnected) {
                try {
                    client.logout()
                } catch (_: Exception) {}
                try {
                    client.disconnect()
                } catch (_: Exception) {}
            }
        }
    }

    override suspend fun listFiles(path: String): List<FileItem> {
        return try {
            withFtpConnection { client ->
                val targetPath = if (path.isBlank() || path == "/") "" else path.trimStart('/')
                val files = client.listFiles(targetPath.ifEmpty { "/" }) ?: emptyArray()

                files.filter { file ->
                    val name = file.name
                    name != "." && name != ".."
                }.map { file ->
                    val itemPath = if (targetPath.isEmpty()) file.name else "$targetPath/${file.name}"
                    FileItem(
                        name = file.name,
                        path = itemPath,
                        isDirectory = file.isDirectory,
                        sizeBytes = file.size,
                        lastModified = file.timestamp?.timeInMillis ?: 0L
                    )
                }.sortedWith(
                    compareByDescending<FileItem> { it.isDirectory }
                        .thenBy { it.name.lowercase(Locale.getDefault()) }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur listFiles FTP ($path): ${e.localizedMessage}", e)
            emptyList()
        }
    }

    override suspend fun deleteFile(path: String): Boolean {
        return try {
            withFtpConnection { client ->
                val targetPath = path.trimStart('/')
                val parent = targetPath.substringBeforeLast('/', "")
                val name = targetPath.substringAfterLast('/')
                val parentFiles = client.listFiles(parent.ifEmpty { "/" }) ?: emptyArray()

                val targetFile = parentFiles.firstOrNull { it.name == name } ?: return@withFtpConnection false

                if (targetFile.isDirectory) {
                    deleteDirectoryRecursive(client, targetPath)
                } else {
                    client.deleteFile(targetPath)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur deleteFile FTP ($path): ${e.localizedMessage}", e)
            false
        }
    }

    private fun deleteDirectoryRecursive(client: FTPClient, path: String): Boolean {
        val files = client.listFiles(path) ?: emptyArray()

        for (file in files) {
            val name = file.name
            if (name == "." || name == "..") continue

            val itemPath = if (path.isEmpty()) name else "$path/$name"
            if (file.isDirectory) {
                deleteDirectoryRecursive(client, itemPath)
            } else {
                client.deleteFile(itemPath)
            }
        }

        return client.removeDirectory(path)
    }

    override suspend fun renameFile(path: String, newName: String): Boolean {
        return try {
            withFtpConnection { client ->
                val targetPath = path.trimStart('/')
                val parent = targetPath.substringBeforeLast('/', "")
                val newPath = if (parent.isEmpty()) newName else "$parent/$newName"
                client.rename(targetPath, newPath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur renameFile FTP ($path -> $newName): ${e.localizedMessage}", e)
            false
        }
    }

    override suspend fun copyFile(source: String, destination: String): Boolean {
        var tempFile: File? = null
        return try {
            tempFile = File.createTempFile("ftp_copy_", null)

            // Step 1: Download source to temporary local file
            val downloaded = withFtpConnection { client ->
                val srcPath = source.trimStart('/')
                tempFile.outputStream().use { output ->
                    client.retrieveFile(srcPath, output)
                }
            }

            if (!downloaded || !tempFile.exists() || tempFile.length() == 0L) {
                return false
            }

            // Step 2: Upload temporary local file to destination
            val uploaded = withFtpConnection { client ->
                val destPath = destination.trimStart('/')
                tempFile.inputStream().use { input ->
                    client.storeFile(destPath, input)
                }
            }

            uploaded
        } catch (e: Exception) {
            Log.e(TAG, "Erreur copyFile FTP ($source -> $destination): ${e.localizedMessage}", e)
            false
        } finally {
            tempFile?.delete()
        }
    }

    override suspend fun createFolder(path: String): Boolean {
        return try {
            withFtpConnection { client ->
                val targetPath = path.trimStart('/')
                client.makeDirectory(targetPath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur createFolder FTP ($path): ${e.localizedMessage}", e)
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
            withFtpConnection { client ->
                val targetPath = if (folderPath.isBlank() || folderPath == "/") "" else folderPath.trimStart('/')
                val files = client.listFiles(targetPath.ifEmpty { "/" }) ?: emptyArray()

                for (file in files) {
                    if (file.isFile && isImageFileName(file.name)) {
                        return@withFtpConnection if (targetPath.isEmpty()) file.name else "$targetPath/${file.name}"
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
