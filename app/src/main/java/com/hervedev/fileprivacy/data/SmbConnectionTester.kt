package com.hervedev.fileprivacy.data

import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbAuthException
import jcifs.smb.SmbException
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Properties

object SmbConnectionTester {

    suspend fun testSmbConnection(
        serverAddress: String,
        shareName: String,
        username: String,
        password: String,
        port: Int = 445
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
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

            val cifsContext: CIFSContext = baseContext.withCredentials(auth)

            val cleanAddress = serverAddress.trim().removePrefix("smb://").removeSuffix("/")
            val cleanShare = shareName.trim().removePrefix("/").removeSuffix("/")
            val smbUrl = "smb://$cleanAddress:$port/$cleanShare/"

            val smbFile = SmbFile(smbUrl, cifsContext)

            if (smbFile.exists()) {
                Result.success(Unit)
            } else {
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
