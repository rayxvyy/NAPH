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
import androidx.compose.material3.CircularProgressIndicator
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
import com.naph.app.core.model.LayoutCustomizationConfig
import com.naph.app.core.model.MediaItem
import com.naph.app.ui.components.CinemaPosterCard
import com.naph.app.ui.components.ContinueWatchingCard
import com.naph.app.ui.components.FastNetworkImage
import com.naph.app.ui.components.ModernGlassButton
import com.naph.app.ui.components.ModernPrimaryButton
import com.naph.app.ui.components.NaphBrandIcon
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .width(520.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF10131C))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                    .padding(32.dp)
            ) {
                NaphBrandIcon(size = 54.dp)

                Text(
                    text = "Welcome to NAPH",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "NAPH starts clean with zero pre-installed addons. Add your favorite community addons in Profile → Content & Discovery → Installed Addons to populate your Home screen.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    color = NaphColors.TextSecondary
                )

                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModernPrimaryButton(
                        text = "Add Your First Addon",
                        icon = Icons.Default.Add,
                        onClick = {
                            appState.navigateSettingsPage(ProfileSettingsPage.ADDONS_MANAGER)
                        }
                    )

                    ModernGlassButton(
                        text = "Connect Jellyfin / Emby",
                        icon = Icons.Default.Dns,
                        iconTint = NaphColors.CyanGlow,
                        onClick = {
                            appState.navigateSettingsPage(ProfileSettingsPage.MEDIA_SERVERS)
                        }
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (layoutConfig.showHeroShowcase && heroItems.isNotEmpty()) {
            item(key = "hero_showcase", contentType = "hero") {
                IsolatedHeroShowcaseCarousel(
                    heroItems = heroItems,
                    layoutConfig = layoutConfig,
                    appState = appState
                )
            }
        }

        if (isLoadingCatalogs) {
            item(key = "loading_indicator", contentType = "status") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10131C))
                        .padding(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Loading catalogs from installed addons...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }

        if (layoutConfig.showContinueWatchingRow && continueWatching.isNotEmpty()) {
            item(key = "continue_watching", contentType = "continue_row") {
                Column {
                    SectionHeader(title = "Continue Watching")
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(continueWatching, key = { it.mediaId }, contentType = { "cw_card" }) { entry ->
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
            item(key = "personal_server_row", contentType = "catalog_row") {
                CatalogRowSection(
                    title = "Personal Media Server",
                    items = personalItems,
                    layoutConfig = layoutConfig,
                    onItemClick = { appState.openMediaDetail(it) }
                )
            }
        }

        // Dynamic Catalog Rows from User-Installed Addons (Virtualized & Reusable ContentType)
        items(
            items = homeCatalogSections,
            key = { it.key },
            contentType = { "catalog_row" }
        ) { section ->
            CatalogRowSection(
                title = section.title,
                items = section.items,
                layoutConfig = layoutConfig,
                onItemClick = { appState.openMediaDetail(it) }
            )
        }

        if (!isLoadingCatalogs && installedAddons.isNotEmpty() && homeCatalogSections.isEmpty() && personalItems.isEmpty()) {
            item(key = "stream_only_notice", contentType = "notice") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF10131C))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Your installed addon (${installedAddons.joinToString { it.name }}) provides streams/subtitles only",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Install a catalog addon in Profile → Content & Discovery → Installed Addons (e.g. Cinemeta, CyberFlix, or TMDB) to populate movie and series rows.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                        color = NaphColors.TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModernPrimaryButton(
                            text = "Manage Addons",
                            icon = Icons.Default.Extension,
                            onClick = { appState.navigateSettingsPage(ProfileSettingsPage.ADDONS_MANAGER) }
                        )
                        ModernGlassButton(
                            text = "Refresh Catalogs",
                            icon = Icons.Default.Refresh,
                            onClick = { appState.refreshLiveCatalogs() }
                        )
                    }
                }
            }
        }

        if (profileWatchlist.isNotEmpty()) {
            item(key = "my_list_row", contentType = "catalog_row") {
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
private fun IsolatedHeroShowcaseCarousel(
    heroItems: List<MediaItem>,
    layoutConfig: LayoutCustomizationConfig,
    appState: NaphAppState
) {
    var heroIndex by remember { mutableStateOf(0) }

    LaunchedEffect(heroItems.size) {
        if (heroItems.size > 1) {
            while (true) {
                delay(7500)
                heroIndex = (heroIndex + 1) % heroItems.size
            }
        }
    }

    val activeHero = heroItems[heroIndex % heroItems.size]
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
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroSpecChip(item.type.uppercase(), MaterialTheme.colorScheme.primary)
                if (layoutConfig.showImdbRating) {
                    item.imdbRating?.let { HeroSpecChip("★ IMDb $it", NaphColors.GoldRating) }
                }
                item.releaseInfo?.let { HeroSpecChip(it, NaphColors.TextSecondary) }
                item.genres.take(2).forEach { g -> HeroSpecChip(g, NaphColors.TextSecondary) }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            if (!item.description.isNullOrBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.58f)
                )
                Spacer(Modifier.height(14.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModernPrimaryButton(
                        text = "Play",
                        icon = Icons.Default.PlayArrow,
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        onClick = onPlayNow
                    )

                    if (item.trailers.isNotEmpty() || item.trailerStreams.isNotEmpty()) {
                        ModernGlassButton(
                            text = "Trailer",
                            icon = Icons.Default.Movie,
                            iconTint = NaphColors.CyanGlow,
                            onClick = onWatchTrailer
                        )
                    }

                    ModernGlassButton(
                        text = if (isInWatchlist) "In List" else "+ My List",
                        icon = if (isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                        iconTint = if (isInWatchlist) NaphColors.EmeraldSeed else Color.White,
                        isActive = isInWatchlist,
                        onClick = onToggleWatchlist
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(totalSlides) { idx ->
                        val isSelected = idx == activeIndex
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isSelected) 20.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White
                                    else Color.White.copy(alpha = 0.30f)
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
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xB30E1017))
            .border(1.dp, color.copy(alpha = 0.40f), RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
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
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = items,
                key = { "${it.type}:${it.id}" },
                contentType = { "poster_card" }
            ) { item ->
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
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        ),
        color = Color.White,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}
