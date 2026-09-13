package com.hervedev.fileprivacy.data

import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import java.util.Properties

object SmbUtils {

    fun createCifsContext(username: String, password: String): CIFSContext {
        val props = Properties()
        props.setProperty("jcifs.smb.client.responseTimeout", "10000")
        props.setProperty("jcifs.smb.client.soTimeout", "10000")
        props.setProperty("jcifs.smb.client.connTimeout", "10000")
        props.setProperty("jcifs.smb.client.minVersion", "SMB202")
        props.setProperty("jcifs.smb.client.maxVersion", "SMB311")

        val config = PropertyConfiguration(props)
        val baseContext = BaseContext(config)

        val auth = if (username.isNotBlank()) {
            NtlmPasswordAuthenticator("", username, password)
        } else {
            NtlmPasswordAuthenticator()
        }

        return baseContext.withCredentials(auth)
    }

    fun buildSmbUrl(
        serverAddress: String,
        shareName: String,
        relativePath: String,
        port: Int = 445,
        isDirectory: Boolean = true
    ): String {
        val cleanAddress = serverAddress.trim().removePrefix("smb://").removeSuffix("/")
        val cleanShare = shareName.trim().removePrefix("/").removeSuffix("/")
        val cleanPath = relativePath.trim().removePrefix("/").removeSuffix("/")

        val fullRelative = when {
            cleanShare.isEmpty() && cleanPath.isEmpty() -> ""
            cleanShare.isEmpty() -> cleanPath
            cleanPath.isEmpty() -> cleanShare
            else -> "$cleanShare/$cleanPath"
        }

        return if (fullRelative.isEmpty()) {
            "smb://$cleanAddress:$port/"
        } else if (isDirectory) {
            "smb://$cleanAddress:$port/$fullRelative/"
        } else {
            "smb://$cleanAddress:$port/$fullRelative"
        }
    }
}
