package com.hervedev.fileprivacy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hervedev.fileprivacy.domain.WebDavConnection

@Entity(tableName = "webdav_connections")
data class WebDavConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val serverUrl: String,
    val username: String,
    val basePath: String = ""
) {
    fun toDomain(): WebDavConnection = WebDavConnection(
        id = id,
        name = name,
        serverUrl = serverUrl,
        username = username,
        basePath = basePath
    )

    companion object {
        fun fromDomain(domain: WebDavConnection): WebDavConnectionEntity = WebDavConnectionEntity(
            id = domain.id,
            name = domain.name,
            serverUrl = domain.serverUrl,
            username = domain.username,
            basePath = domain.basePath
        )
    }
}
