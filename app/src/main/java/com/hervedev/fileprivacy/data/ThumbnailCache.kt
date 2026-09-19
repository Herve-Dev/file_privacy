package com.hervedev.fileprivacy.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.FileSystemProvider
import com.hervedev.fileprivacy.domain.isImage
import com.hervedev.fileprivacy.domain.isVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

object ThumbnailCache {

    private const val TAG = "ThumbnailCache"
    private const val MAX_THUMBNAIL_DIM = 300
    private const val DEFAULT_MAX_CACHE_SIZE = 50L * 1024L * 1024L // 50 MB

    private fun md5(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            input.hashCode().toString()
        }
    }

    suspend fun getOrCreateThumbnail(
        context: Context,
        connectionId: Long,
        connectionType: String,
        fileSystemProvider: FileSystemProvider,
        item: FileItem
    ): File? = withContext(Dispatchers.IO) {
        if (!item.isImage() && !item.isVideo()) return@withContext null

        val dir = File(context.cacheDir, "thumbnails/${connectionType}_$connectionId")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val thumbFile = File(dir, "${md5(item.path)}.jpg")
        if (thumbFile.exists() && thumbFile.length() > 0) {
            return@withContext thumbFile
        }

        var tempSource: File? = null
        try {
            tempSource = File(context.cacheDir, "temp_thumb_${System.currentTimeMillis()}_${md5(item.name)}")
            val downloaded = fileSystemProvider.downloadToCache(item.path, tempSource)

            if (!downloaded || !tempSource.exists() || tempSource.length() == 0L) {
                return@withContext null
            }

            val bitmap: Bitmap? = when {
                item.isImage() -> decodeScaledImageBitmap(tempSource, MAX_THUMBNAIL_DIM)
                item.isVideo() -> extractVideoFrame(tempSource, MAX_THUMBNAIL_DIM)
                else -> null
            }

            if (bitmap != null) {
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
                if (thumbFile.exists() && thumbFile.length() > 0) {
                    return@withContext thumbFile
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Erreur génération miniature (${item.path}): ${e.localizedMessage}", e)
            null
        } finally {
            tempSource?.delete()
        }
    }

    private fun decodeScaledImageBitmap(file: File, maxDim: Int): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            val w = boundsOptions.outWidth
            val h = boundsOptions.outHeight
            if (w <= 0 || h <= 0) return null

            var inSampleSize = 1
            while (w / (inSampleSize * 2) >= maxDim && h / (inSampleSize * 2) >= maxDim) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null
            scaleBitmap(decoded, maxDim)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur décodage image miniature: ${e.localizedMessage}")
            null
        }
    }

    private fun extractVideoFrame(file: File, maxDim: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val frame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
                ?: return null
            scaleBitmap(frame, maxDim)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur extraction frame vidéo miniature: ${e.localizedMessage}")
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap

        val aspect = w.toFloat() / h.toFloat()
        val targetW: Int
        val targetH: Int
        if (w > h) {
            targetW = maxDim
            targetH = (maxDim / aspect).toInt().coerceAtLeast(1)
        } else {
            targetH = maxDim
            targetW = (maxDim * aspect).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    fun enforceCacheSizeLimit(context: Context, maxSizeBytes: Long = DEFAULT_MAX_CACHE_SIZE) {
        try {
            val thumbsDir = File(context.cacheDir, "thumbnails")
            if (!thumbsDir.exists()) return

            val allFiles = thumbsDir.walkTopDown().filter { it.isFile }.toList()
            var currentTotal = allFiles.sumOf { it.length() }

            if (currentTotal > maxSizeBytes) {
                val sortedFiles = allFiles.sortedBy { it.lastModified() }
                for (file in sortedFiles) {
                    val len = file.length()
                    if (file.delete()) {
                        currentTotal -= len
                    }
                    if (currentTotal <= maxSizeBytes) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur éviction LRU du cache de miniatures: ${e.localizedMessage}")
        }
    }
}
