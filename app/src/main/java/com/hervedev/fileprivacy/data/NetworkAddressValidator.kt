package com.hervedev.fileprivacy.data
import java.net.InetAddress
/**
 * Valide qu'un hôte est une adresse de réseau privé RFC1918 ou loopback.
 *
 * Plages autorisées :
 * - 10.0.0.0/8
 * - 172.16.0.0/12
 * - 192.168.0.0/16
 * - 127.x.x.x (loopback IPv4)
 * - ::1 (loopback IPv6)
 * - localhost
 *
 * À appeler depuis un contexte Dispatchers.IO (InetAddress.getByName peut
 * effectuer une résolution DNS bloquante pour les noms d'hôtes).
 */
object NetworkAddressValidator {
    /**
     * Retourne true si [host] est une adresse de réseau privé RFC1918,
     * une adresse de liaison locale, ou une adresse loopback.
     * Retourne false si l'hôte ne peut pas être résolu ou est une adresse publique.
     */
    fun isPrivateOrLoopback(host: String): Boolean {
        val trimmed = host.trim().removePrefix("[").removeSuffix("]")
        if (trimmed.equals("localhost", ignoreCase = true)) return true
        return try {
            val addr = InetAddress.getByName(trimmed)
            addr.isLoopbackAddress || addr.isSiteLocalAddress || addr.isLinkLocalAddress
        } catch (_: Exception) {
            false
        }
    }
    /**
     * Retourne un Result.failure avec un message d'erreur explicite si la connexion
     * cleartext (HTTP/FTP non chiffré) vers [host] doit être refusée.
     * Retourne null si la connexion est autorisée.
     *
     * [protocolLabel] : ex. "FTP" ou "WebDAV/HTTP", utilisé dans le message d'erreur.
     */
    fun validateCleartextHost(host: String, protocolLabel: String): Result<Unit>? {
        return if (!isPrivateOrLoopback(host)) {
            Result.failure(
                SecurityException(
                    "Connexion $protocolLabel non chiffrée refusée : l'hôte « $host » " +
                    "n'est pas une adresse de réseau privé (RFC1918) ni une adresse loopback. " +
                    "Utilisez FTPS ou HTTPS pour les connexions vers des hôtes publics."
                )
            )
        } else {
            null // connexion autorisée
        }
    }
}
