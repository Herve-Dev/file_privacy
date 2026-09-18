package com.hervedev.fileprivacy.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FtpConnectionDao {

    @Query("SELECT * FROM ftp_connections ORDER BY name ASC")
    fun getAll(): Flow<List<FtpConnectionEntity>>

    @Query("SELECT * FROM ftp_connections WHERE id = :id")
    suspend fun getById(id: Long): FtpConnectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: FtpConnectionEntity): Long

    @Delete
    suspend fun delete(connection: FtpConnectionEntity)
}
