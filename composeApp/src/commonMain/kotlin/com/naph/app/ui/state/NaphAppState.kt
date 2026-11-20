package com.naph.app.ui.state

import com.naph.app.core.model.AddonManifest
import com.naph.app.core.model.DebridConfig
import com.naph.app.core.model.EpisodeVideo
import com.naph.app.core.model.MediaItem
import com.naph.app.core.model.MediaServerConnection
import com.naph.app.core.model.MediaStream
import com.naph.app.core.model.P2PSettings
import com.naph.app.core.model.P2PStreamTelemetry
import com.naph.app.core.model.ServerType
import com.naph.app.core.model.SkipSegment
import com.naph.app.core.model.SubtitleAppearanceConfig
import com.naph.app.core.model.SubtitleTrack
import com.naph.app.core.model.TrackerSyncConfig
import com.naph.app.core.model.UserProfile
import com.naph.app.core.model.WatchProgressEntry
import com.naph.app.data.addons.StremioAddonRepository
import com.naph.app.data.debrid.DebridResolverRepository
import com.naph.app.data.metadata.MetadataAndSkipRepository
import com.naph.app.data.servers.PersonalMediaServerRepository
import com.naph.app.data.torrent.P2PTorrentStreamEngine
import com.naph.app.data.trackers.CloudAndTrackerSyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

enum class NaphTab(val label: String) {
    HOME("Home"),
    SEARCH("Explore"),
    LIBRARY("Library & Servers"),
    ADDONS("Addons & P2P"),
    SETTINGS("Settings")
}

class NaphAppState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val addonRepo = StremioAddonRepository()
    val p2pEngine = P2PTorrentStreamEngine()
    val debridRepo = DebridResolverRepository()
    val serverRepo = PersonalMediaServerRepository()
    val metaRepo = MetadataAndSkipRepository()
    val syncRepo = CloudAndTrackerSyncRepository()

    // Navigation & Modal States
    private val _showProfilePicker = MutableStateFlow(false)
    val showProfilePicker: StateFlow<Boolean> = _showProfilePicker.asStateFlow()

    private val _currentTab = MutableStateFlow(NaphTab.HOME)
    val currentTab: StateFlow<NaphTab> = _currentTab.asStateFlow()

    private val _selectedMediaItem = MutableStateFlow<MediaItem?>(null)
    val selectedMediaItem: StateFlow<MediaItem?> = _selectedMediaItem.asStateFlow()

    private val _activeTrailerYtId = MutableStateFlow<String?>(null)
    val activeTrailerYtId: StateFlow<String?> = _activeTrailerYtId.asStateFlow()

    // Profiles
    private val _profiles = MutableStateFlow(
        listOf(
            UserProfile(id = "prof_main", name = "Main Cinema", avatarColorHex = 0xFF6366F1, avatarEmoji = "🎬"),
            UserProfile(id = "prof_anime", name = "Anime & 4K", avatarColorHex = 0xFF06B6D4, avatarEmoji = "⚡"),
            UserProfile(id = "prof_family", name = "Family Room", avatarColorHex = 0xFF10B981, avatarEmoji = "🍿", isKidsProfile = false)
        )
    )
    val profiles: StateFlow<List<UserProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow(_profiles.value.first())
    val activeProfile: StateFlow<UserProfile> = _activeProfile.asStateFlow()

    // Installed Addons
    private val _installedAddons = MutableStateFlow(StremioAddonRepository.defaultInstalledAddons())
    val installedAddons: StateFlow<List<AddonManifest>> = _installedAddons.asStateFlow()

    // Home Catalogs
    private val _heroShowcaseItems = MutableStateFlow(StremioAddonRepository.defaultShowcaseItems())
    val heroShowcaseItems: StateFlow<List<MediaItem>> = _heroShowcaseItems.asStateFlow()

    private val _trendingMovies = MutableStateFlow(StremioAddonRepository.defaultShowcaseItems().filter { it.type == "movie" })
    val trendingMovies: StateFlow<List<MediaItem>> = _trendingMovies.asStateFlow()

    private val _trendingSeries = MutableStateFlow(StremioAddonRepository.defaultShowcaseItems().filter { it.type == "series" })
    val trendingSeries: StateFlow<List<MediaItem>> = _trendingSeries.asStateFlow()

    private val _personalServerItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val personalServerItems: StateFlow<List<MediaItem>> = _personalServerItems.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaItem>>(StremioAddonRepository.defaultShowcaseItems())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()

    // Watchlist & Continue Watching (Per Profile)
    private val _watchlistByProfile = MutableStateFlow<Map<String, List<MediaItem>>>(
        mapOf("prof_main" to StremioAddonRepository.defaultShowcaseItems().take(2))
    )
    val watchlistByProfile: StateFlow<Map<String, List<MediaItem>>> = _watchlistByProfile.asStateFlow()

    private val _watchProgressByProfile = MutableStateFlow<Map<String, List<WatchProgressEntry>>>(
        mapOf(
            "prof_main" to listOf(
                WatchProgressEntry(
                    profileId = "prof_main",
                    mediaId = "tt15239678",
                    mediaType = "movie",
                    title = "Dune: Part Two",
                    posterUrl = "https://images.metahub.space/poster/medium/tt15239678/img",
                    backdropUrl = "https://images.metahub.space/background/medium/tt15239678/img",
                    positionMs = 4_120_000L,
                    durationMs = 9_960_000L,
                    updatedAtEpochMs = Clock.System.now().toEpochMilliseconds()
                ),
                WatchProgressEntry(
                    profileId = "prof_main",
                    mediaId = "tt11126994",
                    mediaType = "series",
                    title = "Arcane",
                    posterUrl = "https://images.metahub.space/poster/medium/tt11126994/img",
                    backdropUrl = "https://images.metahub.space/background/medium/tt11126994/img",
                    episodeId = "tt11126994:1:2",
                    season = 1,
                    episode = 2,
                    episodeTitle = "Some Mysteries Are Better Left Unsolved",
                    positionMs = 1_450_000L,
                    durationMs = 2_460_000L,
                    updatedAtEpochMs = Clock.System.now().toEpochMilliseconds()
                )
            )
        )
    )
    val watchProgressByProfile: StateFlow<Map<String, List<WatchProgressEntry>>> = _watchProgressByProfile.asStateFlow()

    // Detail Streams & Subtitles
    private val _availableStreams = MutableStateFlow<List<MediaStream>>(emptyList())
    val availableStreams: StateFlow<List<MediaStream>> = _availableStreams.asStateFlow()

    private val _isLoadingStreams = MutableStateFlow(false)
    val isLoadingStreams: StateFlow<Boolean> = _isLoadingStreams.asStateFlow()

    private val _availableSubtitles = MutableStateFlow<List<SubtitleTrack>>(StremioAddonRepository.defaultDemoSubtitles())
    val availableSubtitles: StateFlow<List<SubtitleTrack>> = _availableSubtitles.asStateFlow()

    // Active Video Player Session
    private val _activePlaybackItem = MutableStateFlow<MediaItem?>(null)
    val activePlaybackItem: StateFlow<MediaItem?> = _activePlaybackItem.asStateFlow()

    private val _activePlaybackEpisode = MutableStateFlow<EpisodeVideo?>(null)
    val activePlaybackEpisode: StateFlow<EpisodeVideo?> = _activePlaybackEpisode.asStateFlow()

    private val _activeStream = MutableStateFlow<MediaStream?>(null)
    val activeStream: StateFlow<MediaStream?> = _activeStream.asStateFlow()

    private val _activePlayableUrl = MutableStateFlow<String?>(null)
    val activePlayableUrl: StateFlow<String?> = _activePlayableUrl.asStateFlow()

    private val _activeSkipSegments = MutableStateFlow<List<SkipSegment>>(emptyList())
    val activeSkipSegments: StateFlow<List<SkipSegment>> = _activeSkipSegments.asStateFlow()

    // Subtitle Appearance Config
    private val _subtitleConfig = MutableStateFlow(SubtitleAppearanceConfig())
    val subtitleConfig: StateFlow<SubtitleAppearanceConfig> = _subtitleConfig.asStateFlow()

    private val _selectedSubtitleTrack = MutableStateFlow<SubtitleTrack?>(StremioAddonRepository.defaultDemoSubtitles().first())
    val selectedSubtitleTrack: StateFlow<SubtitleTrack?> = _selectedSubtitleTrack.asStateFlow()

    // P2P, Debrid, Servers & Tracker Settings
    private val _p2pSettings = MutableStateFlow(P2PSettings())
    val p2pSettings: StateFlow<P2PSettings> = _p2pSettings.asStateFlow()

    val p2pTelemetry: StateFlow<P2PStreamTelemetry?> = p2pEngine.telemetry
    val p2pCacheClearedBytes: StateFlow<Long> = p2pEngine.cacheClearedCountBytes

    private val _debridConfig = MutableStateFlow(DebridConfig())
    val debridConfig: StateFlow<DebridConfig> = _debridConfig.asStateFlow()

    private val _mediaServers = MutableStateFlow<List<MediaServerConnection>>(emptyList())
    val mediaServers: StateFlow<List<MediaServerConnection>> = _mediaServers.asStateFlow()

    private val _trackerConfig = MutableStateFlow(TrackerSyncConfig())
    val trackerConfig: StateFlow<TrackerSyncConfig> = _trackerConfig.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    init {
        refreshLiveCatalogs()
    }

    fun refreshLiveCatalogs() {
        scope.launch {
            val cinemeta = _installedAddons.value.firstOrNull { it.id.contains("cinemeta") } ?: return@launch
            val movies = addonRepo.fetchCatalog(cinemeta, "movie", "top")
            val series = addonRepo.fetchCatalog(cinemeta, "series", "top")

            if (movies.isNotEmpty()) {
                _trendingMovies.value = StremioAddonRepository.defaultShowcaseItems().filter { it.type == "movie" } +
                    movies.filterNot { m -> _heroShowcaseItems.value.any { it.id == m.id } }.take(18)
            }
            if (series.isNotEmpty()) {
                _trendingSeries.value = StremioAddonRepository.defaultShowcaseItems().filter { it.type == "series" } +
                    series.filterNot { s -> _heroShowcaseItems.value.any { it.id == s.id } }.take(18)
            }
        }
    }

    fun selectTab(tab: NaphTab) {
        _currentTab.value = tab
        _selectedMediaItem.value = null
    }

    fun toggleProfilePicker(show: Boolean) {
        _showProfilePicker.value = show
    }

    fun switchProfile(profile: UserProfile) {
        _activeProfile.value = profile
        _showProfilePicker.value = false
        showBanner("Switched active profile to ${profile.name} ${profile.avatarEmoji}")
    }

    fun addProfile(name: String, emoji: String, isKids: Boolean) {
        if (name.isBlank()) return
        val colors = listOf(0xFF6366F1, 0xFF06B6D4, 0xFFE50914, 0xFF10B981, 0xFFF59E0B)
        val newProf = UserProfile(
            id = "prof_${Clock.System.now().toEpochMilliseconds()}",
            name = name.trim(),
            avatarColorHex = colors[_profiles.value.size % colors.size],
            avatarEmoji = emoji.ifBlank { "🍿" },
            isKidsProfile = isKids
        )
        _profiles.value = _profiles.value + newProf
        _activeProfile.value = newProf
        _showProfilePicker.value = false
    }

    fun openMediaDetail(item: MediaItem) {
        _selectedMediaItem.value = item
        _activeTrailerYtId.value = null
        loadStreamsAndMetadataFor(item, item.videos.firstOrNull())
    }

    fun closeMediaDetail() {
        _selectedMediaItem.value = null
        _activeTrailerYtId.value = null
    }

    fun openInAppTrailer(ytId: String?) {
        _activeTrailerYtId.value = ytId
    }

    fun loadStreamsAndMetadataFor(item: MediaItem, episode: EpisodeVideo? = null) {
        scope.launch {
            _isLoadingStreams.value = true
            val videoId = episode?.id ?: item.id
            val fullMeta = addonRepo.fetchMeta(_installedAddons.value, item.type, item.id) ?: item
            val ratings = metaRepo.fetchAggregatedRatings(fullMeta, _trackerConfig.value.mdbListApiKey)
            _selectedMediaItem.value = fullMeta.copy(aggregatedRatings = ratings)

            val addonStreams = addonRepo.fetchStreamsFromAllAddons(_installedAddons.value, item.type, videoId)
            val serverStreams = if (item.id.startsWith("jellyfin:") || item.id.startsWith("emby:")) {
                val parts = item.id.split(":")
                val serverId = parts.getOrNull(1)
                val server = _mediaServers.value.find { it.id == serverId }
                if (server != null) listOf(serverRepo.buildDirectPlayStream(server, item.id)) else emptyList()
            } else {
                emptyList()
            }

            _availableStreams.value = serverStreams + addonStreams
            _availableSubtitles.value = addonRepo.fetchSubtitlesFromAllAddons(_installedAddons.value, item.type, videoId)
            _isLoadingStreams.value = false
        }
    }

    fun startPlayback(item: MediaItem, stream: MediaStream, episode: EpisodeVideo? = null) {
        _activePlaybackItem.value = item
        _activePlaybackEpisode.value = episode
        _activeStream.value = stream
        _activeSkipSegments.value = metaRepo.getSkipSegmentsFor(item.id, episode?.season, episode?.episode)

        scope.launch {
            syncRepo.scrobblePlaybackEvent(
                action = "start",
                imdbId = item.id,
                mediaType = item.type,
                progressPercent = 1f,
                config = _trackerConfig.value
            )
        }

        // 1. Check if user prefers Debrid resolution for P2P infoHashes
        if (stream.isP2PTorrent && _debridConfig.value.preferDebridOverP2P && debridRepo.hasActiveDebrid(_debridConfig.value)) {
            scope.launch {
                val resolved = debridRepo.resolveInfoHashToHttpUrl(stream, _debridConfig.value)
                resolved.onSuccess { directHttpUrl ->
                    _activePlayableUrl.value = directHttpUrl
                    showBanner("Resolved cached 4K stream via ${debridRepo.activeProviderName(_debridConfig.value)}")
                }.onFailure {
                    // Fallback to native P2P engine immediately
                    p2pEngine.startSequentialStream(stream, _p2pSettings.value) { localUrl ->
                        _activePlayableUrl.value = localUrl
                    }
                }
            }
        } else if (stream.isP2PTorrent) {
            // Native Sequential P2P Torrent Engine (#1 Priority)
            p2pEngine.startSequentialStream(stream, _p2pSettings.value) { localUrl ->
                _activePlayableUrl.value = localUrl
            }
        } else {
            // Direct HTTP / Debrid Cached / Jellyfin / Emby stream
            _activePlayableUrl.value = stream.url
        }
    }

    fun stopPlaybackAndSaveProgress(positionMs: Long, durationMs: Long) {
        val item = _activePlaybackItem.value
        val ep = _activePlaybackEpisode.value
        val prof = _activeProfile.value

        if (item != null && durationMs > 0) {
            val entry = WatchProgressEntry(
                profileId = prof.id,
                mediaId = item.id,
                mediaType = item.type,
                title = item.name,
                posterUrl = item.poster,
                backdropUrl = item.background,
                episodeId = ep?.id,
                season = ep?.season,
                episode = ep?.episode,
                episodeTitle = ep?.displayTitle,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAtEpochMs = Clock.System.now().toEpochMilliseconds()
            )
            val currentList = _watchProgressByProfile.value[prof.id].orEmpty()
                .filterNot { it.mediaId == item.id }
            _watchProgressByProfile.value = _watchProgressByProfile.value + (prof.id to (listOf(entry) + currentList))

            scope.launch {
                syncRepo.syncProfileProgressToSupabase(entry, prof, _installedAddons.value)
                syncRepo.scrobblePlaybackEvent(
                    action = "stop",
                    imdbId = item.id,
                    mediaType = item.type,
                    progressPercent = entry.progressFraction * 100f,
                    config = _trackerConfig.value
                )
            }
        }

        p2pEngine.stopAndCleanSession(_p2pSettings.value)
        _activePlaybackItem.value = null
        _activePlaybackEpisode.value = null
        _activeStream.value = null
        _activePlayableUrl.value = null
    }

    fun playNextEpisodeInQueue() {
        val item = _activePlaybackItem.value ?: return
        val currentEp = _activePlaybackEpisode.value ?: return
        val idx = item.videos.indexOfFirst { it.id == currentEp.id }
        val nextEp = item.videos.getOrNull(idx + 1) ?: return
        val currentStream = _activeStream.value ?: _availableStreams.value.firstOrNull() ?: return
        startPlayback(item, currentStream, nextEp)
    }

    fun toggleWatchlist(item: MediaItem) {
        val profId = _activeProfile.value.id
        val current = _watchlistByProfile.value[profId].orEmpty()
        val updated = if (current.any { it.id == item.id }) {
            current.filterNot { it.id == item.id }
        } else {
            listOf(item) + current
        }
        _watchlistByProfile.value = _watchlistByProfile.value + (profId to updated)
    }

    fun isInWatchlist(itemId: String): Boolean {
        val profId = _activeProfile.value.id
        return _watchlistByProfile.value[profId].orEmpty().any { it.id == itemId }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        val allLocal = (_heroShowcaseItems.value + _trendingMovies.value + _trendingSeries.value + _personalServerItems.value)
            .distinctBy { it.id }

        if (query.isBlank()) {
            _searchResults.value = allLocal
            return
        }

        val filtered = allLocal.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.genres.any { g -> g.contains(query, ignoreCase = true) }
        }
        _searchResults.value = filtered

        scope.launch {
            val cinemeta = _installedAddons.value.firstOrNull { it.id.contains("cinemeta") } ?: return@launch
            val remoteMovies = addonRepo.fetchCatalog(cinemeta, "movie", "top", searchQuery = query)
            val remoteSeries = addonRepo.fetchCatalog(cinemeta, "series", "top", searchQuery = query)
            val combined = (filtered + remoteMovies + remoteSeries).distinctBy { it.id }
            if (_searchQuery.value == query) {
                _searchResults.value = combined
            }
        }
    }

    fun installAddonByManifestUrl(rawUrl: String) {
        if (rawUrl.isBlank()) return
        scope.launch {
            val result = addonRepo.fetchManifest(rawUrl)
            result.onSuccess { manifest ->
                _installedAddons.value = _installedAddons.value.filterNot { it.id == manifest.id } + manifest
                showBanner("Installed addon: ${manifest.name} (v${manifest.version})")
                refreshLiveCatalogs()
            }.onFailure {
                // Still allow adding custom manifest entry if offline or CORS restricted
                val normalized = addonRepo.normalizeManifestUrl(rawUrl)
                val fallback = AddonManifest(
                    id = "custom.${normalized.hashCode()}",
                    version = "1.0.0",
                    name = normalized.substringAfter("://").substringBefore("/"),
                    description = "Custom Stremio JSON Manifest Addon ($normalized)",
                    types = listOf("movie", "series"),
                    manifestUrl = normalized,
                    isEnabled = true
                )
                _installedAddons.value = _installedAddons.value + fallback
                showBanner("Added custom manifest: ${fallback.name}")
            }
        }
    }

    fun removeAddon(addonId: String) {
        _installedAddons.value = _installedAddons.value.filterNot { it.id == addonId }
    }

    fun updateP2PSettings(updated: P2PSettings) {
        _p2pSettings.value = updated
        showBanner("Updated P2P Sequential Engine settings")
    }

    fun updateDebridConfig(updated: DebridConfig) {
        _debridConfig.value = updated
        showBanner("Saved Debrid configuration")
    }

    fun updateTrackerConfig(updated: TrackerSyncConfig) {
        _trackerConfig.value = updated
        showBanner("Saved Trakt.tv / SIMKL / MDBList sync configuration")
    }

    fun updateSubtitleConfig(updated: SubtitleAppearanceConfig) {
        _subtitleConfig.value = updated
    }

    fun selectSubtitleTrack(track: SubtitleTrack?) {
        _selectedSubtitleTrack.value = track
    }

    fun connectMediaServer(
        type: ServerType,
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ) {
        scope.launch {
            val result = serverRepo.authenticateServer(type, name, serverUrl, username, password)
            result.onSuccess { conn ->
                _mediaServers.value = _mediaServers.value.filterNot { it.id == conn.id } + conn
                val items = serverRepo.fetchLibraryItems(conn)
                _personalServerItems.value = (_personalServerItems.value + items).distinctBy { it.id }
                showBanner("Connected to ${conn.name} (${items.size} items loaded)")
            }.onFailure { err ->
                showBanner("Could not reach ${type.name} server at $serverUrl: ${err.message}")
            }
        }
    }

    fun showBanner(msg: String) {
        _statusBannerMessage.value = msg
    }

    fun dismissBanner() {
        _statusBannerMessage.value = null
    }
}
