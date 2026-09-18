package com.hervedev.fileprivacy.domain

data class FtpConnection(
    val id: Long = 0,
    val name: String,
    val serverAddress: String,
    val port: Int = 21,
    val username: String,
    val useFtps: Boolean = false
)
