package com.naph.app.data.debrid

import com.naph.app.core.model.DebridConfig
import com.naph.app.core.model.MediaStream
import com.naph.app.core.network.NaphNetwork
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Multi-Debrid Instant Stream Resolver
 * Supports: Real-Debrid, AllDebrid, TorBox, and Premiumize.
 * Converts P2P magnet/infoHash streams into high-speed cached HTTP 4K HDR links.
 */
class DebridResolverRepository {
    private val client = NaphNetwork.httpClient

    fun hasActiveDebrid(config: DebridConfig): Boolean {
        return config.realDebridApiKey.isNotBlank() ||
            config.allDebridApiKey.isNotBlank() ||
            config.torBoxApiKey.isNotBlank() ||
            config.premiumizeApiKey.isNotBlank()
    }

    fun activeProviderName(config: DebridConfig): String? = when {
        config.realDebridApiKey.isNotBlank() -> "Real-Debrid"
        config.torBoxApiKey.isNotBlank() -> "TorBox"
        config.allDebridApiKey.isNotBlank() -> "AllDebrid"
        config.premiumizeApiKey.isNotBlank() -> "Premiumize"
        else -> null
    }

    suspend fun resolveInfoHashToHttpUrl(
        stream: MediaStream,
        config: DebridConfig
    ): Result<String> = runCatching {
        if (!stream.url.isNullOrBlank()) return@runCatching stream.url
        val infoHash = stream.infoHash ?: error("Stream has no infoHash or HTTP URL")
        val magnet = "magnet:?xt=urn:btih:$infoHash"

        when {
            config.realDebridApiKey.isNotBlank() -> resolveWithRealDebrid(magnet, config.realDebridApiKey)
            config.torBoxApiKey.isNotBlank() -> resolveWithTorBox(infoHash, config.torBoxApiKey)
            config.allDebridApiKey.isNotBlank() -> resolveWithAllDebrid(magnet, config.allDebridApiKey)
            config.premiumizeApiKey.isNotBlank() -> resolveWithPremiumize(magnet, config.premiumizeApiKey)
            else -> error("No Debrid API key configured")
        }
    }

    private suspend fun resolveWithRealDebrid(magnet: String, apiKey: String): String {
        val addResponse = client.submitForm(
            url = "https://api.real-debrid.com/rest/1.0/torrents/addMagnet",
            formParameters = Parameters.build { append("magnet", magnet) }
        ) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }.bodyAsText()

        val torrentId = NaphNetwork.json.parseToJsonElement(addResponse)
            .jsonObject["id"]?.jsonPrimitive?.content ?: error("Real-Debrid addMagnet failed")

        client.submitForm(
            url = "https://api.real-debrid.com/rest/1.0/torrents/selectFiles/$torrentId",
            formParameters = Parameters.build { append("files", "all") }
        ) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }

        val infoText = client.get("https://api.real-debrid.com/rest/1.0/torrents/info/$torrentId") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }.bodyAsText()

        val firstLink = NaphNetwork.json.parseToJsonElement(infoText)
            .jsonObject["links"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
            ?: error("Real-Debrid link not ready yet")

        val unrestrictText = client.submitForm(
            url = "https://api.real-debrid.com/rest/1.0/unrestrict/link",
            formParameters = Parameters.build { append("link", firstLink) }
        ) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }.bodyAsText()

        return NaphNetwork.json.parseToJsonElement(unrestrictText)
            .jsonObject["download"]?.jsonPrimitive?.content
            ?: error("Real-Debrid unrestrict failed")
    }

    private suspend fun resolveWithTorBox(infoHash: String, apiKey: String): String {
        val checkText = client.get("https://api.torbox.app/v1/api/torrents/checkcached?hash=$infoHash&format=object") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }.bodyAsText()
        val parsed = NaphNetwork.json.parseToJsonElement(checkText).jsonObject
        if (parsed["success"]?.jsonPrimitive?.content != "true") {
            error("TorBox cache miss for $infoHash")
        }
        return "https://api.torbox.app/v1/api/torrents/requestdl?token=$apiKey&hash=$infoHash"
    }

    private suspend fun resolveWithAllDebrid(magnet: String, apiKey: String): String {
        val uploadText = client.get("https://api.alldebrid.com/v4/magnet/upload?agent=NAPH&apikey=$apiKey&magnets[]=$magnet")
            .bodyAsText()
        val parsed = NaphNetwork.json.parseToJsonElement(uploadText).jsonObject
        val status = parsed["status"]?.jsonPrimitive?.content
        if (status != "success") error("AllDebrid upload failed")
        return "https://api.alldebrid.com/v4/link/unlock?agent=NAPH&apikey=$apiKey"
    }

    private suspend fun resolveWithPremiumize(magnet: String, apiKey: String): String {
        val ddlText = client.submitForm(
            url = "https://www.premiumize.me/api/transfer/directdl",
            formParameters = Parameters.build {
                append("apikey", apiKey)
                append("src", magnet)
            }
        ).bodyAsText()

        val contentArr = NaphNetwork.json.parseToJsonElement(ddlText)
            .jsonObject["content"]?.jsonArray
        return contentArr?.firstOrNull()?.jsonObject?.get("stream_link")?.jsonPrimitive?.content
            ?: error("Premiumize directdl failed")
    }
}
