package com.hervedev.fileprivacy.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient

object FtpConnectionTester {

    suspend fun testFtpConnection(
        serverAddress: String,
        port: Int,
        username: String,
        password: String,
        useFtps: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val client: FTPClient = if (useFtps) {
            FTPSClient()
        } else {
            FTPClient()
        }

        try {
            client.connectTimeout = 5000
            client.setDefaultTimeout(5000)
            client.connect(serverAddress, port)

            val replyCode = client.replyCode
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                client.disconnect()
                return@withContext Result.failure(Exception("Échec de connexion au serveur FTP (Code $replyCode)"))
            }

            val loggedIn = client.login(username, password)
            if (!loggedIn) {
                client.logout()
                client.disconnect()
                return@withContext Result.failure(Exception("Identifiants de connexion FTP incorrects"))
            }

            client.enterLocalPassiveMode()
            val files = client.listFiles("/")
            if (files == null) {
                client.logout()
                client.disconnect()
                return@withContext Result.failure(Exception("Impossible de lister le contenu du serveur FTP"))
            }

            client.logout()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Erreur de connexion FTP"))
        } finally {
            if (client.isConnected) {
                try {
                    client.disconnect()
                } catch (_: Exception) {}
            }
        }
    }
}
