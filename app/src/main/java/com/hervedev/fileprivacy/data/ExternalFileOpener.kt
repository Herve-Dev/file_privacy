package com.hervedev.fileprivacy.data

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

object ExternalFileOpener {

    fun getMimeType(filePath: String): String {
        val file = File(filePath)
        val dotIndex = file.name.lastIndexOf('.')
        if (dotIndex < 0 || dotIndex == file.name.length - 1) return "*/*"
        val ext = file.name.substring(dotIndex + 1).lowercase(Locale.getDefault())
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    fun openFileExternally(
        context: Context,
        filePath: String,
        mimeType: String = getMimeType(filePath),
        chooserTitle: String = "Ouvrir avec"
    ): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null || pm.queryIntentActivities(intent, 0).isNotEmpty()) {
                context.startActivity(chooser)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
