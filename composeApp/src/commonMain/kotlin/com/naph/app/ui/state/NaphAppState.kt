package com.naph.app.ui.state

import com.naph.app.core.model.AddonManifest
import com.naph.app.core.model.DebridConfig
import com.naph.app.core.model.EpisodeVideo
import com.naph.app.core.model.HomeCatalogSection
import com.naph.app.core.model.LayoutCustomizationConfig
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
import com.naph.app.core.model.WatchPartySession
import com.naph.app.core.model.WatchProgressEntry
import com.naph.app.data.addons.StremioAddonRepository
import com.naph.app.data.debrid.DebridResolverRepository
import com.naph.app.data.metadata.MetadataAndSkipRepository
import com.naph.app.data.servers.PersonalMediaServerRepository
import com.naph.app.data.torrent.P2PTorrentStreamEngine
import com.naph.app.data.trackers.CloudAndTrackerSyncRepository
import com.naph.app.ui.theme.NaphThemePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * 4 Main Left Bar / Bottom Bar Navigation Tabs (Just like Nuvio & User Spec):
 * 1. Home
 * 2. Search
 * 3. Library
 * 4. Profile (Opens all structured Settings sections: Account, General, About)
 */
enum class NaphTab(val label: String) {
    HOME("Home"),
    SEARCH("Search"),
    LIBRARY("Library"),
    PROFILE("Profile")
}

/**
 * Structured Drill-Down Settings Pages inside Profile Tab
 * (Never dumps everything on one screen — clean drill-down sections like Nuvio)
 */
enum class ProfileSettingsPage(val title: String, val parent: ProfileSettingsPage? = null) {
    ROOT("Profile & Settings", null),

    // Section 1: ACCOUNT
    ACCOUNT_DETAIL("NAPH Cloud Account", ROOT),
    WATCH_PARTY("Watch Party", ROOT),
    TRACKING("Tracking (Trakt.tv & SIMKL)", ROOT),

    // Section 2: GENERAL
    APPEARANCE("Appearance & Theme", ROOT),
    LAYOUT_CUSTOMIZATION("Layout Customization", ROOT),
    CONTENT_DISCOVERY("Content & Discovery", ROOT),
    ADDONS_MANAGER("Installed Addons", CONTENT_DISCOVERY),
    MEDIA_SERVERS("Media Servers (Jellyfin & Emby)", CONTENT_DISCOVERY),
    PLAYBACK("Playback & P2P Engine", ROOT),
    INTEGRATIONS("Integrations (Debrid, TMDB, MDBList)", ROOT),
    NOTIFICATIONS("Notifications", ROOT),

    // Section 3: ABOUT
    ABOUT("About NAPH", ROOT)
}

class NaphAppState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val addonRepo = StremioAddonRepository()
    val p2pEngine = P2PTorrentStreamEngine()
    val debridRepo = DebridResolverRepository()
    val serverRepo = PersonalMediaServerRepository()
    val metaRepo = MetadataAndSkipRepository()
    val syncRepo = CloudAndTrackerSyncRepository()

    // Theme & Appearance
    private val _selectedThemePreset = MutableStateFlow(NaphThemePreset.ARCTIC_BLUE)
    val selectedThemePreset: StateFlow<NaphThemePreset> = _selectedThemePreset.asStateFlow()

    private val _amoledBlackEnabled = MutableStateFlow(false)
    val amoledBlackEnabled: StateFlow<Boolean> = _amoledBlackEnabled.asStateFlow()

    private val _layoutConfig = MutableStateFlow(LayoutCustomizationConfig())
    val layoutConfig: StateFlow<LayoutCustomizationConfig> = _layoutConfig.asStateFlow()

    // Navigation & Modal States
    private val _showProfilePicker = MutableStateFlow(false)
    val showProfilePicker: StateFlow<Boolean> = _showProfilePicker.asStateFlow()

    private val _currentTab = MutableStateFlow(NaphTab.HOME)
    val currentTab: StateFlow<NaphTab> = _currentTab.asStateFlow()

    private val _currentSettingsPage = MutableStateFlow(ProfileSettingsPage.ROOT)
    val currentSettingsPage: StateFlow<ProfileSettingsPage> = _currentSettingsPage.asStateFlow()

    private val _selectedMediaItem = MutableStateFlow<MediaItem?>(null)
    val selectedMediaItem: StateFlow<MediaItem?> = _selectedMediaItem.asStateFlow()

    private val _activeTrailerYtId = MutableStateFlow<String?>(null)
    val activeTrailerYtId: StateFlow<String?> = _activeTrailerYtId.asStateFlow()

    // Profiles (Starts with 1 clean profile; users can create, edit, or delete any profile!)
    private val _profiles = MutableStateFlow(
        listOf(
            UserProfile(id = "prof_main", name = "My Profile", avatarColorHex = 0xFF3B82F6, avatarEmoji = "🎬")
        )
    )
    val profiles: StateFlow<List<UserProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow(_profiles.value.first())
    val activeProfile: StateFlow<UserProfile> = _activeProfile.asStateFlow()

    // Installed Addons — Starts EMPTY by default! Users install whatever addons they want.
    private val _installedAddons = MutableStateFlow<List<AddonManifest>>(emptyList())
    val installedAddons: StateFlow<List<AddonManifest>> = _installedAddons.asStateFlow()

    private val _isInstallingAddon = MutableStateFlow(false)
    val isInstallingAddon: StateFlow<Boolean> = _isInstallingAddon.asStateFlow()

    // Dynamic Home Catalogs populated strictly from User-Installed Addons & Personal Servers
    private val _homeCatalogSections = MutableStateFlow<List<HomeCatalogSection>>(emptyList())
    val homeCatalogSections: StateFlow<List<HomeCatalogSection>> = _homeCatalogSections.asStateFlow()

    private val _heroShowcaseItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val heroShowcaseItems: StateFlow<List<MediaItem>> = _heroShowcaseItems.asStateFlow()

    private val _isLoadingHomeCatalogs = MutableStateFlow(false)
    val isLoadingHomeCatalogs: StateFlow<Boolean> = _isLoadingHomeCatalogs.asStateFlow()

    private val _personalServerItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val personalServerItems: StateFlow<List<MediaItem>> = _personalServerItems.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Watchlist & Continue Watching (Per Profile)
    private val _watchlistByProfile = MutableStateFlow<Map<String, List<MediaItem>>>(emptyMap())
    val watchlistByProfile: StateFlow<Map<String, List<MediaItem>>> = _watchlistByProfile.asStateFlow()

    private val _watchProgressByProfile = MutableStateFlow<Map<String, List<WatchProgressEntry>>>(emptyMap())
    val watchProgressByProfile: StateFlow<Map<String, List<WatchProgressEntry>>> = _watchProgressByProfile.asStateFlow()

    // Watch Party
    private val _watchPartySession = MutableStateFlow<WatchPartySession?>(null)
    val watchPartySession: StateFlow<WatchPartySession?> = _watchPartySession.asStateFlow()

    // Notifications
    private val _newEpisodeNotifications = MutableStateFlow(true)
    val newEpisodeNotifications: StateFlow<Boolean> = _newEpisodeNotifications.asStateFlow()

    private val _p2pCompletionNotifications = MutableStateFlow(true)
    val p2pCompletionNotifications: StateFlow<Boolean> = _p2pCompletionNotifications.asStateFlow()

    // Detail Streams & Subtitles
    private val _availableStreams = MutableStateFlow<List<MediaStream>>(emptyList())
    val availableStreams: StateFlow<List<MediaStream>> = _availableStreams.asStateFlow()

    private val _isLoadingStreams = MutableStateFlow(false)
    val isLoadingStreams: StateFlow<Boolean> = _isLoadingStreams.asStateFlow()

    private val _availableSubtitles = MutableStateFlow<List<SubtitleTrack>>(emptyList())
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

    private val _selectedSubtitleTrack = MutableStateFlow<SubtitleTrack?>(null)
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
        // Ensure any old default-seeded addons in cloud are cleared so the user starts clean
        scope.launch {
            syncRepo.clearDefaultSeededAddonsOnCloud()
        }
    }

    fun selectTab(tab: NaphTab) {
        _currentTab.value = tab
        _selectedMediaItem.value = null
        if (tab == NaphTab.PROFILE) {
            _currentSettingsPage.value = ProfileSettingsPage.ROOT
        }
    }

    fun navigateSettingsPage(page: ProfileSettingsPage) {
        _currentTab.value = NaphTab.PROFILE
        _selectedMediaItem.value = null
        _currentSettingsPage.value = page
    }

    fun navigateSettingsBack() {
        val parent = _currentSettingsPage.value.parent ?: ProfileSettingsPage.ROOT
        _currentSettingsPage.value = parent
    }

    fun setThemePreset(preset: NaphThemePreset) {
        _selectedThemePreset.value = preset
    }

    fun setAmoledBlack(enabled: Boolean) {
        _amoledBlackEnabled.value = enabled
    }

    fun updateLayoutConfig(config: LayoutCustomizationConfig) {
        val headerChanged = _layoutConfig.value.showCatalogTypeInHeader != config.showCatalogTypeInHeader
        _layoutConfig.value = config
        if (headerChanged) {
            refreshLiveCatalogs()
        }
    }

    fun setNotifications(newEps: Boolean, p2pDone: Boolean) {
        _newEpisodeNotifications.value = newEps
        _p2pCompletionNotifications.value = p2pDone
    }

    fun createWatchParty() {
        val code = "NAPH-" + (1000..9999).random()
        _watchPartySession.value = WatchPartySession(
            roomCode = code,
            hostName = _activeProfile.value.name,
            activeMediaTitle = _activePlaybackItem.value?.name ?: "Synced Lobby Ready",
            participantCount = 1,
            isConnected = true
        )
        showBanner("Created Watch Party Room: $code")
    }

    fun joinWatchParty(code: String) {
        if (code.isBlank()) return
        _watchPartySession.value = WatchPartySession(
            roomCode = code.trim().uppercase(),
            hostName = "Connected Friend",
            activeMediaTitle = "Synced Playback Active",
            participantCount = 2,
            isConnected = true
        )
        showBanner("Joined Watch Party Room: ${code.trim().uppercase()}")
    }

    fun leaveWatchParty() {
        _watchPartySession.value = null
        showBanner("Left Watch Party")
    }

    /**
     * Dynamically fetches all catalog rows from every enabled user-installed addon
     * and updates the Home screen sections and Hero showcase items!
     */
    fun refreshLiveCatalogs() {
        scope.launch {
            val enabledAddons = _installedAddons.value.filter { it.isEnabled }
            if (enabledAddons.isEmpty()) {
                _homeCatalogSections.value = emptyList()
                _heroShowcaseItems.value = _personalServerItems.value.take(6)
                _isLoadingHomeCatalogs.value = false
                return@launch
            }

            _isLoadingHomeCatalogs.value = true
            val sections = addonRepo.fetchAllHomeCatalogSections(
                addons = enabledAddons,
                showCatalogTypeInTitle = _layoutConfig.value.showCatalogTypeInHeader
            )
            _homeCatalogSections.value = sections

            val allCatalogItems = sections.flatMap { it.items }.distinctBy { "${it.type}:${it.id}" }
            _heroShowcaseItems.value = allCatalogItems.take(8)
            if (_searchQuery.value.isBlank()) {
                _searchResults.value = allCatalogItems
            }
            _isLoadingHomeCatalogs.value = false
        }
    }

    fun installAddonByManifestUrl(rawUrl: String) {
        if (rawUrl.isBlank()) return
        scope.launch {
            _isInstallingAddon.value = true
            val result = addonRepo.fetchManifest(rawUrl)
            result.onSuccess { manifest ->
                _installedAddons.value = _installedAddons.value.filterNot {
                    it.id == manifest.id || it.manifestUrl == manifest.manifestUrl
                } + manifest
                _isInstallingAddon.value = false
                showBanner("Installed '${manifest.name}' (${manifest.catalogs.size} catalogs)")
                syncRepo.pushAddonToCloud(manifest)
                refreshLiveCatalogs()
            }.onFailure { err ->
                _isInstallingAddon.value = false
                showBanner("Failed to load addon manifest: ${err.message}")
            }
        }
    }

    fun createAccountAndSync(username: String, email: String, password: String) {
        scope.launch {
            val res = syncRepo.createNaphAccount(username, email, password)
            res.onSuccess { user ->
                // Push any existing local addons & profiles to this new cloud account
                _installedAddons.value.forEach { syncRepo.pushAddonToCloud(it) }
                _profiles.value.forEach { syncRepo.pushProfileToCloud(it) }
                showBanner("Created account '$user' & synced ${_installedAddons.value.size} addons to cloud")
            }.onFailure { err ->
                showBanner(err.message ?: "Could not create account")
            }
        }
    }

    fun signInAndSync(email: String, password: String) {
        scope.launch {
            val res = syncRepo.signInWithNaphAccount(email, password)
            res.onSuccess { user ->
                showBanner("Signed in as $user • Syncing addons & profiles...")
                syncCloudNow()
            }.onFailure { err ->
                showBanner(err.message ?: "Sign in failed")
            }
        }
    }

    fun syncCloudNow() {
        scope.launch {
            val email = syncRepo.accountEmail.value ?: return@launch
            // 1. Pull addons from cloud for this account_email
            val cloudAddons = syncRepo.pullAddonsFromCloud(email)
            if (cloudAddons.isNotEmpty()) {
                val fetched = coroutineScope {
                    cloudAddons.map { rec ->
                        async {
                            addonRepo.fetchManifest(rec.manifestUrl).getOrNull()?.copy(isEnabled = rec.isEnabled)
                        }
                    }.awaitAll().filterNotNull()
                }
                if (fetched.isNotEmpty()) {
                    val merged = (_installedAddons.value + fetched).distinctBy { it.manifestUrl }
                    _installedAddons.value = merged
                    refreshLiveCatalogs()
                }
            }

            // 2. Push any local addons that aren't in the cloud yet
            val cloudUrls = cloudAddons.map { it.manifestUrl }.toSet()
            _installedAddons.value.filterNot { it.manifestUrl in cloudUrls }.forEach {
                syncRepo.pushAddonToCloud(it)
            }

            // 3. Pull profiles from cloud
            val cloudProfiles = syncRepo.pullProfilesFromCloud(email)
            if (cloudProfiles.isNotEmpty()) {
                _profiles.value = cloudProfiles
                if (cloudProfiles.none { it.id == _activeProfile.value.id }) {
                    _activeProfile.value = cloudProfiles.first()
                }
            } else {
                _profiles.value.forEach { syncRepo.pushProfileToCloud(it) }
            }

            // 4. Pull watch progress from cloud
            val cloudProgress = syncRepo.pullWatchProgressFromCloud(email)
            if (cloudProgress.isNotEmpty()) {
                _watchProgressByProfile.value = cloudProgress.groupBy { it.profileId }
            }

            showBanner("Cloud Sync Complete • ${_installedAddons.value.size} addons synced")
        }
    }

    fun toggleAddonEnabled(addonId: String, enabled: Boolean) {
        val updatedList = _installedAddons.value.map {
            if (it.id == addonId) it.copy(isEnabled = enabled) else it
        }
        _installedAddons.value = updatedList
        val target = updatedList.find { it.id == addonId }
        if (target != null) {
            scope.launch { syncRepo.pushAddonToCloud(target) }
        }
        refreshLiveCatalogs()
    }

    fun removeAddon(addonId: String) {
        _installedAddons.value = _installedAddons.value.filterNot { it.id == addonId }
        scope.launch { syncRepo.removeAddonFromCloud(addonId) }
        refreshLiveCatalogs()
        showBanner("Removed addon")
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
        val colors = listOf(0xFF3B82F6, 0xFF10B981, 0xFFE50914, 0xFF8B5CF6, 0xFFF59E0B)
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
        scope.launch { syncRepo.pushProfileToCloud(newProf) }
        showBanner("Created profile '${newProf.name}'")
    }

    fun deleteProfile(profileId: String) {
        val target = _profiles.value.find { it.id == profileId } ?: return
        val remaining = _profiles.value.filterNot { it.id == profileId }
        _watchlistByProfile.value = _watchlistByProfile.value - profileId
        _watchProgressByProfile.value = _watchProgressByProfile.value - profileId
        scope.launch { syncRepo.deleteProfileFromCloud(profileId) }

        if (remaining.isEmpty()) {
            val freshDefault = UserProfile(
                id = "prof_${Clock.System.now().toEpochMilliseconds()}",
                name = "My Profile",
                avatarColorHex = 0xFF3B82F6,
                avatarEmoji = "🎬"
            )
            _profiles.value = listOf(freshDefault)
            _activeProfile.value = freshDefault
            showBanner("Deleted '${target.name}' and reset to fresh profile")
        } else {
            _profiles.value = remaining
            if (_activeProfile.value.id == profileId) {
                _activeProfile.value = remaining.first()
            }
            showBanner("Deleted profile '${target.name}'")
        }
    }

    fun renameProfile(profileId: String, newName: String, newEmoji: String) {
        if (newName.isBlank()) return
        val updatedList = _profiles.value.map { prof ->
            if (prof.id == profileId) {
                prof.copy(name = newName.trim(), avatarEmoji = newEmoji.ifBlank { prof.avatarEmoji })
            } else {
                prof
            }
        }
        _profiles.value = updatedList
        if (_activeProfile.value.id == profileId) {
            _activeProfile.value = updatedList.first { it.id == profileId }
        }
        showBanner("Updated profile '${newName.trim()}'")
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
            val fullMeta = addonRepo.fetchMeta(_installedAddons.value, item)
            val ratings = metaRepo.fetchAggregatedRatings(fullMeta, _trackerConfig.value.mdbListApiKey)
            val enrichedItem = fullMeta.copy(aggregatedRatings = ratings)
            _selectedMediaItem.value = enrichedItem

            val targetEp = episode ?: enrichedItem.videos.firstOrNull()
            val videoId = targetEp?.id ?: enrichedItem.id

            val addonStreams = addonRepo.fetchStreamsFromAllAddons(_installedAddons.value, enrichedItem.type, videoId)
            val serverStreams = if (enrichedItem.id.startsWith("jellyfin:") || enrichedItem.id.startsWith("emby:")) {
                val parts = enrichedItem.id.split(":")
                val serverId = parts.getOrNull(1)
                val server = _mediaServers.value.find { it.id == serverId }
                if (server != null) listOf(serverRepo.buildDirectPlayStream(server, enrichedItem.id)) else emptyList()
            } else {
                emptyList()
            }

            _availableStreams.value = serverStreams + addonStreams
            val subs = addonRepo.fetchSubtitlesFromAllAddons(_installedAddons.value, enrichedItem.type, videoId)
            _availableSubtitles.value = subs
            _selectedSubtitleTrack.value = subs.firstOrNull()
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

        if (stream.isP2PTorrent && _debridConfig.value.preferDebridOverP2P && debridRepo.hasActiveDebrid(_debridConfig.value)) {
            scope.launch {
                val resolved = debridRepo.resolveInfoHashToHttpUrl(stream, _debridConfig.value)
                resolved.onSuccess { directHttpUrl ->
                    _activePlayableUrl.value = directHttpUrl
                    showBanner("Resolved cached stream via ${debridRepo.activeProviderName(_debridConfig.value)}")
                }.onFailure {
                    p2pEngine.startSequentialStream(
                        stream = stream,
                        settings = _p2pSettings.value,
                        onStreamReady = { localUrl -> _activePlayableUrl.value = localUrl },
                        onError = { err -> showBanner(err) }
                    )
                }
            }
        } else if (stream.isP2PTorrent) {
            p2pEngine.startSequentialStream(
                stream = stream,
                settings = _p2pSettings.value,
                onStreamReady = { localUrl -> _activePlayableUrl.value = localUrl },
                onError = { err -> showBanner(err) }
            )
        } else {
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
        val allLoaded = (_homeCatalogSections.value.flatMap { it.items } + _personalServerItems.value)
            .distinctBy { "${it.type}:${it.id}" }

        if (query.isBlank()) {
            _searchResults.value = allLoaded
            _isSearching.value = false
            return
        }

        val localMatches = allLoaded.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.genres.any { g -> g.contains(query, ignoreCase = true) }
        }
        _searchResults.value = localMatches

        if (_installedAddons.value.isNotEmpty()) {
            scope.launch {
                _isSearching.value = true
                val remoteMatches = addonRepo.searchAcrossInstalledAddons(_installedAddons.value, query)
                if (_searchQuery.value == query) {
                    _searchResults.value = (localMatches + remoteMatches).distinctBy { "${it.type}:${it.id}" }
                    _isSearching.value = false
                }
            }
        }
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
        showBanner("Saved Tracking & Integration settings")
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
