package com.hervedev.fileprivacy.data

import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.FileSystemProvider

class FtpFileSource : FileSystemProvider {
    override suspend fun listFiles(path: String): List<FileItem> {
        TODO("Non implémenté - Phase 4")
    }

    override suspend fun deleteFile(path: String): Boolean {
        TODO("Non implémenté - Phase 4")
    }

    override suspend fun renameFile(path: String, newName: String): Boolean {
        TODO("Non implémenté - Phase 4")
    }

    override suspend fun copyFile(source: String, destination: String): Boolean {
        TODO("Non implémenté - Phase 4")
    }

    override suspend fun createFolder(path: String): Boolean {
        TODO("Non implémenté - Phase 4")
    }
}
