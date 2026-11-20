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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
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
import com.naph.app.core.model.MediaItem
import com.naph.app.core.model.MediaStream
import com.naph.app.ui.components.AggregatedRatingsBar
import com.naph.app.ui.state.NaphAppState
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

    val seasons = remember(item.videos) {
        item.videos.map { it.season }.distinct().sorted().ifEmpty { listOf(1) }
    }
    var selectedSeason by remember(item.id) { mutableStateOf(seasons.firstOrNull() ?: 1) }
    var selectedEpisode by remember(item.id) {
        mutableStateOf(item.videos.firstOrNull())
    }
    var streamFilterMode by remember { mutableStateOf("ALL") } // "ALL", "P2P", "DEBRID", "4K"

    val filteredStreams = remember(streams, streamFilterMode) {
        when (streamFilterMode) {
            "P2P" -> streams.filter { it.isP2PTorrent }
            "DEBRID" -> streams.filter { it.isDebridCached }
            "4K" -> streams.filter { it.detectedResolution.contains("4K") }
            else -> streams
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier
            .fillMaxSize()
            .background(NaphColors.ObsidianBg)
    ) {
        // 1. Top Cinema Backdrop + In-App Trailer Stage
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .background(NaphColors.DeepSurface)
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
                                    NaphColors.ObsidianBg
                                )
                            )
                        )
                )

                // Back Button
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

                // In-App YouTube Trailer Embedded Stage Overlay
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
                    // Metadata Header Overlay
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp, vertical = 24.dp)
                    ) {
                        // Poster Thumbnail
                        AsyncImage(
                            model = item.poster,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(150.dp)
                                .height(225.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, NaphColors.ElevatedBorder, RoundedCornerShape(14.dp))
                        )

                        Spacer(Modifier.width(24.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = item.type.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaphColors.CyanGlow
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
                                style = MaterialTheme.typography.displayLarge
                            )

                            Spacer(Modifier.height(12.dp))

                            // Aggregated Ratings (IMDb, Rotten Tomatoes, Metacritic, Trakt, MDBList)
                            item.aggregatedRatings?.let { ratings ->
                                AggregatedRatingsBar(ratings = ratings)
                                Spacer(Modifier.height(14.dp))
                            }

                            Text(
                                text = item.description.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = NaphColors.TextPrimary.copy(alpha = 0.88f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(16.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        val bestStream = streams.firstOrNull()
                                        if (bestStream != null) {
                                            appState.startPlayback(item, bestStream, selectedEpisode)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NaphColors.ElectricPrimary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedEpisode != null) "Play S${selectedEpisode!!.season}:E${selectedEpisode!!.episode}" else "Play Best Stream",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                val ytId = item.trailers.firstOrNull()?.source
                                    ?: item.trailerStreams.firstOrNull()?.ytId
                                    ?: "Way9Dexny3w"
                                OutlinedButton(
                                    onClick = { appState.openInAppTrailer(ytId) },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.Movie, contentDescription = null, tint = NaphColors.CyanGlow)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Play In-App Trailer", color = NaphColors.TextPrimary)
                                }

                                val inList = appState.isInWatchlist(item.id)
                                OutlinedButton(
                                    onClick = { appState.toggleWatchlist(item) },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (inList) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (inList) NaphColors.EmeraldSeed else Color.White
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (inList) "Saved" else "Watchlist", color = Color.White)
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
                            Text(item.director.joinToString(", "), style = MaterialTheme.typography.bodyMedium, color = NaphColors.TextPrimary)
                        }
                    }
                    if (item.cast.isNotEmpty()) {
                        Column {
                            Text("STARRING CAST", style = MaterialTheme.typography.labelSmall, color = NaphColors.TextMuted)
                            Spacer(Modifier.height(4.dp))
                            Text(item.cast.joinToString(" • "), style = MaterialTheme.typography.bodyMedium, color = NaphColors.TextPrimary)
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Episodes", style = MaterialTheme.typography.titleLarge)
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

        // 4. P2P Swarm, Debrid & Personal Server Streams Section
        item {
            Column(modifier = Modifier.padding(horizontal = 32.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Available Streams (P2P Sequential Swarm • Debrid • Personal Servers)",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "P2P streams use NAPH's sequential header prioritization & auto-cleaning temporary cache",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NaphColors.TextMuted
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ALL" to "All (${streams.size})", "P2P" to "P2P Torrent", "DEBRID" to "Debrid / Direct ⚡", "4K" to "4K UHD").forEach { (key, label) ->
                            FilterChip(
                                selected = streamFilterMode == key,
                                onClick = { streamFilterMode = key },
                                label = { Text(label) }
                            )
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
                        CircularProgressIndicator(color = NaphColors.ElectricPrimary)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredStreams.forEach { stream ->
                            StreamRowCard(
                                stream = stream,
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
                .background(NaphColors.DeepSurface)
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
                        Text(title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "In-App YouTube Trailer Engine • Video ID: $ytId (1080p60 HD)",
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
                        colors = ButtonDefaults.buttonColors(containerColor = NaphColors.ElectricPrimary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Switch to Full Movie / Episode Stream")
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
            .background(if (isSelected) NaphColors.ElectricPrimary.copy(alpha = 0.18f) else NaphColors.CardSurface)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) NaphColors.ElectricPrimary else NaphColors.ElevatedBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Text(
            text = "S${episode.season} • EPISODE ${episode.episode}",
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) NaphColors.CyanGlow else NaphColors.TextMuted
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = episode.displayTitle,
            style = MaterialTheme.typography.titleMedium,
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

@Composable
private fun StreamRowCard(
    stream: MediaStream,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NaphColors.CardSurface)
            .border(
                width = 1.dp,
                color = if (stream.isP2PTorrent) NaphColors.CyanGlow.copy(alpha = 0.45f)
                else NaphColors.EmeraldSeed.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Quality & Engine Pill
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NaphColors.DeepSurface)
                    .padding(vertical = 8.dp, horizontal = 6.dp)
            ) {
                Text(
                    text = stream.detectedResolution,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                    color = if (stream.detectedResolution.contains("4K")) NaphColors.GoldRating else NaphColors.TextPrimary
                )
                stream.detectedHdrBadge?.let { hdr ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = hdr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = NaphColors.CyanGlow
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (stream.isP2PTorrent) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NaphColors.CyanGlow.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = NaphColors.CyanGlow,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "P2P SEQUENTIAL TORRENT",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = NaphColors.CyanGlow
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NaphColors.EmeraldSeed.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = NaphColors.EmeraldSeed,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${stream.debridProvider ?: "DIRECT HTTP"} CACHED",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = NaphColors.EmeraldSeed
                            )
                        }
                    }

                    Text(
                        text = stream.addonName,
                        style = MaterialTheme.typography.labelSmall,
                        color = NaphColors.TextMuted
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = stream.displayDetails,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NaphColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (stream.isP2PTorrent) NaphColors.CyanGlow else NaphColors.ElectricPrimary,
                contentColor = if (stream.isP2PTorrent) Color.Black else Color.White
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (stream.isP2PTorrent) "Stream P2P" else "Instant Play",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
