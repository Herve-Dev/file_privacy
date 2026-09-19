package com.hervedev.fileprivacy.data

import android.util.Log
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.FileSystemProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * WebDavFileSource - Custom WebDAV File System Provider using OkHttp and native XmlPullParser.
 *
 * PATH CONVENTION:
 * 'path' represents a relative path from the WebDAV basePath.
 * E.g. "" or "documents/report.pdf".
 */
class WebDavFileSource(
    private val serverUrl: String,
    private val port: Int,
    private val basePath: String,
    private val username: String,
    private val password: String
) : FileSystemProvider {

    companion object {
        private const val TAG = "WebDavFileSource"
        private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")

        private const val PROPFIND_XML = """<?xml version="1.0" encoding="utf-8" ?><D:propfind xmlns:D="DAV:"><D:allprop/></D:propfind>"""

        private fun isImageFileName(filename: String): Boolean {
            val dotIndex = filename.lastIndexOf('.')
            if (dotIndex <= 0 || dotIndex == filename.length - 1) return false
            val ext = filename.substring(dotIndex + 1).lowercase(Locale.getDefault())
            return IMAGE_EXTS.contains(ext)
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                if (username.isNotBlank()) {
                    requestBuilder.header("Authorization", Credentials.basic(username, password))
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    private fun buildUrl(relativePath: String, isFolder: Boolean = false): String {
        val rootUrl = WebDavConnectionTester.buildFullWebDavUrl(serverUrl, port, basePath)
        val cleanRel = relativePath.trim().trim('/')
        if (cleanRel.isEmpty()) return rootUrl

        val pathWithSlash = if (isFolder) "$cleanRel/" else cleanRel
        val rootWithSlash = if (rootUrl.endsWith("/")) rootUrl else "$rootUrl/"
        return rootWithSlash + pathWithSlash
    }

    override suspend fun listFiles(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(path, isFolder = true)
            val mediaType = "application/xml; charset=utf-8".toMediaType()
            val requestBody = PROPFIND_XML.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .method("PROPFIND", requestBody)
                .header("Content-Type", "application/xml; charset=utf-8")
                .header("Depth", "1")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.code != 207 && response.code != 200) {
                Log.e(TAG, "PROPFIND $url a renvoyé le code HTTP ${response.code}")
                return@withContext emptyList()
            }

            val xmlBody = response.body?.string() ?: return@withContext emptyList()
            val parsedItems = parsePropfindResponse(xmlBody, path)

            parsedItems.sortedWith(
                compareByDescending<FileItem> { it.isDirectory }
                    .thenBy { it.name.lowercase(Locale.getDefault()) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur listFiles WebDAV ($path): ${e.localizedMessage}", e)
            emptyList()
        }
    }

    private fun parsePropfindResponse(xml: String, currentRelPath: String): List<FileItem> {
        val items = mutableListOf<FileItem>()
        val cleanRelPath = currentRelPath.trim().trim('/')

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inResponse = false
            var href: String? = null
            var isDirectory = false
            var contentLength = 0L
            var lastModified = 0L

            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name?.lowercase(Locale.getDefault()) ?: ""

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = tagName
                        if (tagName == "response") {
                            inResponse = true
                            href = null
                            isDirectory = false
                            contentLength = 0L
                            lastModified = 0L
                        } else if (tagName == "collection") {
                            isDirectory = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim() ?: ""
                        if (inResponse && text.isNotEmpty()) {
                            when (currentTag) {
                                "href" -> href = text
                                "getcontentlength" -> contentLength = text.toLongOrNull() ?: 0L
                                "getlastmodified" -> lastModified = parseRfc1123Date(text)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "response" && inResponse) {
                            inResponse = false
                            href?.let { rawHref ->
                                val decodedHref = URLDecoder.decode(rawHref, "UTF-8").trimEnd('/')
                                val name = decodedHref.substringAfterLast('/')

                                if (name.isNotEmpty()) {
                                    val itemRelPath = if (cleanRelPath.isEmpty()) name else "$cleanRelPath/$name"

                                    val isParent = itemRelPath.equals(cleanRelPath, ignoreCase = true) ||
                                            decodedHref.endsWith(cleanRelPath, ignoreCase = true) && name == cleanRelPath.substringAfterLast('/')

                                    if (!isParent && name != "." && name != "..") {
                                        items.add(
                                            FileItem(
                                                name = name,
                                                path = itemRelPath,
                                                isDirectory = isDirectory,
                                                sizeBytes = contentLength,
                                                lastModified = lastModified
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur de parsing XML WebDAV: ${e.localizedMessage}", e)
        }

        return items
    }

    private fun parseRfc1123Date(dateStr: String): Long {
        return try {
            val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
            format.timeZone = TimeZone.getTimeZone("GMT")
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    override suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(path, isFolder = false)
            val request = Request.Builder()
                .url(url)
                .delete()
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful || response.code == 204 || response.code == 200
        } catch (e: Exception) {
            Log.e(TAG, "Erreur deleteFile WebDAV ($path): ${e.localizedMessage}", e)
            false
        }
    }

    override suspend fun renameFile(path: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceUrl = buildUrl(path, isFolder = false)
            val parent = path.trim().trim('/').substringBeforeLast('/', "")
            val newRelPath = if (parent.isEmpty()) newName else "$parent/$newName"
            val destUrl = buildUrl(newRelPath, isFolder = false)

            val request = Request.Builder()
                .url(sourceUrl)
                .method("MOVE", null)
                .header("Destination", destUrl)
                .header("Overwrite", "F")
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful || response.code == 201 || response.code == 204 || response.code == 200
        } catch (e: Exception) {
            Log.e(TAG, "Erreur renameFile WebDAV ($path -> $newName): ${e.localizedMessage}", e)
            false
        }
    }

    override suspend fun copyFile(source: String, destination: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceUrl = buildUrl(source, isFolder = false)
            val destUrl = buildUrl(destination, isFolder = false)

            val request = Request.Builder()
                .url(sourceUrl)
                .method("COPY", null)
                .header("Destination", destUrl)
                .header("Overwrite", "F")
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful || response.code == 201 || response.code == 204 || response.code == 200
        } catch (e: Exception) {
            Log.e(TAG, "Erreur copyFile WebDAV ($source -> $destination): ${e.localizedMessage}", e)
            false
        }
    }

    override suspend fun createFolder(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(path, isFolder = true)
            val request = Request.Builder()
                .url(url)
                .method("MKCOL", null)
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful || response.code == 201 || response.code == 200
        } catch (e: Exception) {
            Log.e(TAG, "Erreur createFolder WebDAV ($path): ${e.localizedMessage}", e)
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

    override suspend fun downloadToCache(path: String, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(path, isFolder = false)
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            val body = response.body ?: return@withContext false
            body.byteStream().use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erreur downloadToCache WebDAV ($path): ${e.localizedMessage}", e)
            false
        }
    }
}
