package com.hervedev.fileprivacy.data

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager

data class StorageVolumeInfo(
    val name: String,
    val path: String
)

data class StorageSpaceInfo(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long
)

object StorageVolumesHelper {

    fun getStorageSpaceInfo(path: String): StorageSpaceInfo? {
        return try {
            val statFs = StatFs(path)
            val totalBytes = statFs.totalBytes
            val freeBytes = statFs.availableBytes
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
            StorageSpaceInfo(
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                usedBytes = usedBytes
            )
        } catch (_: Exception) {
            null
        }
    }

    fun getExternalStorageVolumes(context: Context): List<StorageVolumeInfo> {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            ?: return emptyList()

        val volumes = storageManager.storageVolumes
        val result = mutableListOf<StorageVolumeInfo>()

        for (volume in volumes) {
            if (volume.isPrimary) continue

            val state = volume.state
            if (state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY) {
                val dir = volume.directory
                if (dir != null && dir.exists()) {
                    val name = volume.getDescription(context)
                    result.add(
                        StorageVolumeInfo(
                            name = if (!name.isNullOrBlank()) name else "Stockage externe",
                            path = dir.absolutePath
                        )
                    )
                }
            }
        }

        return result
    }
}
