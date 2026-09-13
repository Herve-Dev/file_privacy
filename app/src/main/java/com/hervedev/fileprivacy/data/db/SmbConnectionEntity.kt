package com.hervedev.fileprivacy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hervedev.fileprivacy.domain.SmbConnection

@Entity(tableName = "smb_connections")
data class SmbConnectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val serverAddress: String,
    val shareName: String,
    val username: String,
    val port: Int = 445
) {
    fun toDomain(): SmbConnection = SmbConnection(
        id = id,
        name = name,
        serverAddress = serverAddress,
        shareName = shareName,
        username = username,
        port = port
    )

    companion object {
        fun fromDomain(domain: SmbConnection): SmbConnectionEntity = SmbConnectionEntity(
            id = domain.id,
            name = domain.name,
            serverAddress = domain.serverAddress,
            shareName = domain.shareName,
            username = domain.username,
            port = domain.port
        )
    }
}
