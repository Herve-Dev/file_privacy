package com.hervedev.fileprivacy.data

import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.isApk
import com.hervedev.fileprivacy.domain.isAudio
import com.hervedev.fileprivacy.domain.isImage
import com.hervedev.fileprivacy.domain.isPdf
import com.hervedev.fileprivacy.domain.isVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.Locale

enum class DateFilter(val displayName: String) {
    TODAY("Aujourd'hui"),
    THIS_WEEK("Cette semaine"),
    THIS_MONTH("Ce mois")
}

object SearchScanner {

    suspend fun searchFiles(
        query: String,
        rootPath: String,
        typeFilter: FileCategory? = null,
        dateFilter: DateFilter? = null
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim().lowercase(Locale.getDefault())
        if (trimmedQuery.length < 2) return@withContext emptyList()

        val root = File(rootPath)
        if (!root.exists() || !root.isDirectory) return@withContext emptyList()

        val dateThreshold = calculateDateThreshold(dateFilter)

        val results = mutableListOf<FileItem>()
        scanAndFilterDirectory(
            dir = root,
            query = trimmedQuery,
            typeFilter = typeFilter,
            dateThreshold = dateThreshold,
            results = results,
            depth = 5
        )

        results.sortedWith(
            compareByDescending<FileItem> { it.name.lowercase(Locale.getDefault()) == trimmedQuery }
                .thenByDescending { it.name.lowercase(Locale.getDefault()).startsWith(trimmedQuery) }
                .thenByDescending { it.lastModified }
        )
    }

    private fun calculateDateThreshold(dateFilter: DateFilter?): Long {
        if (dateFilter == null) return 0L
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return when (dateFilter) {
            DateFilter.TODAY -> calendar.timeInMillis
            DateFilter.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.timeInMillis
            }
            DateFilter.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.timeInMillis
            }
        }
    }

    private fun scanAndFilterDirectory(
        dir: File,
        query: String,
        typeFilter: FileCategory?,
        dateThreshold: Long,
        results: MutableList<FileItem>,
        depth: Int
    ) {
        if (depth <= 0) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            val name = file.name
            if (name.startsWith(".")) continue
            if (file.isDirectory) {
                if (name.equals("Android", ignoreCase = true) || name.equals(".trash-storage", ignoreCase = true)) continue
                scanAndFilterDirectory(file, query, typeFilter, dateThreshold, results, depth - 1)
            } else {
                val item = FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = false,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified()
                )

                if (item.name.lowercase(Locale.getDefault()).contains(query)) {
                    if (dateThreshold == 0L || item.lastModified >= dateThreshold) {
                        if (matchesTypeFilter(item, typeFilter)) {
                            results.add(item)
                        }
                    }
                }
            }
        }
    }

    private fun matchesTypeFilter(item: FileItem, typeFilter: FileCategory?): Boolean {
        if (typeFilter == null) return true
        return when (typeFilter) {
            FileCategory.IMAGES -> item.isImage()
            FileCategory.VIDEOS -> item.isVideo()
            FileCategory.AUDIO -> item.isAudio()
            FileCategory.DOCUMENTS -> item.isPdf()
            FileCategory.DOWNLOADS -> item.path.contains("/Download/", ignoreCase = true)
            FileCategory.APK -> item.isApk()
        }
    }
}
