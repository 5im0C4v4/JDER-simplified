package com.jder.data

import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val LATEST_RELEASE_URL = "https://api.github.com/repos/LoryPelli/JDER/releases/latest"
private val requestTimeout = Duration.ofSeconds(10)

private val releaseJson = Json { ignoreUnknownKeys = true }

@Serializable private class GitHubRelease(val assets: List<GitHubAsset> = emptyList())

@Serializable
private class GitHubAsset(
    val name: String,
    val size: Long,
    val digest: String? = null,
    @SerialName("browser_download_url") val downloadUrl: String,
)

object UpdateChecker {
    suspend fun findUpdate(): Result<String?> =
        withContext(Dispatchers.IO) { runCatching { checkLatestRelease() } }

    private fun checkLatestRelease(): String? {
        val jar = runningJar() ?: return null
        val asset = latestReleaseJar() ?: return null
        val remoteSha256 = asset.digest?.takeIf { it.startsWith("sha256:") }?.substringAfter(':')
        val isDifferent =
            asset.size != jar.length() ||
                (remoteSha256 != null && !remoteSha256.equals(sha256(jar), ignoreCase = true))
        return if (isDifferent) asset.downloadUrl else null
    }

    private fun runningJar(): File? {
        val location = javaClass.protectionDomain.codeSource?.location ?: return null
        return File(location.toURI()).takeIf { it.isFile }
    }

    private fun latestReleaseJar(): GitHubAsset? {
        val client =
            HttpClient.newBuilder()
                .connectTimeout(requestTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
        val request =
            HttpRequest.newBuilder(URI(LATEST_RELEASE_URL))
                .timeout(requestTimeout)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "JDER")
                .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IOException("GitHub ha risposto con il codice ${response.statusCode()}")
        }
        val body = response.body()
        return releaseJson.decodeFromString(GitHubRelease.serializer(), body).assets.firstOrNull {
            it.name.endsWith(".jar")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        DigestInputStream(file.inputStream(), digest).use {
            it.copyTo(OutputStream.nullOutputStream())
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
