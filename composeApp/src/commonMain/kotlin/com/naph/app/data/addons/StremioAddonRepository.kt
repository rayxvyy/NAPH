package com.naph.app.data.addons

import com.naph.app.core.model.AddonCatalogDef
import com.naph.app.core.model.AddonExtraProp
import com.naph.app.core.model.AddonManifest
import com.naph.app.core.model.EpisodeVideo
import com.naph.app.core.model.HomeCatalogSection
import com.naph.app.core.model.MediaItem
import com.naph.app.core.model.MediaStream
import com.naph.app.core.model.StreamBehaviorHints
import com.naph.app.core.model.SubtitleTrack
import com.naph.app.core.model.TrailerSource
import com.naph.app.core.network.NaphNetwork
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Stremio v3 Compatible JSON Manifest Addon Engine (Modeled after Nuvio's AddonManifestParser & HomeCatalogParser)
 *
 * - NO default addons are pre-installed (users install only the addons they want).
 * - Handles mixed `extra`, `resources`, `imdbRating` (number or string), `releaseInfo`/`year`,
 *   and `banner`/`background`/`landscapePoster` fields so any community addon works reliably
 *   and populates its catalog sections on the Home Screen.
 */
class StremioAddonRepository {
    private val client = NaphNetwork.httpClient
    private val json = NaphNetwork.json

    fun normalizeManifestUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        require(trimmed.isNotEmpty()) { "Please enter an addon manifest URL" }

        val normalizedScheme = when {
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("stremio://", ignoreCase = true) ->
                "https://" + trimmed.substringAfter("://")
            else -> "https://$trimmed"
        }

        val withoutFragment = normalizedScheme.substringBefore("#")
        val query = withoutFragment.substringAfter("?", "")
        val path = withoutFragment.substringBefore("?").trimEnd('/')
        val manifestPath = if (path.endsWith("/manifest.json")) path else "$path/manifest.json"

        return if (query.isEmpty()) manifestPath else "$manifestPath?$query"
    }

    private fun buildResourceUrl(
        manifestUrl: String,
        resource: String,
        type: String,
        id: String,
        extraPath: String? = null
    ): String {
        val base = manifestUrl.substringBefore("?").removeSuffix("/manifest.json").trimEnd('/')
        val query = manifestUrl.substringAfter("?", "").let { if (it.isBlank()) "" else "?$it" }
        val encodedId = encodePathSegment(id)
        val path = if (extraPath.isNullOrBlank()) {
            "$base/$resource/$type/$encodedId.json"
        } else {
            "$base/$resource/$type/$encodedId/$extraPath.json"
        }
        return path + query
    }

    suspend fun fetchManifest(rawUrl: String): Result<AddonManifest> = runCatching {
        val normalized = normalizeManifestUrl(rawUrl)
        val payload = client.get(normalized).bodyAsText()
        parseManifestJson(normalized, payload)
    }

    fun parseManifestJson(manifestUrl: String, payload: String): AddonManifest {
        val root = json.parseToJsonElement(payload).jsonObject
        val id = root.optString("id") ?: error("Manifest missing 'id'")
        val name = root.optString("name") ?: id
        val version = root.optString("version") ?: "1.0.0"
        val description = root.optString("description").orEmpty()
        val types = root.optStringList("types")
        val idPrefixes = root.optStringList("idPrefixes")

        val catalogs = root.optArray("catalogs").mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val cType = obj.optString("type") ?: return@mapNotNull null
            val cId = obj.optString("id") ?: return@mapNotNull null
            val cName = obj.optString("name")?.takeIf { it.isNotBlank() } ?: cId

            // Parse both `extra` (object list) and `extraRequired` / `extraSupported` (legacy Stremio v2/v3)
            val extraRequiredSet = obj.optStringList("extraRequired").toSet()
            val extraProps = obj.optArray("extra").mapNotNull { extraEl ->
                when (extraEl) {
                    is JsonObject -> {
                        val eName = extraEl.optString("name") ?: return@mapNotNull null
                        AddonExtraProp(
                            name = eName,
                            isRequired = extraEl.optBoolean("isRequired") || eName in extraRequiredSet,
                            options = extraEl.optStringList("options")
                        )
                    }
                    is JsonPrimitive -> {
                        val eName = extraEl.contentOrNull ?: return@mapNotNull null
                        AddonExtraProp(name = eName, isRequired = eName in extraRequiredSet)
                    }
                    else -> null
                }
            }.ifEmpty {
                extraRequiredSet.map { AddonExtraProp(name = it, isRequired = true) }
            }

            AddonCatalogDef(
                type = cType,
                id = cId,
                name = cName,
                extra = extraProps
            )
        }

        return AddonManifest(
            id = id,
            version = version,
            name = name,
            description = description,
            logo = root.optString("logo"),
            background = root.optString("background"),
            types = types,
            catalogs = catalogs,
            idPrefixes = idPrefixes.ifEmpty { null },
            manifestUrl = manifestUrl,
            isEnabled = true
        )
    }

    /**
     * Fetches all non-search-only catalogs across all enabled user-installed addons
     * and returns populated HomeCatalogSections for the Home screen.
     */
    suspend fun fetchAllHomeCatalogSections(
        addons: List<AddonManifest>,
        showCatalogTypeInTitle: Boolean = true
    ): List<HomeCatalogSection> = coroutineScope {
        val enabledAddons = addons.filter { it.isEnabled }
        val definitions = enabledAddons.flatMap { addon ->
            addon.catalogs
                .filter { catalog -> !catalog.requiresExtraInput }
                .map { catalog -> addon to catalog }
        }

        definitions.map { (addon, catalog) ->
            async {
                val items = fetchCatalog(
                    addon = addon,
                    type = catalog.type,
                    catalogId = catalog.id
                )
                if (items.isNotEmpty()) {
                    val typeLabel = catalog.type.replaceFirstChar { it.uppercase() }
                    val displayTitle = if (showCatalogTypeInTitle) {
                        "${catalog.name} — $typeLabel"
                    } else {
                        catalog.name
                    }
                    HomeCatalogSection(
                        key = "${addon.id}:${catalog.type}:${catalog.id}",
                        title = displayTitle,
                        addonName = addon.name,
                        type = catalog.type,
                        catalogId = catalog.id,
                        manifestUrl = addon.manifestUrl,
                        items = items
                    )
                } else {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    suspend fun fetchCatalog(
        addon: AddonManifest,
        type: String,
        catalogId: String,
        searchQuery: String? = null,
        genre: String? = null
    ): List<MediaItem> = runCatching {
        val extraParts = buildList {
            if (!searchQuery.isNullOrBlank()) add("search=${encodePathSegment(searchQuery)}")
            if (!genre.isNullOrBlank()) add("genre=${encodePathSegment(genre)}")
        }
        val url = buildResourceUrl(
            manifestUrl = addon.manifestUrl,
            resource = "catalog",
            type = type,
            id = catalogId,
            extraPath = extraParts.joinToString("&").ifBlank { null }
        )
        val payload = client.get(url).bodyAsText()
        val root = json.parseToJsonElement(payload).jsonObject
        root.optArray("metas").mapNotNull { el ->
            (el as? JsonObject)?.toMediaItem(defaultType = type, addonName = addon.name)
        }
    }.getOrDefault(emptyList())

    suspend fun searchAcrossInstalledAddons(
        addons: List<AddonManifest>,
        query: String
    ): List<MediaItem> = coroutineScope {
        val enabledAddons = addons.filter { it.isEnabled }
        val searchableCatalogs = enabledAddons.flatMap { addon ->
            val explicitSearch = addon.catalogs.filter { cat ->
                cat.extra.any { it.name.equals("search", ignoreCase = true) }
            }
            val targets = explicitSearch.ifEmpty { addon.catalogs.take(2) }
            targets.map { addon to it }
        }

        searchableCatalogs.map { (addon, catalog) ->
            async {
                fetchCatalog(
                    addon = addon,
                    type = catalog.type,
                    catalogId = catalog.id,
                    searchQuery = query
                )
            }
        }.awaitAll().flatten().distinctBy { "${it.type}:${it.id}" }
    }

    suspend fun fetchMeta(
        addons: List<AddonManifest>,
        fallbackItem: MediaItem
    ): MediaItem {
        for (addon in addons.filter { it.isEnabled }) {
            val url = buildResourceUrl(
                manifestUrl = addon.manifestUrl,
                resource = "meta",
                type = fallbackItem.type,
                id = fallbackItem.id
            )
            val detailed = runCatching {
                val payload = client.get(url).bodyAsText()
                val root = json.parseToJsonElement(payload).jsonObject
                val metaObj = root["meta"] as? JsonObject
                metaObj?.toMediaItem(defaultType = fallbackItem.type, addonName = addon.name)
            }.getOrNull()

            if (detailed != null) {
                return detailed.copy(
                    poster = detailed.poster ?: fallbackItem.poster,
                    background = detailed.background ?: fallbackItem.background,
                    description = detailed.description ?: fallbackItem.description
                )
            }
        }
        return fallbackItem
    }

    suspend fun fetchStreamsFromAllAddons(
        addons: List<AddonManifest>,
        type: String,
        videoId: String
    ): List<MediaStream> = coroutineScope {
        addons.filter { it.isEnabled }.map { addon ->
            async {
                runCatching {
                    val url = buildResourceUrl(
                        manifestUrl = addon.manifestUrl,
                        resource = "stream",
                        type = type,
                        id = videoId
                    )
                    val payload = client.get(url).bodyAsText()
                    val root = json.parseToJsonElement(payload).jsonObject
                    root.optArray("streams").mapNotNull { el ->
                        (el as? JsonObject)?.toMediaStream(addon.name)
                    }
                }.getOrDefault(emptyList())
            }
        }.awaitAll().flatten()
    }

    suspend fun fetchSubtitlesFromAllAddons(
        addons: List<AddonManifest>,
        type: String,
        videoId: String
    ): List<SubtitleTrack> = coroutineScope {
        addons.filter { it.isEnabled }.map { addon ->
            async {
                runCatching {
                    val url = buildResourceUrl(
                        manifestUrl = addon.manifestUrl,
                        resource = "subtitles",
                        type = type,
                        id = videoId
                    )
                    val payload = client.get(url).bodyAsText()
                    val root = json.parseToJsonElement(payload).jsonObject
                    root.optArray("subtitles").mapNotNull { el ->
                        val obj = el as? JsonObject ?: return@mapNotNull null
                        val subUrl = obj.optString("url") ?: return@mapNotNull null
                        val lang = obj.optString("lang") ?: "eng"
                        val id = obj.optString("id") ?: subUrl.hashCode().toString()
                        SubtitleTrack(
                            id = id,
                            url = subUrl,
                            lang = lang,
                            label = "${lang.uppercase()} (${addon.name})",
                            format = if (subUrl.endsWith(".ass", true) || subUrl.endsWith(".ssa", true)) "ass" else "srt"
                        )
                    }
                }.getOrDefault(emptyList())
            }
        }.awaitAll().flatten()
    }

    private fun JsonObject.toMediaItem(defaultType: String, addonName: String): MediaItem? {
        val id = optString("id") ?: return null
        val name = optString("name") ?: optString("title") ?: return null
        val type = optString("type") ?: defaultType
        val poster = optString("poster") ?: optString("landscapePoster")
        val background = optString("background") ?: optString("banner") ?: optString("landscapePoster") ?: poster
        val logo = optString("logo")
        val description = optString("description") ?: optString("overview")
        val releaseInfo = optString("releaseInfo") ?: optString("year") ?: optString("released")?.take(4)
        val imdbRating = optString("imdbRating")?.takeIf { it.isNotBlank() && it != "null" }
        val runtime = optString("runtime")
        val genres = optStringList("genres").ifEmpty { optStringList("genre") }
        val director = optStringList("director")
        val cast = optStringList("cast")

        val trailers = optArray("trailers").mapNotNull { tEl ->
            val tObj = tEl as? JsonObject ?: return@mapNotNull null
            val src = tObj.optString("source") ?: return@mapNotNull null
            TrailerSource(source = src, type = tObj.optString("type") ?: "Trailer")
        }

        val videos = optArray("videos").mapNotNull { vEl ->
            val vObj = vEl as? JsonObject ?: return@mapNotNull null
            val vId = vObj.optString("id") ?: return@mapNotNull null
            val vTitle = vObj.optString("title") ?: vObj.optString("name") ?: ""
            val season = vObj.optInt("season") ?: 1
            val episode = vObj.optInt("episode") ?: vObj.optInt("number") ?: 1
            EpisodeVideo(
                id = vId,
                title = vTitle,
                season = season,
                episode = episode,
                overview = vObj.optString("overview") ?: vObj.optString("description"),
                thumbnail = vObj.optString("thumbnail"),
                released = vObj.optString("released")
            )
        }

        return MediaItem(
            id = id,
            type = type,
            name = name,
            poster = poster,
            background = background,
            logo = logo,
            description = description,
            releaseInfo = releaseInfo,
            imdbRating = imdbRating,
            genres = genres,
            runtime = runtime,
            director = director,
            cast = cast,
            videos = videos,
            trailers = trailers,
            sourceOrigin = addonName
        )
    }

    private fun JsonObject.toMediaStream(addonName: String): MediaStream? {
        val url = optString("url")
        val infoHash = optString("infoHash")
        val ytId = optString("ytId")
        val externalUrl = optString("externalUrl")
        if (url.isNullOrBlank() && infoHash.isNullOrBlank() && ytId.isNullOrBlank() && externalUrl.isNullOrBlank()) {
            return null
        }

        val name = optString("name")
        val title = optString("title")
        val description = optString("description")
        val fileIdx = optInt("fileIdx")
        val hintsObj = this["behaviorHints"] as? JsonObject
        val hints = hintsObj?.let {
            StreamBehaviorHints(
                bingeGroup = it.optString("bingeGroup"),
                filename = it.optString("filename"),
                videoSize = it.optLong("videoSize"),
                notWebReady = it.optBoolean("notWebReady")
            )
        }

        val combinedText = "${name.orEmpty()} ${title.orEmpty()} ${description.orEmpty()}".uppercase()
        val isDebrid = !url.isNullOrBlank() && ("RD+" in combinedText || "AD+" in combinedText || "TB+" in combinedText || "PM+" in combinedText || "DEBRID" in combinedText)

        return MediaStream(
            name = name,
            title = title,
            description = description,
            url = url,
            infoHash = infoHash,
            fileIdx = fileIdx,
            ytId = ytId,
            externalUrl = externalUrl,
            behaviorHints = hints,
            addonName = addonName,
            isDebridCached = isDebrid || (!url.isNullOrBlank() && infoHash.isNullOrBlank()),
            debridProvider = if (isDebrid) "Debrid Cached" else if (!url.isNullOrBlank()) "Direct HTTP" else null
        )
    }

    private fun JsonObject.optString(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }

    private fun JsonObject.optInt(key: String): Int? =
        (this[key] as? JsonPrimitive)?.intOrNull ?: optString(key)?.toIntOrNull()

    private fun JsonObject.optLong(key: String): Long? =
        (this[key] as? JsonPrimitive)?.longOrNull ?: optString(key)?.toLongOrNull()

    private fun JsonObject.optBoolean(key: String): Boolean =
        (this[key] as? JsonPrimitive)?.booleanOrNull == true

    private fun JsonObject.optArray(key: String): JsonArray =
        (this[key] as? JsonArray) ?: JsonArray(emptyList())

    private fun JsonObject.optStringList(key: String): List<String> =
        when (val el: JsonElement? = this[key]) {
            is JsonArray -> el.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf { s -> s.isNotBlank() } }
            is JsonPrimitive -> el.contentOrNull?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
            else -> emptyList()
        }

    private fun encodePathSegment(input: String): String = buildString {
        input.encodeToByteArray().forEach { byte ->
            val v = byte.toInt() and 0xFF
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
}
