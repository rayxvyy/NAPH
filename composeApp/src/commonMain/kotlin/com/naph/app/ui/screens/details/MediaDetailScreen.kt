package com.naph.app.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import coil3.compose.AsyncImage
import com.naph.app.core.model.EpisodeVideo
import com.naph.app.core.model.LayoutCustomizationConfig
import com.naph.app.core.model.MediaItem
import com.naph.app.core.model.MediaStream
import com.naph.app.ui.components.AggregatedRatingsBar
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
    var resolutionFilter by remember { mutableStateOf("ALL") } // "ALL", "4K", "1080p", "720p"

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
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // 1. Top Cinema Backdrop + In-App Trailer Stage
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AsyncImage(
                    model = item.background ?: item.poster,
                    contentDescription = item.name,
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
                                    Color(0x55070709),
                                    bg
                                )
                            )
                        )
                )

                IconButton(
                    onClick = { appState.closeMediaDetail() },
                    modifier = Modifier
                        .padding(20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC12141C))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

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
                            .padding(horizontal = 32.dp, vertical = 24.dp)
                    ) {
                        AsyncImage(
                            model = item.poster,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(150.dp)
                                .height(225.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                        )

                        Spacer(Modifier.width(24.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = item.type.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                item.releaseInfo?.let {
                                    Text("• $it", style = MaterialTheme.typography.labelSmall, color = NaphColors.TextSecondary)
                                }
                                item.runtime?.let {
                                    Text("• $it", style = MaterialTheme.typography.labelSmall, color = NaphColors.TextSecondary)
                                }
                                if (item.genres.isNotEmpty()) {
                                    Text(
                                        text = "• " + item.genres.joinToString(", "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NaphColors.TextSecondary
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.White
                            )

                            Spacer(Modifier.height(10.dp))

                            // Shows ratings enabled by user in Layout Customization Settings!
                            item.aggregatedRatings?.let { ratings ->
                                AggregatedRatingsBar(
                                    ratings = ratings,
                                    layoutConfig = layoutConfig
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            if (!item.description.isNullOrBlank()) {
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.90f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(16.dp))
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        val bestStream = streams.firstOrNull()
                                        if (bestStream != null) {
                                            appState.startPlayback(item, bestStream, selectedEpisode)
                                        } else {
                                            appState.showBanner("Install a stream addon (e.g. Torrentio) in Profile → Content & Discovery to fetch streams")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 13.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedEpisode != null) "Play S${selectedEpisode!!.season}:E${selectedEpisode!!.episode}" else "Play",
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                val ytId = item.trailers.firstOrNull()?.source
                                    ?: item.trailerStreams.firstOrNull()?.ytId
                                if (!ytId.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = { appState.openInAppTrailer(ytId) },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp)
                                    ) {
                                        Icon(Icons.Default.Movie, contentDescription = null, tint = NaphColors.CyanGlow)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Trailer", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                val inList = appState.isInWatchlist(item.id)
                                OutlinedButton(
                                    onClick = { appState.toggleWatchlist(item) },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp)
                                ) {
                                    Icon(
                                        imageVector = if (inList) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (inList) NaphColors.EmeraldSeed else Color.White
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (inList) "In Library" else "+ Library", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Cast & Director Bar
        if (item.cast.isNotEmpty() || item.director.isNotEmpty()) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    if (item.director.isNotEmpty()) {
                        Column {
                            Text("DIRECTOR", style = MaterialTheme.typography.labelSmall, color = NaphColors.TextMuted)
                            Spacer(Modifier.height(4.dp))
                            Text(item.director.joinToString(", "), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                    }
                    if (item.cast.isNotEmpty()) {
                        Column {
                            Text("CAST", style = MaterialTheme.typography.labelSmall, color = NaphColors.TextMuted)
                            Spacer(Modifier.height(4.dp))
                            Text(item.cast.joinToString(" • "), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                    }
                }
            }
        }

        // 3. Season & Episode Selector (if Series / Anime)
        if (item.videos.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 32.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Episodes", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        seasons.forEach { seasonNum ->
                            FilterChip(
                                selected = selectedSeason == seasonNum,
                                onClick = { selectedSeason = seasonNum },
                                label = { Text("Season $seasonNum") }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        val seasonEps = item.videos.filter { it.season == selectedSeason }
                        items(seasonEps, key = { it.id }) { ep ->
                            val isSelected = selectedEpisode?.id == ep.id
                            EpisodeCard(
                                episode = ep,
                                isSelected = isSelected,
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

        // 4. Available Streams Section (Hides Debrid / Server labels if user hasn't connected them!)
        item {
            Column(modifier = Modifier.padding(horizontal = 32.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = buildString {
                                append("Available Streams (${filteredStreams.size})")
                                if (hasDebridConnected) append(" • Debrid Connected")
                                if (hasServersConnected) append(" • Media Server Connected")
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Select a stream below to start playback",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NaphColors.TextSecondary
                        )
                    }

                    // Clean Resolution Filter Chips (No unconnected Debrid/Server clutter!)
                    if (streams.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "ALL" to "All (${streams.size})",
                                "4K" to "4K UHD",
                                "1080p" to "1080p",
                                "720p" to "720p"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = resolutionFilter == key,
                                    onClick = { resolutionFilter = key },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (isLoadingStreams) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (streams.isEmpty()) {
                    // Clean guidance if user only installed a catalog addon (e.g. Cinemeta) without a stream addon
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                            .padding(22.dp)
                    ) {
                        Text(
                            text = "No Streams Found for '${item.name}'",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Make sure you have installed a stream provider addon (such as Torrentio or your preferred P2P/HTTP stream addon) in Profile → Content & Discovery → Installed Addons.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NaphColors.TextSecondary
                        )
                        Button(
                            onClick = { appState.navigateSettingsPage(ProfileSettingsPage.ADDONS_MANAGER) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Extension, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Stream Addon", color = Color.White)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredStreams.forEach { stream ->
                            StreamRowCard(
                                stream = stream,
                                layoutConfig = layoutConfig,
                                hasDebridConnected = hasDebridConnected,
                                onClick = {
                                    appState.startPlayback(item, stream, selectedEpisode)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StreamRowCard(
    stream: MediaStream,
    layoutConfig: LayoutCustomizationConfig,
    hasDebridConnected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Left Resolution & HDR Badge Box
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(92.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                    .padding(vertical = 10.dp, horizontal = 6.dp)
            ) {
                Text(
                    text = stream.detectedResolution,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                    color = if (stream.detectedResolution.contains("4K")) NaphColors.GoldRating else Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                stream.detectedHdrBadge?.let { hdr ->
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = hdr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = NaphColors.CyanGlow
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Center Stream Metadata: File Size (MB/GB), Seeders, Video Codec, Audio Format, Source Quality & Filename
            Column(modifier = Modifier.weight(1f)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 1. File Size Badge (MB / GB)
                    if (layoutConfig.showStreamFileSizeBadge) {
                        stream.detectedFileSizeLabel?.let { sizeLabel ->
                            StreamSpecBadge(
                                text = "💾 $sizeLabel",
                                bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                textColor = Color.White
                            )
                        }
                    }

                    // 2. P2P Seeders Badge
                    if (layoutConfig.showStreamSeedersBadge) {
                        stream.parsedSeeders?.let { seeds ->
                            StreamSpecBadge(
                                text = "👤 $seeds Seeds",
                                bgColor = NaphColors.EmeraldSeed.copy(alpha = 0.18f),
                                textColor = NaphColors.EmeraldSeed
                            )
                        }
                    }

                    // 3. Video Codec / Audio / Quality Badges
                    if (layoutConfig.showStreamCodecAndAudioBadges) {
                        stream.detectedSourceQuality?.let { q ->
                            StreamSpecBadge(
                                text = q,
                                bgColor = Color.White.copy(alpha = 0.10f),
                                textColor = NaphColors.TextSecondary
                            )
                        }
                        stream.detectedVideoCodec?.let { codec ->
                            StreamSpecBadge(
                                text = codec,
                                bgColor = NaphColors.CyanGlow.copy(alpha = 0.16f),
                                textColor = NaphColors.CyanGlow
                            )
                        }
                        stream.detectedAudioBadge?.let { audio ->
                            StreamSpecBadge(
                                text = "🔊 $audio",
                                bgColor = NaphColors.GoldRating.copy(alpha = 0.16f),
                                textColor = NaphColors.GoldRating
                            )
                        }
                    }

                    // 4. Only show Debrid badge if stream is actually Debrid or user connected Debrid
                    if (hasDebridConnected && stream.isDebridCached) {
                        StreamSpecBadge(
                            text = "⚡ ${stream.debridProvider ?: "Debrid Instant"}",
                            bgColor = NaphColors.EmeraldSeed.copy(alpha = 0.22f),
                            textColor = NaphColors.EmeraldSeed
                        )
                    }

                    // 5. Addon Name
                    StreamSpecBadge(
                        text = stream.addonName,
                        bgColor = Color.White.copy(alpha = 0.07f),
                        textColor = NaphColors.TextSecondary
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stream.cleanTitleLine,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val extraInfo = stream.displayDetails.replace("\n", " • ")
                if (extraInfo != stream.cleanTitleLine) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = extraInfo,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = NaphColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Play",
                fontWeight = FontWeight.Bold
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
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = textColor,
            fontWeight = FontWeight.SemiBold
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
            .padding(32.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, NaphColors.CyanGlow.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = NaphColors.CyanGlow)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text(
                            text = "In-App Trailer • Video ID: $ytId",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NaphColors.TextSecondary
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Trailer", tint = Color.White)
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = "https://img.youtube.com/vi/$ytId/maxresdefault.jpg",
                    contentDescription = title,
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
                        text = "▶ PLAYING IN-APP TRAILER ($ytId)",
                        style = MaterialTheme.typography.titleMedium,
                        color = NaphColors.CyanGlow
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onPlayFullMovie,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Play Full Stream", color = Color.White)
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                color = NaphColors.CyanGlow,
                trackColor = NaphColors.ElevatedBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
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
            .width(250.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Text(
            text = "S${episode.season} • EPISODE ${episode.episode}",
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) NaphColors.CyanGlow else NaphColors.TextSecondary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = episode.displayTitle,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!episode.overview.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = episode.overview,
                style = MaterialTheme.typography.bodyMedium,
                color = NaphColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
