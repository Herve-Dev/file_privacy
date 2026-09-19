package com.hervedev.fileprivacy.data

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockftpserver.fake.FakeFtpServer
import org.mockftpserver.fake.UserAccount
import org.mockftpserver.fake.filesystem.DirectoryEntry
import org.mockftpserver.fake.filesystem.FileEntry
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem

class FtpFileSourceTest {

    private lateinit var fakeFtpServer: FakeFtpServer
    private lateinit var ftpFileSource: FtpFileSource
    private var ftpPort: Int = 0

    @Before
    fun setUp() {
        fakeFtpServer = FakeFtpServer()
        fakeFtpServer.serverControlPort = 0 // Auto-assign free port

        val userAccount = UserAccount("testuser", "testpass", "/")
        fakeFtpServer.addUserAccount(userAccount)

        val fileSystem = UnixFakeFileSystem()
        fileSystem.add(DirectoryEntry("/"))
        fileSystem.add(DirectoryEntry("/documents"))
        fileSystem.add(FileEntry("/documents/report.pdf", "Contenu du rapport PDF"))
        fileSystem.add(FileEntry("/documents/notes.txt", "Notes de texte"))
        fileSystem.add(DirectoryEntry("/photos"))
        fileSystem.add(FileEntry("/photos/cat.jpg", "Image fake"))

        // Directory with nested files for recursive delete test
        fileSystem.add(DirectoryEntry("/to_delete"))
        fileSystem.add(FileEntry("/to_delete/f1.txt", "Fichier 1"))
        fileSystem.add(FileEntry("/to_delete/f2.txt", "Fichier 2"))
        fileSystem.add(DirectoryEntry("/to_delete/sub_dir"))
        fileSystem.add(FileEntry("/to_delete/sub_dir/f3.txt", "Fichier 3"))

        fakeFtpServer.fileSystem = fileSystem
        fakeFtpServer.start()

        ftpPort = fakeFtpServer.serverControlPort

        ftpFileSource = FtpFileSource(
            serverAddress = "localhost",
            port = ftpPort,
            username = "testuser",
            password = "testpass",
            useFtps = false
        )
    }

    @After
    fun tearDown() {
        if (::fakeFtpServer.isInitialized) {
            fakeFtpServer.stop()
        }
    }

    @Test
    fun testListFilesRoot() = runBlocking {
        val items = ftpFileSource.listFiles("/")
        assertTrue(items.isNotEmpty())
        val names = items.map { it.name }
        assertTrue(names.contains("documents"))
        assertTrue(names.contains("photos"))
        assertTrue(names.contains("to_delete"))
    }

    @Test
    fun testListFilesSubDirectory() = runBlocking {
        val items = ftpFileSource.listFiles("documents")
        assertEquals(2, items.size)
        val names = items.map { it.name }
        assertTrue(names.contains("report.pdf"))
        assertTrue(names.contains("notes.txt"))
    }

    @Test
    fun testDeleteSimpleFile() = runBlocking {
        val deleteSuccess = ftpFileSource.deleteFile("photos/cat.jpg")
        assertTrue(deleteSuccess)

        val remainingItems = ftpFileSource.listFiles("photos")
        assertFalse(remainingItems.any { it.name == "cat.jpg" })
    }

    @Test
    fun testDeleteNonEmptyDirectoryRecursively() = runBlocking {
        // Confirm directory exists before deletion
        val initialRoot = ftpFileSource.listFiles("/")
        assertTrue(initialRoot.any { it.name == "to_delete" })

        // Delete entire non-empty directory
        val deleteSuccess = ftpFileSource.deleteFile("to_delete")
        assertTrue(deleteSuccess)

        // Verify directory no longer exists at root
        val remainingRoot = ftpFileSource.listFiles("/")
        assertFalse(remainingRoot.any { it.name == "to_delete" })
    }

    @Test
    fun testRenameFile() = runBlocking {
        val renameSuccess = ftpFileSource.renameFile("documents/notes.txt", "renamed_notes.txt")
        assertTrue(renameSuccess)

        val remainingItems = ftpFileSource.listFiles("documents")
        val names = remainingItems.map { it.name }
        assertFalse(names.contains("notes.txt"))
        assertTrue(names.contains("renamed_notes.txt"))
    }

    @Test
    fun testCreateFolder() = runBlocking {
        val createSuccess = ftpFileSource.createFolder("new_folder")
        assertTrue(createSuccess)

        val items = ftpFileSource.listFiles("/")
        assertTrue(items.any { it.name == "new_folder" && it.isDirectory })
    }

    @Test
    fun testCopyFile() = runBlocking {
        val copySuccess = ftpFileSource.copyFile("documents/report.pdf", "photos/report_copy.pdf")
        assertTrue(copySuccess)

        // Verify source still exists
        val docItems = ftpFileSource.listFiles("documents")
        assertTrue(docItems.any { it.name == "report.pdf" })

        // Verify destination exists in photos folder
        val photoItems = ftpFileSource.listFiles("photos")
        assertTrue(photoItems.any { it.name == "report_copy.pdf" })
    }

    @Test
    fun testDeleteNonExistentFileReturnsFalse() = runBlocking {
        val deleteSuccess = ftpFileSource.deleteFile("non_existent_file.xyz")
        assertFalse(deleteSuccess)
    }
}
