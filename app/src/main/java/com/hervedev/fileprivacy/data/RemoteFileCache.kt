package com.hervedev.fileprivacy.data

import android.content.Context
import java.io.File

object RemoteFileCache {

    fun getCacheFileFor(context: Context, connectionId: Long, fileName: String): File {
        val dir = File(context.cacheDir, "remote_previews/$connectionId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, fileName)
    }

    fun clearCache(context: Context) {
        try {
            val dir = File(context.cacheDir, "remote_previews")
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        } catch (_: Exception) {}
    }
}
