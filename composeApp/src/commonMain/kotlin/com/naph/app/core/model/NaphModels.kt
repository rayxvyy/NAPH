package com.naph.app.core.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

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
    val name: String = id,
    val extra: List<AddonExtraProp> = emptyList()
) {
    val requiresExtraInput: Boolean
        get() = extra.any { it.isRequired }
}

@Serializable
data class AddonExtraProp(
    val name: String,
    val isRequired: Boolean = false,
    val options: List<String> = emptyList()
)

data class HomeCatalogSection(
    val key: String,
    val title: String,
    val addonName: String,
    val type: String,
    val catalogId: String,
    val manifestUrl: String,
    val items: List<MediaItem>
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
    val type: String = "movie",
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
    val sourceOrigin: String = "Addon"
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
    val source: String,
    val type: String = "Trailer"
)

@Serializable
data class TrailerStream(
    val title: String = "Official Trailer",
    val ytId: String
)

@Serializable
data class AggregatedRatings(
    val imdb: String? = null,
    val rottenTomatoesCritics: String? = null,
    val rottenTomatoesAudience: String? = null,
    val metacritic: String? = null,
    val trakt: String? = null,
    val mdbListScore: String? = null
)

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
    val addonName: String = "Addon",
    val isDebridCached: Boolean = false,
    val debridProvider: String? = null
) {
    val isP2PTorrent: Boolean
        get() = !infoHash.isNullOrBlank() && url.isNullOrBlank()

    val cleanTitleLine: String
        get() {
            val raw = behaviorHints?.filename
                ?: (title ?: description ?: "").lines().firstOrNull { it.isNotBlank() }
                ?: name
                ?: "Video Stream"
            return raw.trim()
        }

    val displayDetails: String
        get() = title ?: description ?: (url ?: "P2P InfoHash: ${infoHash?.take(10)}...")

    val detectedResolution: String
        get() {
            val combined = "${name.orEmpty()} ${title.orEmpty()} ${description.orEmpty()} ${behaviorHints?.filename.orEmpty()}".uppercase()
            return when {
                "2160P" in combined || "4K" in combined || "UHD" in combined -> "4K UHD"
                "1440P" in combined || "2K" in combined -> "1440p"
                "1080P" in combined || "FHD" in combined -> "1080p"
                "720P" in combined -> "720p"
                "480P" in combined -> "480p"
                else -> "HD"
            }
        }

    val detectedHdrBadge: String?
        get() {
            val combined = "${name.orEmpty()} ${title.orEmpty()} ${description.orEmpty()} ${behaviorHints?.filename.orEmpty()}".uppercase()
            return when {
                "DV" in combined || "DOLBY VISION" in combined || "DOVI" in combined -> "Dolby Vision"
                "HDR10+" in combined -> "HDR10+"
                "HDR" in combined -> "HDR10"
                else -> null
            }
        }

    val detectedVideoCodec: String?
        get() {
            val combined = "${name.orEmpty()} ${title.orEmpty()} ${description.orEmpty()} ${behaviorHints?.filename.orEmpty()}".uppercase()
            return when {
                "HEVC" in combined || "X265" in combined || "H265" in combined || "H.265" in combined -> "HEVC"
                "AV1" in combined -> "AV1"
                "AVC" in combined || "X264" in combined || "H264" in combined || "H.264" in combined -> "H.264"
                "REMUX" in combined -> "REMUX"
                else -> null
            }
        }

    val detectedAudioBadge: String?
        get() {
            val combined = "${name.orEmpty()} ${title.orEmpty()} ${description.orEmpty()} ${behaviorHints?.filename.orEmpty()}".uppercase()
            return when {
                "ATMOS" in combined -> "Dolby Atmos"
                "TRUEHD" in combined -> "TrueHD 7.1"
                "DTS-HD" in combined || "DTS:X" in combined -> "DTS-HD MA"
                "DDP5.1" in combined || "DD+ 5.1" in combined || "EAC3" in combined -> "DD+ 5.1"
                "5.1" in combined -> "5.1 Audio"
                "AAC" in combined -> "AAC"
                else -> null
            }
        }

    val detectedFileSizeLabel: String?
        get() {
            val bytes = behaviorHints?.videoSize
            if (bytes != null && bytes > 0L) {
                val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                return if (gb >= 1.0) {
                    val rounded = (gb * 100.0).roundToInt() / 100.0
                    "$rounded GB"
                } else {
                    val mb = (bytes.toDouble() / (1024.0 * 1024.0)).roundToInt()
                    "$mb MB"
                }
            }
            val text = "${title.orEmpty()} ${description.orEmpty()}"
            val sizeRegex = Regex("""💾?\s*(\d+(?:\.\d+)?\s*(?:GB|MB|GiB|MiB))""", RegexOption.IGNORE_CASE)
            return sizeRegex.find(text)?.groupValues?.getOrNull(1)?.uppercase()
        }

    val parsedSeeders: Int?
        get() {
            val text = "${title.orEmpty()} ${description.orEmpty()}"
            val regex = Regex("""(?:👤|Seeds?:?\s*|S:\s*)(\d+)""", RegexOption.IGNORE_CASE)
            return regex.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }

    val detectedSourceQuality: String?
        get() {
            val combined = "${title.orEmpty()} ${description.orEmpty()} ${behaviorHints?.filename.orEmpty()}".uppercase()
            return when {
                "REMUX" in combined -> "BluRay REMUX"
                "BLURAY" in combined || "BLU-RAY" in combined -> "BluRay"
                "WEB-DL" in combined || "WEBDL" in combined -> "WEB-DL"
                "WEBRIP" in combined -> "WEBRip"
                else -> null
            }
        }
}

@Serializable
data class StreamBehaviorHints(
    val bingeGroup: String? = null,
    val filename: String? = null,
    val videoSize: Long? = null,
    val notWebReady: Boolean? = null
)

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
    val format: String = "srt"
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

@Serializable
data class P2PSettings(
    val useTempCacheAutoClean: Boolean = true,
    val keepDownloadedFiles: Boolean = false,
    val customSaveDirectory: String = "",
    val maxCacheSizeGb: Int = 6,
    val maxDownloadSpeedKbps: Int = 0,
    val maxUploadSpeedKbps: Int = 512,
    val enableDht: Boolean = true,
    val enablePeerExchange: Boolean = true,
    val externalTorrServerUrl: String = ""
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
    val type: ServerType,
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
    val tmdbApiKey: String = "",
    val autoScrobbleEnabled: Boolean = true
)

enum class SidebarBehaviorMode(val label: String) {
    AUTO_EXPAND_HOVER("Apple TV Auto-Hover (Compact 64dp → Expands to 168dp on Hover)"),
    ALWAYS_COMPACT("Always Compact Icons (64dp)"),
    ALWAYS_EXPANDED("Always Expanded (168dp)")
}

enum class PosterCardSizePreset(val label: String, val widthDp: Int) {
    COMPACT("Compact (136dp)", 136),
    STANDARD("Netflix Standard (156dp)", 156),
    LARGE("Cinema Large (184dp)", 184)
}

@Serializable
data class LayoutCustomizationConfig(
    val sidebarMode: SidebarBehaviorMode = SidebarBehaviorMode.AUTO_EXPAND_HOVER,
    val posterSizePreset: PosterCardSizePreset = PosterCardSizePreset.STANDARD,
    val showPosterTitlesBelow: Boolean = false, // False by default = Pure Netflix Poster Artwork Only!
    val showRatingBadgeOnPoster: Boolean = false, // False by default = Clean un-cluttered Netflix Poster!
    val posterCornerRadiusDp: Int = 10,
    val showHeroShowcase: Boolean = true,
    val showCatalogTypeInHeader: Boolean = false,
    val showContinueWatchingRow: Boolean = true,
    // Individual Rating Source Toggles (Only IMDb ON by default — user can toggle Rotten Tomatoes, Metacritic, Trakt, MDBList in Settings!)
    val showImdbRating: Boolean = true,
    val showRottenTomatoesCritics: Boolean = false,
    val showRottenTomatoesAudience: Boolean = false,
    val showMetacriticRating: Boolean = false,
    val showTraktRating: Boolean = false,
    val showMdbListRating: Boolean = false,
    // Stream Card Information Toggles
    val showStreamFileSizeBadge: Boolean = true,
    val showStreamSeedersBadge: Boolean = true,
    val showStreamCodecAndAudioBadges: Boolean = true
)

@Serializable
data class WatchPartySession(
    val roomCode: String,
    val hostName: String,
    val activeMediaTitle: String = "Waiting for playback...",
    val participantCount: Int = 1,
    val isConnected: Boolean = true
)
