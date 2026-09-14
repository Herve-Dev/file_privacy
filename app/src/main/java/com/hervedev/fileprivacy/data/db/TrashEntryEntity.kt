package com.hervedev.fileprivacy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_entries")
data class TrashEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalPath: String,
    val trashPath: String,
    val fileName: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val deletedAt: Long = System.currentTimeMillis()
)
