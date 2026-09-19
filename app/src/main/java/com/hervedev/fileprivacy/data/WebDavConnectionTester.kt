package com.hervedev.fileprivacy.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object WebDavConnectionTester {

    private val PROPFIND_XML = """
        <?xml version="1.0" encoding="utf-8" ?>
        <D:propfind xmlns:D="DAV:">
            <D:allprop/>
        </D:propfind>
    """.trimIndent()

    fun buildFullWebDavUrl(serverUrl: String, port: Int, basePath: String): String {
        val trimmedUrl = serverUrl.trim()
        val isHttps = trimmedUrl.startsWith("https://", ignoreCase = true)
        val scheme = if (isHttps) "https" else "http"

        val hostAndPath = trimmedUrl
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("HTTP://")
            .removePrefix("HTTPS://")
            .trimEnd('/')

        val host = hostAndPath.substringBefore('/').substringBefore(':')

        val defaultPort = if (isHttps) 443 else 80
        val hostWithPort = if (port == defaultPort || port <= 0) host else "$host:$port"

        val cleanPath = when {
            basePath.isBlank() -> "/"
            basePath.trim().startsWith("/") && basePath.trim().endsWith("/") -> basePath.trim()
            basePath.trim().startsWith("/") -> "${basePath.trim()}/"
            basePath.trim().endsWith("/") -> "/${basePath.trim()}"
            else -> "/${basePath.trim()}/"
        }

        return "$scheme://$hostWithPort$cleanPath"
    }

    suspend fun testWebDavConnection(
        serverUrl: String,
        port: Int,
        basePath: String,
        username: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fullUrl = buildFullWebDavUrl(serverUrl, port, basePath)

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val mediaType = "application/xml; charset=utf-8".toMediaType()
            val requestBody = PROPFIND_XML.toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url(fullUrl)
                .method("PROPFIND", requestBody)
                .header("Content-Type", "application/xml; charset=utf-8")
                .header("Depth", "0")

            if (username.isNotBlank()) {
                val credential = Credentials.basic(username, password)
                requestBuilder.header("Authorization", credential)
            }

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            val code = response.code

            when (code) {
                207, 200 -> Result.success(Unit)
                401 -> Result.failure(Exception("Identifiants incorrects (Erreur 401)"))
                403 -> Result.failure(Exception("Accès refusé par le serveur (Erreur 403)"))
                404 -> Result.failure(Exception("Chemin WebDAV introuvable sur le serveur (Erreur 404)"))
                405 -> Result.failure(Exception("Méthode non autorisée (Erreur 405) - Vérifiez l'adresse et le chemin WebDAV"))
                else -> Result.failure(Exception("Échec de connexion WebDAV (Code HTTP $code)"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Erreur de connexion WebDAV"))
        }
    }
}
