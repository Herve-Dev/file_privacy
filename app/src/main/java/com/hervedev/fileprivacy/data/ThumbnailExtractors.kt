package com.hervedev.fileprivacy.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object ThumbnailExtractors {

    private val bitmapCache = ConcurrentHashMap<String, Bitmap>()

    suspend fun extractAudioThumbnail(path: String): Bitmap? = withContext(Dispatchers.IO) {
        bitmapCache[path]?.let { return@withContext it }

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val picture = retriever.embeddedPicture
            retriever.release()

            if (picture != null) {
                val bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                if (bitmap != null) {
                    bitmapCache[path] = bitmap
                    return@withContext bitmap
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun extractPdfThumbnail(path: String): Bitmap? = withContext(Dispatchers.IO) {
        bitmapCache[path]?.let { return@withContext it }

        var fileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null

        try {
            val file = File(path)
            if (!file.exists()) return@withContext null

            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor)

            if (pdfRenderer.pageCount > 0) {
                page = pdfRenderer.openPage(0)
                val width = 200
                val height = (width * (page.height.toFloat() / page.width.toFloat())).toInt().coerceAtLeast(200)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                bitmapCache[path] = bitmap
                return@withContext bitmap
            }
            null
        } catch (_: Exception) {
            null
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { pdfRenderer?.close() } catch (_: Exception) {}
            try { fileDescriptor?.close() } catch (_: Exception) {}
        }
    }

    suspend fun extractApkIcon(context: Context, path: String): Bitmap? = withContext(Dispatchers.IO) {
        bitmapCache[path]?.let { return@withContext it }

        try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES)

            val appInfo = packageInfo?.applicationInfo
            if (appInfo != null) {
                appInfo.sourceDir = path
                appInfo.publicSourceDir = path
                val drawable = appInfo.loadIcon(packageManager)

                val bitmap = drawableToBitmap(drawable)
                if (bitmap != null) {
                    bitmapCache[path] = bitmap
                    return@withContext bitmap
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        return try {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }
}
