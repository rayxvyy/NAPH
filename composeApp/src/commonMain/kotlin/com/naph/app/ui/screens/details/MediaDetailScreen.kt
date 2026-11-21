package com.naph.app.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naph.app.core.model.EpisodeVideo
import com.naph.app.core.model.LayoutCustomizationConfig
import com.naph.app.core.model.MediaItem
import com.naph.app.core.model.MediaStream
import com.naph.app.ui.components.AggregatedRatingsBar
import com.naph.app.ui.components.FastNetworkImage
import com.naph.app.ui.components.ModernFilterPill
import com.naph.app.ui.components.ModernGlassButton
import com.naph.app.ui.components.ModernIconPill
import com.naph.app.ui.components.ModernPrimaryButton
import com.naph.app.ui.components.liquidGlassBorderBrush
import com.naph.app.ui.components.liquidGlassFillBrush
import com.naph.app.ui.state.NaphAppState
import com.naph.app.ui.state.ProfileSettingsPage
import com.naph.app.ui.theme.NaphColors
import kotlinx.coroutines.delay

@Composable
fun MediaDetailScreen(
    item: MediaItem,
    appState: NaphAppState,
    modifier: Modifier = Modifier
) {
    val streams by appState.availableStreams.collectAsState()
    val isLoadingStreams by appState.isLoadingStreams.collectAsState()
    val activeTrailerYtId by appState.activeTrailerYtId.collectAsState()
    val layoutConfig by appState.layoutConfig.collectAsState()
    val debridConfig by appState.debridConfig.collectAsState()
    val mediaServers by appState.mediaServers.collectAsState()

    val hasDebridConnected = appState.debridRepo.hasActiveDebrid(debridConfig)
    val hasServersConnected = mediaServers.isNotEmpty()

    val seasons = remember(item.videos) {
        item.videos.map { it.season }.distinct().sorted().ifEmpty { listOf(1) }
    }
    var selectedSeason by remember(item.id) { mutableStateOf(seasons.firstOrNull() ?: 1) }
    var selectedEpisode by remember(item.id, item.videos) {
        mutableStateOf(item.videos.firstOrNull())
    }
    var resolutionFilter by remember { mutableStateOf("ALL") }

    val filteredStreams = remember(streams, resolutionFilter) {
        when (resolutionFilter) {
            "4K" -> streams.filter { it.detectedResolution.contains("4K") }
            "1080p" -> streams.filter { it.detectedResolution.contains("1080") }
            "720p" -> streams.filter { it.detectedResolution.contains("720") }
            else -> streams
        }
    }

    val bg = MaterialTheme.colorScheme.background

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        val isCompact = maxWidth < 640.dp
        val hPad = if (isCompact) 18.dp else 28.dp

        LazyColumn(
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 14.dp else 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Responsive Cinema Hero Header (Vertical Apple TV / Nuvio layout on iPhone, Widescreen on Desktop)
            item(key = "hero_header") {
                if (activeTrailerYtId != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompact) 320.dp else 420.dp)
                            .statusBarsPadding()
                    ) {
                        InAppYouTubeTrailerOverlay(
                            ytId = activeTrailerYtId!!,
                            title = "${item.name} — Trailer",
                            onClose = { appState.openInAppTrailer(null) },
                            onPlayFullMovie = {
                                appState.openInAppTrailer(null)
                                streams.firstOrNull()?.let { appState.startPlayback(item, it, selectedEpisode) }
                            }
                        )
                    }
                } else if (isCompact) {
                    // MOBILE / iPHONE RESPONSIVE DETAIL HEADER (Zero cramped text, generous line heights!)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(290.dp)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            FastNetworkImage(
                                url = item.background ?: item.poster,
                                contentDescription = item.name,
                                widthPx = 720,
                                heightPx = 420,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0x88070709),
                                                Color(0x33070709),
                                                bg
                                            )
                                        )
                                    )
                            )
                            ModernIconPill(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                onClick = { appState.closeMediaDetail() },
                                size = 38.dp,
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = hPad)
                        ) {
                            val metaSummary = buildList {
                                add(item.type.uppercase())
                                item.releaseInfo?.let { add(it) }
                                item.runtime?.let { add(it) }
                            }.joinToString("  •  ")

                            Text(
                                text = metaSummary,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    letterSpacing = 0.6.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 24.sp,
                                    lineHeight = 30.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )

                            if (item.genres.isNotEmpty()) {
                                Text(
                                    text = item.genres.take(4).joinToString(" • "),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp
                                    ),
                                    color = NaphColors.TextSecondary
                                )
                            }

                            item.aggregatedRatings?.let { ratings ->
                                AggregatedRatingsBar(
                                    ratings = ratings,
                                    layoutConfig = layoutConfig
                                )
                            }

                            if (!item.description.isNullOrBlank()) {
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        lineHeight = 21.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.88f),
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.height(2.dp))

                            // Full-width Primary Play Button
                            ModernPrimaryButton(
                                text = if (selectedEpisode != null) "Play S${selectedEpisode!!.season}:E${selectedEpisode!!.episode}" else "Play",
                                icon = Icons.Default.PlayArrow,
                                containerColor = Color.White,
                                contentColor = Color.Black,
                                height = 44.dp,
                                onClick = {
                                    val bestStream = streams.firstOrNull()
                                    if (bestStream != null) {
                                        appState.startPlayback(item, bestStream, selectedEpisode)
                                    } else {
                                        appState.showBanner("Install a stream addon in Profile → Content & Discovery")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Side-by-side Liquid Glass Secondary Actions
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val ytId = item.trailers.firstOrNull()?.source
                                    ?: item.trailerStreams.firstOrNull()?.ytId
                                if (!ytId.isNullOrBlank()) {
                                    ModernGlassButton(
                                        text = "Trailer",
                                        icon = Icons.Default.Movie,
                                        iconTint = NaphColors.CyanGlow,
                                        height = 40.dp,
                                        onClick = { appState.openInAppTrailer(ytId) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                val inList = appState.isInWatchlist(item.id)
                                ModernGlassButton(
                                    text = if (inList) "In Library" else "+ Library",
                                    icon = if (inList) Icons.Default.Check else Icons.Default.Add,
                                    iconTint = if (inList) NaphColors.EmeraldSeed else Color.White,
                                    isActive = inList,
                                    height = 40.dp,
                                    onClick = { appState.toggleWatchlist(item) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                } else {
                    // DESKTOP / TABLET WIDESCREEN DETAIL HEADER
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(410.dp)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        FastNetworkImage(
                            url = item.background ?: item.poster,
                            contentDescription = item.name,
                            widthPx = 960,
                            heightPx = 540,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x88070709),
                                            Color(0x66070709),
                                            bg
                                        )
                                    )
                                )
                        )

                        ModernIconPill(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = { appState.closeMediaDetail() },
                            size = 38.dp,
                            modifier = Modifier.padding(20.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 28.dp, vertical = 22.dp)
                        ) {
                            FastNetworkImage(
                                url = item.poster,
                                contentDescription = item.name,
                                widthPx = 280,
                                heightPx = 420,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(142.dp)
                                    .height(213.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, liquidGlassBorderBrush(), RoundedCornerShape(12.dp))
                            )

                            Spacer(Modifier.width(24.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = item.type.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    item.releaseInfo?.let {
                                        Text("• $it", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp), color = NaphColors.TextSecondary)
                                    }
                                    item.runtime?.let {
                                        Text("• $it", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp), color = NaphColors.TextSecondary)
                                    }
                                    if (item.genres.isNotEmpty()) {
                                        Text(
                                            text = "• " + item.genres.joinToString(", "),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                                            color = NaphColors.TextSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 28.sp,
                                        lineHeight = 34.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )

                                item.aggregatedRatings?.let { ratings ->
                                    AggregatedRatingsBar(
                                        ratings = ratings,
                                        layoutConfig = layoutConfig
                                    )
                                }

                                if (!item.description.isNullOrBlank()) {
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            lineHeight = 21.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.88f),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(Modifier.height(4.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ModernPrimaryButton(
                                        text = if (selectedEpisode != null) "Play S${selectedEpisode!!.season}:E${selectedEpisode!!.episode}" else "Play",
                                        icon = Icons.Default.PlayArrow,
                                        containerColor = Color.White,
                                        contentColor = Color.Black,
                                        onClick = {
                                            val bestStream = streams.firstOrNull()
                                            if (bestStream != null) {
                                                appState.startPlayback(item, bestStream, selectedEpisode)
                                            } else {
                                                appState.showBanner("Install a stream addon (e.g. Torrentio) in Profile → Content & Discovery")
                                            }
                                        }
                                    )

                                    val ytId = item.trailers.firstOrNull()?.source
                                        ?: item.trailerStreams.firstOrNull()?.ytId
                                    if (!ytId.isNullOrBlank()) {
                                        ModernGlassButton(
                                            text = "Trailer",
                                            icon = Icons.Default.Movie,
                                            iconTint = NaphColors.CyanGlow,
                                            onClick = { appState.openInAppTrailer(ytId) }
                                        )
                                    }

                                    val inList = appState.isInWatchlist(item.id)
                                    ModernGlassButton(
                                        text = if (inList) "In Library" else "+ Library",
                                        icon = if (inList) Icons.Default.Check else Icons.Default.Add,
                                        iconTint = if (inList) NaphColors.EmeraldSeed else Color.White,
                                        isActive = inList,
                                        onClick = { appState.toggleWatchlist(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Cast & Director Bar
            if (item.cast.isNotEmpty() || item.director.isNotEmpty()) {
                item(key = "cast_bar") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = hPad)
                    ) {
                        if (item.director.isNotEmpty()) {
                            Text(
                                text = "Director: ${item.director.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                                color = NaphColors.TextSecondary
                            )
                        }
                        if (item.cast.isNotEmpty()) {
                            Text(
                                text = "Cast: ${item.cast.take(6).joinToString(" • ")}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                                color = NaphColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // 3. Season & Episode Selector (if Series / Anime)
            if (item.videos.isNotEmpty()) {
                item(key = "episodes_selector") {
                    val seasonEps = remember(item.videos, selectedSeason) {
                        item.videos.filter { it.season == selectedSeason }
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Episodes",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = hPad)
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = hPad),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(seasons) { seasonNum ->
                                ModernFilterPill(
                                    label = "Season $seasonNum",
                                    selected = selectedSeason == seasonNum,
                                    onClick = { selectedSeason = seasonNum }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = hPad),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(seasonEps, key = { it.id }) { ep ->
                                EpisodeCard(
                                    episode = ep,
                                    isSelected = selectedEpisode?.id == ep.id,
                                    onClick = {
                                        selectedEpisode = ep
                                        appState.loadStreamsAndMetadataFor(item, ep)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Available Streams Header + Horizontally Scrollable Resolution Filter Pills
            item(key = "streams_header") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = hPad)) {
                        Text(
                            text = buildString {
                                append("Streams (${filteredStreams.size})")
                                if (hasDebridConnected) append(" • Debrid")
                                if (hasServersConnected) append(" • Server")
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            color = Color.White
                        )
                        Text(
                            text = "Tap any stream below to play",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = NaphColors.TextSecondary
                        )
                    }

                    if (streams.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = hPad),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filters = listOf(
                                "ALL" to "All (${streams.size})",
                                "4K" to "4K UHD",
                                "1080p" to "1080p",
                                "720p" to "720p"
                            )
                            items(filters) { (key, label) ->
                                ModernFilterPill(
                                    label = label,
                                    selected = resolutionFilter == key,
                                    onClick = { resolutionFilter = key }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Lazy Virtualized Stream Cards
            if (isLoadingStreams) {
                item(key = "streams_loading") {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            } else if (streams.isEmpty()) {
                item(key = "streams_empty") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = hPad)
                            .clip(RoundedCornerShape(14.dp))
                            .background(liquidGlassFillBrush())
                            .border(1.dp, liquidGlassBorderBrush(), RoundedCornerShape(14.dp))
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "No Streams Found for '${item.name}'",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Install a stream provider addon (such as Torrentio) in Profile → Content & Discovery → Installed Addons.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, lineHeight = 19.sp),
                            color = NaphColors.TextSecondary
                        )
                        ModernPrimaryButton(
                            text = "Add Stream Addon",
                            icon = Icons.Default.Extension,
                            onClick = { appState.navigateSettingsPage(ProfileSettingsPage.ADDONS_MANAGER) }
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = filteredStreams,
                    key = { idx, s -> "${s.addonName}:${s.infoHash ?: s.url ?: idx}:$idx" }
                ) { _, stream ->
                    StreamRowCard(
                        stream = stream,
                        layoutConfig = layoutConfig,
                        hasDebridConnected = hasDebridConnected,
                        isCompact = isCompact,
                        onClick = {
                            appState.startPlayback(item, stream, selectedEpisode)
                        },
                        modifier = Modifier.padding(horizontal = hPad)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamRowCard(
    stream: MediaStream,
    layoutConfig: LayoutCustomizationConfig,
    hasDebridConnected: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(liquidGlassFillBrush())
            .border(1.dp, liquidGlassBorderBrush(), shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Left Resolution Pill
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(if (isCompact) 62.dp else 74.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFF0A0D14))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(9.dp))
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = stream.detectedResolution,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = if (isCompact) 11.sp else 11.5.sp),
                    color = if (stream.detectedResolution.contains("4K")) NaphColors.GoldRating else Color.White,
                    fontWeight = FontWeight.Bold
                )
                stream.detectedHdrBadge?.let { hdr ->
                    Text(
                        text = hdr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        color = NaphColors.CyanGlow,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Badges Row (Compact & Crisp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (layoutConfig.showStreamFileSizeBadge) {
                        stream.detectedFileSizeLabel?.let { sizeLabel ->
                            StreamSpecBadge(
                                text = "💾 $sizeLabel",
                                bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                textColor = Color.White
                            )
                        }
                    }

                    if (layoutConfig.showStreamSeedersBadge) {
                        stream.parsedSeeders?.let { seeds ->
                            StreamSpecBadge(
                                text = "👤 $seeds",
                                bgColor = NaphColors.EmeraldSeed.copy(alpha = 0.18f),
                                textColor = NaphColors.EmeraldSeed
                            )
                        }
                    }

                    if (layoutConfig.showStreamCodecAndAudioBadges) {
                        stream.detectedVideoCodec?.let { codec ->
                            StreamSpecBadge(
                                text = codec,
                                bgColor = NaphColors.CyanGlow.copy(alpha = 0.15f),
                                textColor = NaphColors.CyanGlow
                            )
                        }
                        if (!isCompact) {
                            stream.detectedAudioBadge?.let { audio ->
                                StreamSpecBadge(
                                    text = audio,
                                    bgColor = NaphColors.GoldRating.copy(alpha = 0.15f),
                                    textColor = NaphColors.GoldRating
                                )
                            }
                            stream.detectedSourceQuality?.let { q ->
                                StreamSpecBadge(
                                    text = q,
                                    bgColor = Color.White.copy(alpha = 0.08f),
                                    textColor = NaphColors.TextSecondary
                                )
                            }
                        }
                    }

                    if (hasDebridConnected && stream.isDebridCached) {
                        StreamSpecBadge(
                            text = "⚡ ${stream.debridProvider ?: "Debrid"}",
                            bgColor = NaphColors.EmeraldSeed.copy(alpha = 0.22f),
                            textColor = NaphColors.EmeraldSeed
                        )
                    }
                }

                Text(
                    text = stream.cleanTitleLine,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = stream.addonName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = NaphColors.TextSecondary,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        if (isCompact) {
            ModernIconPill(
                icon = Icons.Default.PlayArrow,
                contentDescription = "Play",
                isActive = true,
                size = 34.dp,
                onClick = onClick
            )
        } else {
            ModernPrimaryButton(
                text = "Play",
                icon = Icons.Default.PlayArrow,
                height = 34.dp,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun StreamSpecBadge(
    text: String,
    bgColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun InAppYouTubeTrailerOverlay(
    ytId: String,
    title: String,
    onClose: () -> Unit,
    onPlayFullMovie: () -> Unit
) {
    var progress by remember { mutableStateOf(0.05f) }
    LaunchedEffect(ytId) {
        while (progress < 1f) {
            delay(300)
            progress = (progress + 0.01f).coerceAtMost(1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2070709))
            .padding(18.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(liquidGlassFillBrush())
                .border(1.dp, liquidGlassBorderBrush(NaphColors.CyanGlow), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = NaphColors.CyanGlow, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                ModernIconPill(
                    icon = Icons.Default.Close,
                    contentDescription = "Close Trailer",
                    onClick = onClose,
                    size = 30.dp
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                FastNetworkImage(
                    url = "https://img.youtube.com/vi/$ytId/maxresdefault.jpg",
                    contentDescription = title,
                    widthPx = 640,
                    heightPx = 360,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x66000000))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "▶ TRAILER ($ytId)",
                        style = MaterialTheme.typography.labelLarge,
                        color = NaphColors.CyanGlow
                    )
                    Spacer(Modifier.height(10.dp))
                    ModernPrimaryButton(
                        text = "Play Full Stream",
                        icon = Icons.Default.PlayArrow,
                        onClick = onPlayFullMovie
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                color = NaphColors.CyanGlow,
                trackColor = NaphColors.ElevatedBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: EpisodeVideo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .width(210.dp)
            .clip(shape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color(0xCC131724))
            .border(
                width = 1.dp,
                brush = liquidGlassBorderBrush(if (isSelected) MaterialTheme.colorScheme.primary else null),
                shape = shape
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(
            text = "S${episode.season} • EP ${episode.episode}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = if (isSelected) NaphColors.CyanGlow else NaphColors.TextSecondary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = episode.displayTitle,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.5.sp, lineHeight = 18.sp),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!episode.overview.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = episode.overview,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                color = NaphColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
