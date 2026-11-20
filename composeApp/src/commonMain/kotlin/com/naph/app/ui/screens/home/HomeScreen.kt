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
import com.naph.app.core.model.LayoutCustomizationConfig
import com.naph.app.core.model.MediaItem
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
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )

                Text(
                    text = "NAPH starts clean with zero pre-installed addons. Add your favorite community addons in Profile → Content & Discovery → Installed Addons to populate your Home screen.",
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
        verticalArrangement = Arrangement.spacedBy(26.dp),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (layoutConfig.showHeroShowcase && heroItems.isNotEmpty()) {
            val activeHero = heroItems[heroIndex % heroItems.size]
            item {
                HeroShowcaseStage(
                    item = activeHero,
                    totalSlides = heroItems.size,
                    activeIndex = heroIndex % heroItems.size,
                    layoutConfig = layoutConfig,
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

        if (layoutConfig.showContinueWatchingRow && continueWatching.isNotEmpty()) {
            item {
                Column {
                    SectionHeader(title = "Continue Watching")
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

        if (personalItems.isNotEmpty()) {
            item {
                CatalogRowSection(
                    title = "Personal Media Server",
                    items = personalItems,
                    layoutConfig = layoutConfig,
                    onItemClick = { appState.openMediaDetail(it) }
                )
            }
        }

        // Dynamic Catalog Rows from User-Installed Addons (Clean Netflix Section Headers!)
        items(homeCatalogSections, key = { it.key }) { section ->
            CatalogRowSection(
                title = section.title,
                items = section.items,
                layoutConfig = layoutConfig,
                onItemClick = { appState.openMediaDetail(it) }
            )
        }

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
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "To see movie, series, or anime catalog rows on the Home screen, install a catalog addon in Profile → Content & Discovery → Installed Addons (e.g. Cinemeta, CyberFlix, or TMDB).",
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
                            Text("Manage Addons", color = Color.White)
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

        if (profileWatchlist.isNotEmpty()) {
            item {
                CatalogRowSection(
                    title = "My List",
                    items = profileWatchlist,
                    layoutConfig = layoutConfig,
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
    layoutConfig: LayoutCustomizationConfig,
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
            .height(450.dp)
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
                            bg.copy(alpha = 0.78f),
                            Color.Transparent
                        ),
                        endX = 1050f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x44070709),
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
                if (layoutConfig.showImdbRating) {
                    item.imdbRating?.let { HeroSpecChip("★ IMDb $it", NaphColors.GoldRating) }
                }
                item.releaseInfo?.let { HeroSpecChip(it, NaphColors.TextSecondary) }
                item.genres.take(2).forEach { g -> HeroSpecChip(g, NaphColors.TextSecondary) }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            if (!item.description.isNullOrBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.90f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.62f)
                )
                Spacer(Modifier.height(18.dp))
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
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Play", fontWeight = FontWeight.ExtraBold)
                    }

                    if (item.trailers.isNotEmpty() || item.trailerStreams.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onWatchTrailer,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = NaphColors.CyanGlow)
                            Spacer(Modifier.width(8.dp))
                            Text("Trailer", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedButton(
                        onClick = onToggleWatchlist,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isInWatchlist) NaphColors.EmeraldSeed else Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isInWatchlist) "My List" else "+ My List",
                            color = Color.White
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(totalSlides) { idx ->
                        val isSelected = idx == activeIndex
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(if (isSelected) 24.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White
                                    else Color.White.copy(alpha = 0.35f)
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
            .background(Color(0xB30E1017))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
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
    items: List<MediaItem>,
    layoutConfig: LayoutCustomizationConfig,
    onItemClick: (MediaItem) -> Unit
) {
    Column {
        SectionHeader(title = title)
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items, key = { "${it.type}:${it.id}" }) { item ->
                CinemaPosterCard(
                    item = item,
                    layoutConfig = layoutConfig,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
        modifier = Modifier.padding(horizontal = 28.dp)
    )
}
