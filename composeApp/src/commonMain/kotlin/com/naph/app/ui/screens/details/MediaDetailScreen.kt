package com.naph.app.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

    LazyColumn(
        contentPadding = PaddingValues(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // 1. Top Cinema Backdrop + In-App Trailer Stage
        item(key = "hero_header") {
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
                    size = 36.dp,
                    modifier = Modifier.padding(20.dp)
                )

                if (activeTrailerYtId != null) {
                    InAppYouTubeTrailerOverlay(
                        ytId = activeTrailerYtId!!,
                        title = "${item.name} — Official Trailer",
                        onClose = { appState.openInAppTrailer(null) },
                        onPlayFullMovie = {
                            appState.openInAppTrailer(null)
                            streams.firstOrNull()?.let { appState.startPlayback(item, it, selectedEpisode) }
                        }
                    )
                } else {
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
                                .width(138.dp)
                                .height(207.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                        )

                        Spacer(Modifier.width(22.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = item.type.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                item.releaseInfo?.let {
                                    Text("• $it", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = NaphColors.TextSecondary)
                                }
                                item.runtime?.let {
                                    Text("• $it", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = NaphColors.TextSecondary)
                                }
                                if (item.genres.isNotEmpty()) {
                                    Text(
                                        text = "• " + item.genres.joinToString(", "),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = NaphColors.TextSecondary
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(8.dp))

                            item.aggregatedRatings?.let { ratings ->
                                AggregatedRatingsBar(
                                    ratings = ratings,
                                    layoutConfig = layoutConfig
                                )
                                Spacer(Modifier.height(10.dp))
                            }

                            if (!item.description.isNullOrBlank()) {
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                    color = Color.White.copy(alpha = 0.88f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(14.dp))
                            }

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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)
                ) {
                    if (item.director.isNotEmpty()) {
                        Column {
                            Text("DIRECTOR", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = NaphColors.TextMuted)
                            Spacer(Modifier.height(2.dp))
                            Text(item.director.joinToString(", "), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp), color = Color.White)
                        }
                    }
                    if (item.cast.isNotEmpty()) {
                        Column {
                            Text("CAST", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = NaphColors.TextMuted)
                            Spacer(Modifier.height(2.dp))
                            Text(item.cast.joinToString(" • "), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp), color = Color.White)
                        }
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
                Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Episodes", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(Modifier.width(6.dp))
                        seasons.forEach { seasonNum ->
                            ModernFilterPill(
                                label = "Season $seasonNum",
                                selected = selectedSeason == seasonNum,
                                onClick = { selectedSeason = seasonNum }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

        // 4. Available Streams Header + Filter Pills
        item(key = "streams_header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 6.dp)
            ) {
                Column {
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
                        text = "Select a stream to start playback",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = NaphColors.TextSecondary
                    )
                }

                if (streams.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "ALL" to "All (${streams.size})",
                            "4K" to "4K UHD",
                            "1080p" to "1080p",
                            "720p" to "720p"
                        ).forEach { (key, label) ->
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

        // 5. Lazy Stream Items (100% Lazy Virtualized Rows — Zero Scroll Lag Even With 200+ Streams!)
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
                        .padding(horizontal = 28.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10131C))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = "No Streams Found for '${item.name}'",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Add a stream provider addon (e.g. Torrentio) in Profile → Content & Discovery → Installed Addons.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
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
                    onClick = {
                        appState.startPlayback(item, stream, selectedEpisode)
                    },
                    modifier = Modifier.padding(horizontal = 28.dp)
                )
            }
        }
    }
}

@Composable
private fun StreamRowCard(
    stream: MediaStream,
    layoutConfig: LayoutCustomizationConfig,
    hasDebridConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0xFF10131C))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(11.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Compact Left Resolution Pill
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(74.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0A0C12))
                    .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(8.dp))
                    .padding(vertical = 7.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = stream.detectedResolution,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    color = if (stream.detectedResolution.contains("4K")) NaphColors.GoldRating else Color.White,
                    fontWeight = FontWeight.Bold
                )
                stream.detectedHdrBadge?.let { hdr ->
                    Text(
                        text = hdr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        color = NaphColors.CyanGlow
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Fast Single-Pass Metadata Column (No expensive FlowRow layout overhead!)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (layoutConfig.showStreamFileSizeBadge) {
                        stream.detectedFileSizeLabel?.let { sizeLabel ->
                            StreamSpecBadge(
                                text = "💾 $sizeLabel",
                                bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                textColor = Color.White
                            )
                        }
                    }

                    if (layoutConfig.showStreamSeedersBadge) {
                        stream.parsedSeeders?.let { seeds ->
                            StreamSpecBadge(
                                text = "👤 $seeds",
                                bgColor = NaphColors.EmeraldSeed.copy(alpha = 0.16f),
                                textColor = NaphColors.EmeraldSeed
                            )
                        }
                    }

                    if (layoutConfig.showStreamCodecAndAudioBadges) {
                        stream.detectedVideoCodec?.let { codec ->
                            StreamSpecBadge(
                                text = codec,
                                bgColor = NaphColors.CyanGlow.copy(alpha = 0.14f),
                                textColor = NaphColors.CyanGlow
                            )
                        }
                        stream.detectedAudioBadge?.let { audio ->
                            StreamSpecBadge(
                                text = audio,
                                bgColor = NaphColors.GoldRating.copy(alpha = 0.14f),
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

                    if (hasDebridConnected && stream.isDebridCached) {
                        StreamSpecBadge(
                            text = "⚡ ${stream.debridProvider ?: "Debrid"}",
                            bgColor = NaphColors.EmeraldSeed.copy(alpha = 0.20f),
                            textColor = NaphColors.EmeraldSeed
                        )
                    }

                    StreamSpecBadge(
                        text = stream.addonName,
                        bgColor = Color.White.copy(alpha = 0.05f),
                        textColor = NaphColors.TextSecondary
                    )
                }

                Spacer(Modifier.height(5.dp))

                Text(
                    text = stream.cleanTitleLine,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val extraInfo = stream.displayDetails
                if (extraInfo != stream.cleanTitleLine) {
                    Text(
                        text = extraInfo,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp),
                        color = NaphColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        ModernPrimaryButton(
            text = "Play",
            icon = Icons.Default.PlayArrow,
            height = 32.dp,
            onClick = onClick
        )
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
            .clip(RoundedCornerShape(5.dp))
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
            .padding(24.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, NaphColors.CyanGlow.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = NaphColors.CyanGlow, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
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
                    .clip(RoundedCornerShape(10.dp))
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
    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else Color(0xFF10131C))
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(
            text = "S${episode.season} • EP ${episode.episode}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (isSelected) NaphColors.CyanGlow else NaphColors.TextSecondary
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = episode.displayTitle,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!episode.overview.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = episode.overview,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp),
                color = NaphColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
