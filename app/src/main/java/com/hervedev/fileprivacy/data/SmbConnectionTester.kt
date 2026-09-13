package com.hervedev.fileprivacy.data

import jcifs.smb.SmbAuthException
import jcifs.smb.SmbException
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object SmbConnectionTester {

    suspend fun testSmbConnection(
        serverAddress: String,
        shareName: String,
        username: String,
        password: String,
        port: Int = 445
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cifsContext = SmbUtils.createCifsContext(username, password)
            val smbUrl = SmbUtils.buildSmbUrl(
                serverAddress = serverAddress,
                shareName = shareName,
                relativePath = "",
                port = port,
                isDirectory = true
            )

            val smbFile = SmbFile(smbUrl, cifsContext)

            if (smbFile.exists()) {
                Result.success(Unit)
            } else {
                val cleanShare = shareName.trim().removePrefix("/").removeSuffix("/")
                Result.failure(Exception("Le partage '$cleanShare' n'existe pas ou n'est pas accessible."))
            }
        } catch (e: SmbAuthException) {
            Result.failure(Exception("Authentification refusée : Nom d'utilisateur ou mot de passe incorrect (${e.message})"))
        } catch (e: UnknownHostException) {
            Result.failure(Exception("Serveur introuvable : Impossible de résoudre l'adresse '$serverAddress'"))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Délai d'attente dépassé : Le serveur ne répond pas sur le port $port"))
        } catch (_: ConnectException) {
            Result.failure(Exception("Connexion refusée : Le serveur est injoignable sur le port $port"))
        } catch (e: SmbException) {
            Result.failure(Exception("Erreur SMB (${e.ntStatus}) : ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Erreur de connexion : ${e.localizedMessage ?: e.message}"))
        }
    }
}
