package com.naph.app.ui.screens.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.naph.app.core.model.MediaItem
import com.naph.app.ui.components.AggregatedRatingsBar
import com.naph.app.ui.components.CinemaPosterCard
import com.naph.app.ui.components.ContinueWatchingCard
import com.naph.app.ui.state.NaphAppState
import com.naph.app.ui.theme.NaphColors
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    appState: NaphAppState,
    modifier: Modifier = Modifier
) {
    val heroItems by appState.heroShowcaseItems.collectAsState()
    val trendingMovies by appState.trendingMovies.collectAsState()
    val trendingSeries by appState.trendingSeries.collectAsState()
    val personalItems by appState.personalServerItems.collectAsState()
    val activeProfile by appState.activeProfile.collectAsState()
    val watchProgressMap by appState.watchProgressByProfile.collectAsState()
    val watchlistMap by appState.watchlistByProfile.collectAsState()

    val continueWatching = watchProgressMap[activeProfile.id].orEmpty()
    val profileWatchlist = watchlistMap[activeProfile.id].orEmpty()

    var heroIndex by remember { mutableStateOf(0) }

    LaunchedEffect(heroItems.size) {
        if (heroItems.isNotEmpty()) {
            while (true) {
                delay(7500)
                heroIndex = (heroIndex + 1) % heroItems.size
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        modifier = modifier
            .fillMaxSize()
            .background(NaphColors.ObsidianBg)
    ) {
        // 1. Apple TV+ / HBO Max Style Full-Bleed Hero Showcase
        if (heroItems.isNotEmpty()) {
            val activeHero = heroItems[heroIndex % heroItems.size]
            item {
                HeroShowcaseStage(
                    item = activeHero,
                    totalSlides = heroItems.size,
                    activeIndex = heroIndex % heroItems.size,
                    isInWatchlist = appState.isInWatchlist(activeHero.id),
                    onSelectSlide = { heroIndex = it },
                    onPlayNow = { appState.openMediaDetail(activeHero) },
                    onWatchTrailer = {
                        appState.openMediaDetail(activeHero)
                        appState.openInAppTrailer(activeHero.trailers.firstOrNull()?.source ?: "Way9Dexny3w")
                    },
                    onToggleWatchlist = { appState.toggleWatchlist(activeHero) }
                )
            }
        }

        // 2. Continue Watching Row (Synced across Windows, Linux, iOS, Android via Supabase)
        if (continueWatching.isNotEmpty()) {
            item {
                Column {
                    SectionHeader(
                        title = "Continue Watching for ${activeProfile.name} ${activeProfile.avatarEmoji}",
                        subtitle = "Synced across Windows, Linux, iOS & Android"
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(continueWatching, key = { it.mediaId }) { entry ->
                            ContinueWatchingCard(
                                entry = entry,
                                onClick = {
                                    val found = (heroItems + trendingMovies + trendingSeries + personalItems)
                                        .find { it.id == entry.mediaId }
                                    if (found != null) {
                                        appState.openMediaDetail(found)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Personal Jellyfin / Emby Server Row (if connected)
        if (personalItems.isNotEmpty()) {
            item {
                CatalogRowSection(
                    title = "From Your Jellyfin & Emby Servers",
                    subtitle = "Direct Play from your personal self-hosted library",
                    items = personalItems,
                    onItemClick = { appState.openMediaDetail(it) }
                )
            }
        }

        // 4. Trending Movies (Cinemeta + Installed Stremio Catalogs)
        item {
            CatalogRowSection(
                title = "Trending Movies • 4K HDR & P2P Swarm",
                subtitle = "Instant sequential P2P streaming & Debrid cached links",
                items = trendingMovies,
                onItemClick = { appState.openMediaDetail(it) }
            )
        }

        // 5. Trending Series & Anime
        item {
            CatalogRowSection(
                title = "Binge-Worthy Series & Anime",
                subtitle = "With smart IntroDB / AniSkip auto-skip & ASS styled subtitles",
                items = trendingSeries,
                onItemClick = { appState.openMediaDetail(it) }
            )
        }

        // 6. My Watchlist Row
        if (profileWatchlist.isNotEmpty()) {
            item {
                CatalogRowSection(
                    title = "${activeProfile.name}'s Watchlist",
                    subtitle = "2-way synced with NAPH Cloud, Trakt.tv & SIMKL",
                    items = profileWatchlist,
                    onItemClick = { appState.openMediaDetail(it) }
                )
            }
        }
    }
}

@Composable
private fun HeroShowcaseStage(
    item: MediaItem,
    totalSlides: Int,
    activeIndex: Int,
    isInWatchlist: Boolean,
    onSelectSlide: (Int) -> Unit,
    onPlayNow: () -> Unit,
    onWatchTrailer: () -> Unit,
    onToggleWatchlist: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(470.dp)
            .background(NaphColors.DeepSurface)
    ) {
        // Backdrop Art
        AsyncImage(
            model = item.background ?: item.poster,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Multi-directional obsidian cinema vignette gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            NaphColors.ObsidianBg,
                            NaphColors.ObsidianBg.copy(alpha = 0.78f),
                            Color.Transparent
                        ),
                        endX = 1100f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x55070709),
                            Color.Transparent,
                            NaphColors.ObsidianBg
                        )
                    )
                )
        )

        // Hero Content Overlay
        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 28.dp)
        ) {
            // Tech Badges Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroSpecChip("4K UHD", NaphColors.ElectricPrimary)
                HeroSpecChip("DOLBY VISION • HDR10+", NaphColors.CyanGlow)
                HeroSpecChip("P2P SEQUENTIAL READY", NaphColors.EmeraldSeed)
                item.releaseInfo?.let { HeroSpecChip(it, NaphColors.TextSecondary) }
                item.runtime?.let { HeroSpecChip(it, NaphColors.TextSecondary) }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.displayLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            item.aggregatedRatings?.let { ratings ->
                AggregatedRatingsBar(ratings = ratings)
                Spacer(Modifier.height(12.dp))
            }

            Text(
                text = item.description.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = NaphColors.TextPrimary.copy(alpha = 0.85f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.65f)
            )

            Spacer(Modifier.height(20.dp))

            // Action Buttons + Carousel Dots
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onPlayNow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NaphColors.TextPrimary,
                            contentColor = NaphColors.ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Stream Now", fontWeight = FontWeight.ExtraBold)
                    }

                    OutlinedButton(
                        onClick = onWatchTrailer,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = NaphColors.CyanGlow)
                        Spacer(Modifier.width(8.dp))
                        Text("In-App Trailer", color = NaphColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onToggleWatchlist,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isInWatchlist) NaphColors.EmeraldSeed else NaphColors.TextPrimary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isInWatchlist) "In Watchlist" else "Watchlist",
                            color = NaphColors.TextPrimary
                        )
                    }
                }

                // Slide Indicators
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(totalSlides) { idx ->
                        val isSelected = idx == activeIndex
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isSelected) 28.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) NaphColors.ElectricPrimary
                                    else NaphColors.TextMuted.copy(alpha = 0.45f)
                                )
                                .clickable { onSelectSlide(idx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSpecChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = color
        )
    }
}

@Composable
private fun CatalogRowSection(
    title: String,
    subtitle: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit
) {
    Column {
        SectionHeader(title = title, subtitle = subtitle)
        Spacer(Modifier.height(14.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.id }) { item ->
                CinemaPosterCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 28.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = NaphColors.TextMuted
        )
    }
}
