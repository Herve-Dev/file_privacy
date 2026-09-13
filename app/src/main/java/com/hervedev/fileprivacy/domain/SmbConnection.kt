package com.hervedev.fileprivacy.domain

data class SmbConnection(
    val id: Long = 0,
    val name: String,
    val serverAddress: String,
    val shareName: String,
    val username: String,
    val port: Int = 445
)
