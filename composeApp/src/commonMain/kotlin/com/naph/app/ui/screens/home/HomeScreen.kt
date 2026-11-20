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
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.naph.app.ui.state.ProfileSettingsPage
import com.naph.app.ui.theme.NaphColors
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    appState: NaphAppState,
    modifier: Modifier = Modifier
) {
    val installedAddons by appState.installedAddons.collectAsState()
    val homeCatalogSections by appState.homeCatalogSections.collectAsState()
    val heroItems by appState.heroShowcaseItems.collectAsState()
    val isLoadingCatalogs by appState.isLoadingHomeCatalogs.collectAsState()
    val personalItems by appState.personalServerItems.collectAsState()
    val activeProfile by appState.activeProfile.collectAsState()
    val watchProgressMap by appState.watchProgressByProfile.collectAsState()
    val watchlistMap by appState.watchlistByProfile.collectAsState()
    val layoutConfig by appState.layoutConfig.collectAsState()

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

    // Empty State when NO addons and NO media servers are installed yet (Just like Nuvio!)
    if (installedAddons.isEmpty() && personalItems.isEmpty() && !isLoadingCatalogs) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .width(560.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                    .padding(36.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Text(
                    text = "Welcome to NAPH",
                    style = MaterialTheme.typography.headlineLarge
                )

                Text(
                    text = "NAPH starts clean with zero pre-installed addons. Bring your own Stremio-compatible community addons or connect your personal Jellyfin / Emby media server to populate your Home screen.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = NaphColors.TextSecondary
                )

                Spacer(Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Button(
                        onClick = {
                            appState.navigateSettingsPage(ProfileSettingsPage.ADDONS_MANAGER)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Your First Addon", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            appState.navigateSettingsPage(ProfileSettingsPage.MEDIA_SERVERS)
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = NaphColors.CyanGlow)
                        Spacer(Modifier.width(8.dp))
                        Text("Connect Jellyfin / Emby", color = Color.White)
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Hero Showcase Stage (populated from user-installed addon catalogs)
        if (layoutConfig.showHeroShowcase && heroItems.isNotEmpty()) {
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

        // Loading indicator while fetching catalogs from newly added addons
        if (isLoadingCatalogs) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 18.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(20.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Text(
                        text = "Loading catalogs & artwork from your installed addons...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }

        // 2. Continue Watching Row
        if (layoutConfig.showContinueWatchingRow && continueWatching.isNotEmpty()) {
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
                                    val allItems = homeCatalogSections.flatMap { it.items } + personalItems
                                    val found = allItems.find { it.id == entry.mediaId }
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

        // 4. Every Dynamic Catalog Row from User-Installed Addons!
        items(homeCatalogSections, key = { it.key }) { section ->
            CatalogRowSection(
                title = section.title,
                subtitle = section.addonName,
                items = section.items,
                onItemClick = { appState.openMediaDetail(it) }
            )
        }

        // If user installed a stream-only addon (like Torrentio) that has no catalog rows, guide them
        if (!isLoadingCatalogs && installedAddons.isNotEmpty() && homeCatalogSections.isEmpty() && personalItems.isEmpty()) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Your installed addon (${installedAddons.joinToString { it.name }}) provides streams/subtitles only",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "To see movie, series, or anime catalog rows on the Home screen, install a catalog addon in Profile → Content & Discovery → Installed Addons (for example: Cinemeta, CyberFlix, TMDB, or Anime Kitsu).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NaphColors.TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { appState.navigateSettingsPage(ProfileSettingsPage.ADDONS_MANAGER) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Extension, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Manage Addons")
                        }
                        OutlinedButton(onClick = { appState.refreshLiveCatalogs() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Refresh Catalogs", color = Color.White)
                        }
                    }
                }
            }
        }

        // 5. Watchlist Row
        if (profileWatchlist.isNotEmpty()) {
            item {
                CatalogRowSection(
                    title = "${activeProfile.name}'s Watchlist",
                    subtitle = "Saved in your library",
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
    val bg = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(460.dp)
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
                    Brush.horizontalGradient(
                        colors = listOf(
                            bg,
                            bg.copy(alpha = 0.80f),
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
                            bg
                        )
                    )
                )
        )

        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 28.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroSpecChip(item.type.uppercase(), MaterialTheme.colorScheme.primary)
                HeroSpecChip(item.sourceOrigin, NaphColors.CyanGlow)
                item.imdbRating?.let { HeroSpecChip("★ IMDb $it", NaphColors.GoldRating) }
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

            if (!item.description.isNullOrBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.90f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.65f)
                )
                Spacer(Modifier.height(20.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onPlayNow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
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
                        Text("In-App Trailer", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onToggleWatchlist,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isInWatchlist) NaphColors.EmeraldSeed else Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isInWatchlist) "In Library" else "+ Library",
                            color = Color.White
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(totalSlides) { idx ->
                        val isSelected = idx == activeIndex
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isSelected) 28.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
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
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
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
            items(items, key = { "${it.type}:${it.id}" }) { item ->
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
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = NaphColors.TextSecondary
        )
    }
}
