package com.hervedev.fileprivacy.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WebDavConnectionDao {

    @Query("SELECT * FROM webdav_connections ORDER BY name ASC")
    fun getAll(): Flow<List<WebDavConnectionEntity>>

    @Query("SELECT * FROM webdav_connections WHERE id = :id")
    suspend fun getById(id: Long): WebDavConnectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: WebDavConnectionEntity): Long

    @Delete
    suspend fun delete(connection: WebDavConnectionEntity)
}
