package com.hervedev.fileprivacy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hervedev.fileprivacy.domain.FtpConnection

@Entity(tableName = "ftp_connections")
data class FtpConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val serverAddress: String,
    val port: Int = 21,
    val username: String,
    val useFtps: Boolean = false
) {
    fun toDomain(): FtpConnection = FtpConnection(
        id = id,
        name = name,
        serverAddress = serverAddress,
        port = port,
        username = username,
        useFtps = useFtps
    )

    companion object {
        fun fromDomain(domain: FtpConnection): FtpConnectionEntity = FtpConnectionEntity(
            id = domain.id,
            name = domain.name,
            serverAddress = domain.serverAddress,
            port = domain.port,
            username = domain.username,
            useFtps = domain.useFtps
        )
    }
}
