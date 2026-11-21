package com.naph.app.data.trackers

import com.naph.app.core.model.AddonManifest
import com.naph.app.core.model.TrackerSyncConfig
import com.naph.app.core.model.UserProfile
import com.naph.app.core.model.WatchProgressEntry
import com.naph.app.core.network.NaphNetwork
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

data class CloudAddonRecord(
    val id: String,
    val manifestUrl: String,
    val isEnabled: Boolean
)

/**
 * Built-in NAPH Cloud Sync (Powered transparently by NAPH-Cloud Supabase — NO raw API URL/Key prompts shown to users!)
 * - Real cross-device account creation & sign-in persisted in `public.naph_accounts`.
 * - Scopes `installed_addons`, `profiles`, and `watch_progress` by `account_email` so addons & progress added on Linux
 *   instantly sync down when signing in with the same email on iPhone, Windows, or Android!
 */
class CloudAndTrackerSyncRepository {
    private val client = NaphNetwork.httpClient

    private val builtInCloudUrl = "https://pdspcutbecilcgpskgad.supabase.co"
    private val builtInCloudKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBkc3BjdXRiZWNpbGNncHNrZ2FkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTUyMDcwOTcsImV4cCI6MjExMDc4MzA5N30.H_rnE0DFZIvqPrxbZ_8XZBwxiS3B_kz-zrYQ6Hgl-yg"

    private val _accountEmail = MutableStateFlow<String?>(null)
    val accountEmail: StateFlow<String?> = _accountEmail.asStateFlow()

    private val _accountUsername = MutableStateFlow<String?>(null)
    val accountUsername: StateFlow<String?> = _accountUsername.asStateFlow()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _cloudSyncStatus = MutableStateFlow("Local Mode • Create an account or sign in to sync across devices")
    val cloudSyncStatus: StateFlow<String> = _cloudSyncStatus.asStateFlow()

    private fun simpleHash(input: String): String {
        var h = 1125899906842597L
        for (c in input) {
            h = 31L * h + c.code.toLong()
        }
        return h.toULong().toString(16)
    }

    private fun encodeQuery(value: String): String = buildString {
        value.encodeToByteArray().forEach { b ->
            val v = b.toInt() and 0xFF
            val c = v.toChar()
            if (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '-' || c == '_' || c == '.' || c == '~') {
                append(c)
            } else {
                val hex = "0123456789ABCDEF"
                append('%')
                append(hex[v shr 4])
                append(hex[v and 0x0F])
            }
        }
    }

    suspend fun createNaphAccount(username: String, email: String, password: String): Result<String> {
        val cleanEmail = email.trim().lowercase()
        val cleanUser = username.trim().ifBlank { cleanEmail.substringBefore("@") }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        _isSyncing.value = true
        return runCatching {
            val response = client.post("$builtInCloudUrl/rest/v1/naph_accounts") {
                contentType(ContentType.Application.Json)
                header("apikey", builtInCloudKey)
                header(HttpHeaders.Authorization, "Bearer $builtInCloudKey")
                header("Prefer", "resolution=merge-duplicates")
                setBody(
                    buildJsonObject {
                        put("email", cleanEmail)
                        put("username", cleanUser)
                        put("password_hash", simpleHash("$cleanEmail:$password"))
                    }.toString()
                )
            }
            if (!response.status.isSuccess()) {
                error("Cloud server returned ${response.status.value}")
            }
            _accountUsername.value = cleanUser
            _accountEmail.value = cleanEmail
            _isSignedIn.value = true
            _cloudSyncStatus.value = "Account created for $cleanUser ($cleanEmail) • Cloud Sync Active"
            cleanUser
        }.also {
            _isSyncing.value = false
        }
    }

    suspend fun signInWithNaphAccount(email: String, password: String): Result<String> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your password"))
        }

        _isSyncing.value = true
        return runCatching {
            val encodedEmail = encodeQuery(cleanEmail)
            val text = client.get("$builtInCloudUrl/rest/v1/naph_accounts?email=eq.$encodedEmail&select=email,username,password_hash") {
                header("apikey", builtInCloudKey)
                header(HttpHeaders.Authorization, "Bearer $builtInCloudKey")
            }.bodyAsText()

            val arr = NaphNetwork.json.parseToJsonElement(text).jsonArray
            val expectedHash = simpleHash("$cleanEmail:$password")
            val userObj = arr.firstOrNull()?.jsonObject

            val resolvedUsername = if (userObj != null) {
                val storedHash = userObj["password_hash"]?.jsonPrimitive?.contentOrNull.orEmpty()
                if (storedHash.isNotBlank() && storedHash != expectedHash) {
                    error("Incorrect password for $cleanEmail")
                }
                userObj["username"]?.jsonPrimitive?.contentOrNull ?: cleanEmail.substringBefore("@")
            } else {
                // If user previously created an account locally before cloud accounts table existed, register seamlessly
                val fallbackUser = cleanEmail.substringBefore("@")
                client.post("$builtInCloudUrl/rest/v1/naph_accounts") {
                    contentType(ContentType.Application.Json)
                    header("apikey", builtInCloudKey)
                    header(HttpHeaders.Authorization, "Bearer $builtInCloudKey")
                    header("Prefer", "resolution=merge-duplicates")
                    setBody(
                        buildJsonObject {
                            put("email", cleanEmail)
                            put("username", fallbackUser)
                            put("password_hash", expectedHash)
                        }.toString()
                    )
                }
                fallbackUser
            }

            _accountUsername.value = resolvedUsername
            _accountEmail.value = cleanEmail
            _isSignedIn.value = true
            _cloudSyncStatus.value = "Signed in as $resolvedUsername ($cleanEmail) • Synced with NAPH Cloud"
            resolvedUsername
        }.also {
            _isSyncing.value = false
        }
    }

    fun signOutNaphAccount() {
        _accountEmail.value = null
        _accountUsername.value = null
        _isSignedIn.value = false
        _cloudSyncStatus.value = "Local Mode • Create an account or sign in to sync across devices"
    }

    suspend fun clearDefaultSeededAddonsOnCloud() {
        runCatching {
            client.delete("$builtInCloudUrl/rest/v1/installed_addons?account_email=eq.&id=in.(com.linvo.cinemeta,org.stremio.opensubtitlesv3,org.naph.p2p.indexer)") {
                header("apikey", builtInCloudKey)
                header(HttpHeaders.Authorization, "Bearer $builtInCloudKey")
            }
        }
    }

    suspend fun pushAddonToCloud(addon: AddonManifest) {
        val email = _accountEmail.value?.lowercase() ?: return
        if (!_isSignedIn.value) return
        runCatching {
            client.post("$builtInCloudUrl/rest/v1/installed_addons") {
                contentType(ContentType.Application.Json)
                header("apikey", builtInCloudKey)
                header(HttpHeaders.Authorization, "Bearer $builtInCloudKey")
                header("Prefer", "resolution=merge-duplicates")
                setBody(
                    buildJsonObject {
                        put("account_email", email)
                        put("id", addon.id)
                        put("name", addon.name)
                        put("version", addon.version)
                        put("description", addon.description)
                        put("manifest_url", addon.manifestUrl)
                        put("is_enabled", addon.isEnabled)
                    }.toString()
                )
            }
            _cloudSyncStatus.value = "Synced '${addon.name}' to NAPH Cloud ($email)"
        }
    }

    suspend fun removeAddonFromCloud(addonId: String) {
        val email = _accountEmail.value?.lowercase() ?: return
        if (!_isSignedIn.value) return
        runCatching {
            client.delete("$builtInCloudUrl/rest/v1/installed_addons?account_email=eq.${encodeQuery(email)}&id=eq.${encodeQuery(addonId)}") {
                header("apikey", builtInCloudKey)
                header(HttpHeaders.Authorization, "Bearer $builtInCloudKey")
            }
        }
    }

    suspend fun pullAddonsFromCloud(emailOverride: String? = null): List<CloudAddonRecord> {
        val email = (emailOverride ?: _accountEmail.value)?.trim()?.lowercase() ?: return emptyList()
        return runCatching {
            val text = client.get("$builtInCloudUrl/rest/v1/installed_addons?account_email=eq.${encodeQuery(email)}&select=id,manifest_url,is_enabled") {
                header("apikey", builtInCloudKey)
                header(HttpHeaders.Authorization, "Bearer $builtInCloudKey")
            }.bodyAsText()
            val arr = NaphNetwork.json.parseToJsonElement(text).jsonArray
            arr.mapNotNull { el ->
                val obj = el.jsonObject
                val url = obj["manifest_url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: url
                val enabled = obj["is_enabled"]?.jsonPrimitive?.booleanOrNull ?: true
                CloudAddonRecord(id = id, manifestUrl = url, isEnabled = enabled)
            }
        }.getOrDefault(emptyList())
    }

    suspend fun pushProfileToCloud(profile: UserProfile) {
        val email = _accountEmail.value?.lowercase() ?: return
        if (!_isSignedIn.value) return
        runCatching {
            client.post("$builtInCloudUrl/rest/v1/profiles") {
                contentType(ContentType.Application.Json)
                header("apikey", builtInCloudKey)
                header(HttpHeaders.Authorization, "Bearer $builtInCloudKey")
                header("Prefer", "resolution=merge-duplicates")
                setBody(
                    buildJsonObject {
                        put("account_email", email)
                        put("id", profile.id)
                        put("name", profile.name)
                        put("avatar_color_hex", profile.avatarColorHex)
                        put("avatar_emoji", profile.avatarEmoji)
                        put("is_kids_profile", profile.isKidsProfile)
                        put("auto_skip_intros", profile.autoSkipIntros)
                        put("auto_play_next_episode", profile.autoPlayNextEpisode)
                    }.toString()
                )
            }
        }
    }

    suspend fun deleteProfileFromCloud(profileId: String) {
        val email = _accountEmail.value?.lowercase() ?: return
        if (!_isSignedIn.value) return
        runCatching {
            client.delete("$builtInCloudUrl/rest/v1/profiles?account_email=eq.${encodeQuery(email)}&id=eq.${encodeQuery(profileId)}") {
                header("apikey", builtInCloudKey)
                header(HttpHeaders.Authorization, "Bearer $builtInCloudKey")
            }
        }
    }

    suspend fun pullProfilesFromCloud(emailOverride: String? = null): List<UserProfile> {
        val email = (emailOverride ?: _accountEmail.value)?.trim()?.lowercase() ?: return emptyList()
        return runCatching {
            val text = client.get("$builtInCloudUrl/rest/v1/profiles?account_email=eq.${encodeQuery(email)}&select=id,name,avatar_color_hex,avatar_emoji,is_kids_profile,auto_skip_intros,auto_play_next_episode") {
                header("apikey", builtInCloudKey)
                header(HttpHeaders.Authorization, "Bearer $builtInCloudKey")
            }.bodyAsText()
            val arr = NaphNetwork.json.parseToJsonElement(text).jsonArray
            arr.mapNotNull { el ->
                val obj = el.jsonObject
                val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Profile"
                UserProfile(
                    id = id,
                    name = name,
                    avatarColorHex = obj["avatar_color_hex"]?.jsonPrimitive?.longOrNull ?: 0xFF3B82F6,
                    avatarEmoji = obj["avatar_emoji"]?.jsonPrimitive?.contentOrNull ?: "🎬",
                    isKidsProfile = obj["is_kids_profile"]?.jsonPrimitive?.booleanOrNull ?: false,
                    autoSkipIntros = obj["auto_skip_intros"]?.jsonPrimitive?.booleanOrNull ?: true,
                    autoPlayNextEpisode = obj["auto_play_next_episode"]?.jsonPrimitive?.booleanOrNull ?: true
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun pullWatchProgressFromCloud(emailOverride: String? = null): List<WatchProgressEntry> {
        val email = (emailOverride ?: _accountEmail.value)?.trim()?.lowercase() ?: return emptyList()
        return runCatching {
            val text = client.get("$builtInCloudUrl/rest/v1/watch_progress?account_email=eq.${encodeQuery(email)}&order=updated_at_epoch_ms.desc&limit=40") {
                header("apikey", builtInCloudKey)
                header(HttpHeaders.Authorization, "Bearer $builtInCloudKey")
            }.bodyAsText()
            val arr = NaphNetwork.json.parseToJsonElement(text).jsonArray
            arr.mapNotNull { el ->
                val obj = el.jsonObject
                val profId = obj["profile_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val mediaId = obj["media_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                WatchProgressEntry(
                    profileId = profId,
                    mediaId = mediaId,
                    mediaType = obj["media_type"]?.jsonPrimitive?.contentOrNull ?: "movie",
                    title = title,
                    posterUrl = obj["poster_url"]?.jsonPrimitive?.contentOrNull,
                    backdropUrl = obj["backdrop_url"]?.jsonPrimitive?.contentOrNull,
                    episodeId = obj["episode_id"]?.jsonPrimitive?.contentOrNull,
                    season = obj["season"]?.jsonPrimitive?.intOrNull,
                    episode = obj["episode"]?.jsonPrimitive?.intOrNull,
                    episodeTitle = obj["episode_title"]?.jsonPrimitive?.contentOrNull,
                    positionMs = obj["position_ms"]?.jsonPrimitive?.longOrNull ?: 0L,
                    durationMs = obj["duration_ms"]?.jsonPrimitive?.longOrNull ?: 0L,
                    updatedAtEpochMs = obj["updated_at_epoch_ms"]?.jsonPrimitive?.longOrNull ?: 0L
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun syncProfileProgressToSupabase(
        entry: WatchProgressEntry,
        activeProfile: UserProfile,
        installedAddons: List<AddonManifest>
    ) {
        val email = _accountEmail.value?.lowercase() ?: return
        if (!_isSignedIn.value) return
        runCatching {
            client.post("$builtInCloudUrl/rest/v1/watch_progress") {
                contentType(ContentType.Application.Json)
                header("apikey", builtInCloudKey)
                header(HttpHeaders.Authorization, "Bearer $builtInCloudKey")
                header("Prefer", "resolution=merge-duplicates")
                setBody(
                    buildJsonObject {
                        put("account_email", email)
                        put("profile_id", entry.profileId)
                        put("media_id", entry.mediaId)
                        put("media_type", entry.mediaType)
                        put("title", entry.title)
                        put("poster_url", entry.posterUrl)
                        put("backdrop_url", entry.backdropUrl)
                        put("episode_id", entry.episodeId)
                        put("season", entry.season)
                        put("episode", entry.episode)
                        put("episode_title", entry.episodeTitle)
                        put("position_ms", entry.positionMs)
                        put("duration_ms", entry.durationMs)
                        put("updated_at_epoch_ms", entry.updatedAtEpochMs)
                    }.toString()
                )
            }
            _cloudSyncStatus.value = "Synced '${entry.title}' (${(entry.progressFraction * 100).toInt()}%) to NAPH Cloud"
        }
    }

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
