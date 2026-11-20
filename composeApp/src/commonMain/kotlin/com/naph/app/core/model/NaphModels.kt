package com.naph.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Stremio v3 Manifest Protocol & NAPH Unified Media Domain Models
 */
@Serializable
data class AddonManifest(
    val id: String,
    val version: String,
    val name: String,
    val description: String = "",
    val logo: String? = null,
    val background: String? = null,
    val types: List<String> = emptyList(),
    val catalogs: List<AddonCatalogDef> = emptyList(),
    val idPrefixes: List<String>? = null,
    val manifestUrl: String = "",
    val isEnabled: Boolean = true
)

@Serializable
data class AddonCatalogDef(
    val type: String,
    val id: String,
    val name: String = id
)

@Serializable
data class CatalogResponse(
    val metas: List<MediaItem> = emptyList()
)

@Serializable
data class MetaResponse(
    val meta: MediaItem? = null
)

@Serializable
data class MediaItem(
    val id: String,
    val type: String = "movie", // "movie" or "series" or "anime"
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val genres: List<String> = emptyList(),
    val runtime: String? = null,
    val director: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val videos: List<EpisodeVideo> = emptyList(),
    val trailers: List<TrailerSource> = emptyList(),
    val trailerStreams: List<TrailerStream> = emptyList(),
    val aggregatedRatings: AggregatedRatings? = null,
    val sourceOrigin: String = "Addon" // "Addon", "Jellyfin", "Emby"
)

@Serializable
data class EpisodeVideo(
    val id: String,
    val title: String = "",
    val name: String? = null,
    val season: Int = 1,
    val episode: Int = 1,
    val overview: String? = null,
    val thumbnail: String? = null,
    val released: String? = null
) {
    val displayTitle: String
        get() = title.ifBlank { name ?: "Episode $episode" }
}

@Serializable
data class TrailerSource(
    val source: String, // YouTube Video ID (e.g. "dQw4w9WgXcQ")
    val type: String = "Trailer"
)

@Serializable
data class TrailerStream(
    val title: String = "Official Trailer",
    val ytId: String
)

/**
 * Aggregated Ratings from IMDb, Rotten Tomatoes, Metacritic, Trakt, and MDBList
 */
@Serializable
data class AggregatedRatings(
    val imdb: String? = null,
    val rottenTomatoesCritics: String? = null,
    val rottenTomatoesAudience: String? = null,
    val metacritic: String? = null,
    val trakt: String? = null,
    val mdbListScore: String? = null
)

/**
 * Stremio Stream & P2P / Debrid / Jellyfin Unified Stream Object
 */
@Serializable
data class StreamResponse(
    val streams: List<MediaStream> = emptyList()
)

@Serializable
data class MediaStream(
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val sources: List<String>? = null,
    val externalUrl: String? = null,
    val ytId: String? = null,
    val behaviorHints: StreamBehaviorHints? = null,
    val addonName: String = "Community Addon",
    val isDebridCached: Boolean = false,
    val debridProvider: String? = null
) {
    val isP2PTorrent: Boolean
        get() = !infoHash.isNullOrBlank() && url.isNullOrBlank()

    val displayName: String
        get() = name ?: addonName

    val displayDetails: String
        get() = title ?: description ?: (url ?: "P2P InfoHash: ${infoHash?.take(10)}...")

    val detectedResolution: String
        get() {
            val combined = "${name.orEmpty()} ${title.orEmpty()} ${description.orEmpty()}".uppercase()
            return when {
                "2160P" in combined || "4K" in combined || "UHD" in combined -> "4K UHD"
                "1080P" in combined || "FHD" in combined -> "1080p"
                "720P" in combined || "HD" in combined -> "720p"
                else -> "HD"
            }
        }

    val detectedHdrBadge: String?
        get() {
            val combined = "${name.orEmpty()} ${title.orEmpty()} ${description.orEmpty()}".uppercase()
            return when {
                "DV" in combined || "DOLBY VISION" in combined -> "Dolby Vision"
                "HDR10+" in combined -> "HDR10+"
                "HDR" in combined -> "HDR"
                else -> null
            }
        }

    val parsedSeeders: Int?
        get() {
            val text = "${title.orEmpty()} ${description.orEmpty()}"
            val regex = Regex("""(?:👤|Seeds?:?\s*|S:\s*)(\d+)""", RegexOption.IGNORE_CASE)
            return regex.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }
}

@Serializable
data class StreamBehaviorHints(
    val bingeGroup: String? = null,
    val filename: String? = null,
    val videoSize: Long? = null,
    val notWebReady: Boolean? = null
)

/**
 * Subtitles (Stremio Subtitles + ASS/SSA Live Appearance Style)
 */
@Serializable
data class SubtitleResponse(
    val subtitles: List<SubtitleTrack> = emptyList()
)

@Serializable
data class SubtitleTrack(
    val id: String,
    val url: String,
    val lang: String,
    val label: String = lang.uppercase(),
    val format: String = "srt" // "srt", "vtt", "ass", "ssa"
)

@Serializable
data class SubtitleAppearanceConfig(
    val fontFamily: String = "Sans-Serif",
    val fontSizeSp: Float = 22f,
    val textColorHex: Long = 0xFFFFFFFF,
    val outlineColorHex: Long = 0xFF000000,
    val outlineWidthPx: Float = 2.5f,
    val backgroundOpacity: Float = 0.45f,
    val verticalOffsetDp: Float = 28f,
    val delayOffsetMs: Long = 0L,
    val preserveAssStyling: Boolean = true
)

/**
 * IntroDB / AniSkip Auto-Skip Segments (Intros, Recaps, Outros)
 */
@Serializable
data class SkipSegment(
    val type: SkipSegmentType,
    val startMs: Long,
    val endMs: Long
)

enum class SkipSegmentType(val label: String) {
    INTRO("Skip Intro"),
    RECAP("Skip Recap"),
    OUTRO("Skip Credits")
}

/**
 * NAPH Household Profile & Watch Progress (Synced via Supabase Cloud)
 */
@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val avatarColorHex: Long,
    val avatarEmoji: String = "🎬",
    val isKidsProfile: Boolean = false,
    val pinCode: String? = null,
    val preferredAudioLang: String = "eng",
    val preferredSubLang: String = "eng",
    val autoSkipIntros: Boolean = true,
    val autoPlayNextEpisode: Boolean = true
)

@Serializable
data class WatchProgressEntry(
    val profileId: String,
    val mediaId: String,
    val mediaType: String,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val episodeId: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val episodeTitle: String? = null,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtEpochMs: Long
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

/**
 * P2P Engine Configuration & Real-Time Telemetry
 */
@Serializable
data class P2PSettings(
    val useTempCacheAutoClean: Boolean = true,
    val keepDownloadedFiles: Boolean = false,
    val customSaveDirectory: String = "",
    val maxCacheSizeGb: Int = 6,
    val maxDownloadSpeedKbps: Int = 0, // 0 = Unlimited
    val maxUploadSpeedKbps: Int = 512,
    val enableDht: Boolean = true,
    val enablePeerExchange: Boolean = true,
    val externalTorrServerUrl: String = "" // Optional external TorrServer override
)

data class P2PStreamTelemetry(
    val infoHash: String = "",
    val fileName: String = "",
    val localStreamUrl: String = "",
    val isReadyForPlayback: Boolean = false,
    val bufferProgressPercent: Float = 0f,
    val downloadSpeedBytesPerSec: Long = 0L,
    val uploadSpeedBytesPerSec: Long = 0L,
    val connectedPeers: Int = 0,
    val totalSeeds: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalFileBytes: Long = 0L,
    val headerPiecesReady: Boolean = false
)

/**
 * Debrid & Personal Media Server Configurations
 */
@Serializable
data class DebridConfig(
    val realDebridApiKey: String = "",
    val allDebridApiKey: String = "",
    val torBoxApiKey: String = "",
    val premiumizeApiKey: String = "",
    val preferDebridOverP2P: Boolean = false
)

@Serializable
data class MediaServerConnection(
    val id: String,
    val type: ServerType, // JELLYFIN or EMBY
    val name: String,
    val serverUrl: String,
    val username: String,
    val accessToken: String,
    val userId: String
)

@Serializable
enum class ServerType {
    JELLYFIN,
    EMBY
}

@Serializable
data class TrackerSyncConfig(
    val traktAccessToken: String = "",
    val traktUsername: String = "",
    val simklAccessToken: String = "",
    val simklUsername: String = "",
    val mdbListApiKey: String = "",
    val autoScrobbleEnabled: Boolean = true
)
