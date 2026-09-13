package com.hervedev.fileprivacy.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SmbConnectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SmbConnectionEntity): Long

    @Delete
    suspend fun delete(entity: SmbConnectionEntity)

    @Query("SELECT * FROM smb_connections")
    fun getAll(): Flow<List<SmbConnectionEntity>>

    @Query("SELECT * FROM smb_connections WHERE id = :id")
    suspend fun getById(id: Long): SmbConnectionEntity?
}
