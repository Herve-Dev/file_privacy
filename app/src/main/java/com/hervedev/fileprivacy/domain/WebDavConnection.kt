package com.hervedev.fileprivacy.domain

data class WebDavConnection(
    val id: Long = 0,
    val name: String,
    val serverUrl: String,
    val username: String,
    val basePath: String = ""
)
