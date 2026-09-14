package com.hervedev.fileprivacy.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TrashEntryEntity): Long

    @Delete
    suspend fun delete(entry: TrashEntryEntity)

    @Query("DELETE FROM trash_entries WHERE trashPath = :trashPath")
    suspend fun deleteByTrashPath(trashPath: String)

    @Query("SELECT * FROM trash_entries ORDER BY deletedAt DESC")
    fun getAll(): Flow<List<TrashEntryEntity>>

    @Query("SELECT * FROM trash_entries WHERE trashPath = :trashPath LIMIT 1")
    suspend fun getByTrashPath(trashPath: String): TrashEntryEntity?

    @Query("SELECT COUNT(*) FROM trash_entries")
    fun getCount(): Flow<Int>
}
