package com.hervedev.fileprivacy.domain

import com.hervedev.fileprivacy.data.db.TrashEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrashCleanupPolicyTest {

    @Test
    fun testExpiredEntryIsIdentified() {
        val now = 1000000000000L
        val retentionDays = 30
        val retentionMillis = retentionDays * 24L * 60L * 60L * 1000L

        // Entry deleted 31 days ago (expired)
        val expiredEntry = TrashEntryEntity(
            id = 1,
            originalPath = "/storage/emulated/0/doc.pdf",
            trashPath = "/storage/emulated/0/.trash-storage/1_doc.pdf",
            fileName = "doc.pdf",
            isDirectory = false,
            sizeBytes = 1024L,
            deletedAt = now - retentionMillis - (24L * 60L * 60L * 1000L)
        )

        val result = TrashCleanupPolicy.findExpiredTrashEntries(
            entries = listOf(expiredEntry),
            retentionDays = retentionDays,
            now = now
        )

        assertEquals(1, result.size)
        assertEquals(expiredEntry.id, result.first().id)
    }

    @Test
    fun testRecentEntryIsNotExpired() {
        val now = 1000000000000L
        val retentionDays = 30

        // Entry deleted 10 days ago (recent, NOT expired)
        val recentEntry = TrashEntryEntity(
            id = 2,
            originalPath = "/storage/emulated/0/photo.jpg",
            trashPath = "/storage/emulated/0/.trash-storage/2_photo.jpg",
            fileName = "photo.jpg",
            isDirectory = false,
            sizeBytes = 2048L,
            deletedAt = now - (10L * 24L * 60L * 60L * 1000L)
        )

        val result = TrashCleanupPolicy.findExpiredTrashEntries(
            entries = listOf(recentEntry),
            retentionDays = retentionDays,
            now = now
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun testEmptyListReturnsEmptyList() {
        val result = TrashCleanupPolicy.findExpiredTrashEntries(
            entries = emptyList(),
            retentionDays = 30,
            now = System.currentTimeMillis()
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun testMixedEntriesOnlyReturnsExpired() {
        val now = 1000000000000L
        val retentionDays = 7
        val retentionMillis = retentionDays * 24L * 60L * 60L * 1000L

        val expired1 = TrashEntryEntity(
            id = 1,
            originalPath = "/path/1",
            trashPath = "/trash/1",
            fileName = "f1",
            isDirectory = false,
            sizeBytes = 100,
            deletedAt = now - retentionMillis - 10000L
        )

        val recent1 = TrashEntryEntity(
            id = 2,
            originalPath = "/path/2",
            trashPath = "/trash/2",
            fileName = "f2",
            isDirectory = false,
            sizeBytes = 100,
            deletedAt = now - (2L * 24L * 60L * 60L * 1000L)
        )

        val expired2 = TrashEntryEntity(
            id = 3,
            originalPath = "/path/3",
            trashPath = "/trash/3",
            fileName = "f3",
            isDirectory = false,
            sizeBytes = 100,
            deletedAt = now - (10L * 24L * 60L * 60L * 1000L)
        )

        val result = TrashCleanupPolicy.findExpiredTrashEntries(
            entries = listOf(expired1, recent1, expired2),
            retentionDays = retentionDays,
            now = now
        )

        assertEquals(2, result.size)
        assertEquals(listOf(1L, 3L), result.map { it.id })
    }

    @Test
    fun testBoundaryConditionExactRetentionDays() {
        val now = 1000000000000L
        val retentionDays = 30
        val retentionMillis = retentionDays * 24L * 60L * 60L * 1000L

        // Entry deleted EXACTLY at the boundary timestamp: (now - deletedAt) == retentionMillis
        val exactBoundaryEntry = TrashEntryEntity(
            id = 4,
            originalPath = "/path/4",
            trashPath = "/trash/4",
            fileName = "f4",
            isDirectory = false,
            sizeBytes = 100,
            deletedAt = now - retentionMillis
        )

        val result = TrashCleanupPolicy.findExpiredTrashEntries(
            entries = listOf(exactBoundaryEntry),
            retentionDays = retentionDays,
            now = now
        )

        // Exclusive threshold (> retentionMillis): exact boundary is NOT expired yet
        assertTrue(result.isEmpty())
    }
}
