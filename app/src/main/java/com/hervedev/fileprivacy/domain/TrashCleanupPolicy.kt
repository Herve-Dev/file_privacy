package com.hervedev.fileprivacy.domain

import com.hervedev.fileprivacy.data.db.TrashEntryEntity

object TrashCleanupPolicy {

    /**
     * Identifies entries in the trash that have exceeded the retention period.
     * An entry is expired if (now - deletedAt) > retentionDays in milliseconds.
     */
    fun findExpiredTrashEntries(
        entries: List<TrashEntryEntity>,
        retentionDays: Int,
        now: Long = System.currentTimeMillis()
    ): List<TrashEntryEntity> {
        if (retentionDays <= 0) return emptyList()
        val retentionMillis = retentionDays * 24L * 60L * 60L * 1000L
        return entries.filter { entry ->
            (now - entry.deletedAt) > retentionMillis
        }
    }
}
