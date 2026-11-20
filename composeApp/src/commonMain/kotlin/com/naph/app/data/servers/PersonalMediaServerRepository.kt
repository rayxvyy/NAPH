package com.naph.app.data.servers

import com.naph.app.core.model.MediaItem
import com.naph.app.core.model.MediaServerConnection
import com.naph.app.core.model.MediaStream
import com.naph.app.core.model.ServerType
import com.naph.app.core.network.NaphNetwork
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Personal Media Server Client for Jellyfin & Emby
 * Authenticates via `/Users/AuthenticateByName`, fetches Movies & Series from the user's
 * personal server, and builds direct-play / universal stream URLs.
 */
class PersonalMediaServerRepository {
    private val client = NaphNetwork.httpClient

    private fun authHeader(): String =
        """MediaBrowser Client="NAPH", Device="NAPH Multiplatform", DeviceId="naph-kmp-v1", Version="1.0.0""""

    suspend fun authenticateServer(
        type: ServerType,
        name: String,
        rawServerUrl: String,
        username: String,
        password: String
    ): Result<MediaServerConnection> = runCatching {
        val baseUrl = rawServerUrl.trim().trimEnd('/')
        val endpoint = if (type == ServerType.EMBY && !baseUrl.endsWith("/emby")) {
            "$baseUrl/Users/AuthenticateByName"
        } else {
            "$baseUrl/Users/AuthenticateByName"
        }

        val responseText = client.post(endpoint) {
            contentType(ContentType.Application.Json)
            header("X-Emby-Authorization", authHeader())
            setBody(
                buildJsonObject {
                    put("Username", username)
                    put("Pw", password)
                }.toString()
            )
        }.bodyAsText()

        val json = NaphNetwork.json.parseToJsonElement(responseText).jsonObject
        val token = json["AccessToken"]?.jsonPrimitive?.content
            ?: error("Authentication failed: No AccessToken returned")
        val userId = json["User"]?.jsonObject?.get("Id")?.jsonPrimitive?.content
            ?: error("Authentication failed: No UserId returned")

        MediaServerConnection(
            id = "${type.name.lowercase()}_${userId.take(8)}",
            type = type,
            name = name.ifBlank { "${type.name.lowercase().replaceFirstChar { it.uppercase() }} ($username)" },
            serverUrl = baseUrl,
            username = username,
            accessToken = token,
            userId = userId
        )
    }

    suspend fun fetchLibraryItems(server: MediaServerConnection): List<MediaItem> = runCatching {
        val base = server.serverUrl.trimEnd('/')
        val url = "$base/Users/${server.userId}/Items?IncludeItemTypes=Movie,Series&Recursive=true&Fields=Overview,Genres,CommunityRating,ProductionYear&Limit=60"

        val responseText = client.get(url) {
            header("X-Emby-Token", server.accessToken)
            header("X-Emby-Authorization", authHeader())
        }.bodyAsText()

        val itemsArray = NaphNetwork.json.parseToJsonElement(responseText)
            .jsonObject["Items"]?.jsonArray ?: return@runCatching emptyList()

        itemsArray.mapNotNull { element ->
            val obj = element.jsonObject
            val itemId = obj["Id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val title = obj["Name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val itemType = obj["Type"]?.jsonPrimitive?.content ?: "Movie"
            val year = obj["ProductionYear"]?.jsonPrimitive?.content
            val rating = obj["CommunityRating"]?.jsonPrimitive?.content
            val overview = obj["Overview"]?.jsonPrimitive?.content

            MediaItem(
                id = "${server.type.name.lowercase()}:${server.id}:$itemId",
                type = if (itemType.equals("Series", ignoreCase = true)) "series" else "movie",
                name = title,
                poster = "$base/Items/$itemId/Images/Primary?maxHeight=600&quality=90",
                background = "$base/Items/$itemId/Images/Backdrop?maxWidth=1920&quality=85",
                description = overview ?: "Hosted on ${server.name}",
                releaseInfo = year,
                imdbRating = rating,
                sourceOrigin = server.type.name
            )
        }
    }.getOrDefault(emptyList())

    fun buildDirectPlayStream(server: MediaServerConnection, rawItemId: String): MediaStream {
        val cleanId = rawItemId.substringAfterLast(":")
        val base = server.serverUrl.trimEnd('/')
        val streamUrl = "$base/Videos/$cleanId/stream?static=true&api_key=${server.accessToken}"
        return MediaStream(
            name = "${server.type.name}\nDirect Play",
            title = "Personal Server (${server.name}) • Original Quality Direct Stream",
            url = streamUrl,
            addonName = server.name,
            isDebridCached = true,
            debridProvider = server.type.name
        )
    }
}
