package com.naph.app.data.trackers

import com.naph.app.core.model.TrackerSyncConfig
import com.naph.app.core.network.NaphNetwork
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Optional 3rd-Party Watch History Scrobbling (Trakt.tv & SIMKL).
 * Zero cloud account or Supabase dependency — all profiles and addons stay 100% local on device.
 */
class CloudAndTrackerSyncRepository {
    private val client = NaphNetwork.httpClient

    suspend fun scrobblePlaybackEvent(
        action: String,
        imdbId: String,
        mediaType: String,
        progressPercent: Float,
        config: TrackerSyncConfig
    ) {
        if (!config.autoScrobbleEnabled) return

        if (config.traktAccessToken.isNotBlank()) {
            runCatching {
                client.post("https://api.trakt.tv/scrobble/$action") {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer ${config.traktAccessToken}")
                    header("trakt-api-version", "2")
                    setBody(
                        buildJsonObject {
                            put("progress", progressPercent)
                            put("app_version", "1.0.0")
                        }.toString()
                    )
                }
            }
        }

        if (config.simklAccessToken.isNotBlank()) {
            runCatching {
                client.post("https://api.simkl.com/scrobble/$action") {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer ${config.simklAccessToken}")
                    setBody(
                        buildJsonObject {
                            put("progress", progressPercent)
                        }.toString()
                    )
                }
            }
        }
    }
}
