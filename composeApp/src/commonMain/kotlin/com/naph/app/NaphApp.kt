package com.naph.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.naph.app.core.model.SidebarBehaviorMode
import com.naph.app.ui.components.LiquidGlassFloatingNavBar
import com.naph.app.ui.components.NaphBrandIcon
import com.naph.app.ui.components.liquidGlassBorderBrush
import com.naph.app.ui.screens.details.MediaDetailScreen
import com.naph.app.ui.screens.home.HomeScreen
import com.naph.app.ui.screens.library.LibraryAndServersScreen
import com.naph.app.ui.screens.player.UnifiedVideoPlayerScreen
import com.naph.app.ui.screens.profiles.ProfilePickerOverlay
import com.naph.app.ui.screens.search.SearchAndExploreScreen
import com.naph.app.ui.screens.settings.ProfileAndSettingsScreen
import com.naph.app.ui.state.NaphAppState
import com.naph.app.ui.state.NaphTab
import com.naph.app.ui.theme.NaphColors
import com.naph.app.ui.theme.NaphTheme

@Composable
fun NaphApp() {
    // Configure High-Performance In-Memory Image Cache so scrolling posters is 60-120 FPS instant
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .crossfade(false)
            .build()
    }

    val appState = remember { NaphAppState() }

    val currentTab by appState.currentTab.collectAsState()
    val selectedMediaItem by appState.selectedMediaItem.collectAsState()
    val activePlaybackItem by appState.activePlaybackItem.collectAsState()
    val showProfilePicker by appState.showProfilePicker.collectAsState()
    val activeProfile by appState.activeProfile.collectAsState()
    val p2pTelemetry by appState.p2pTelemetry.collectAsState()
    val bannerMessage by appState.statusBannerMessage.collectAsState()
    val selectedThemePreset by appState.selectedThemePreset.collectAsState()
    val amoledBlack by appState.amoledBlackEnabled.collectAsState()
    val layoutConfig by appState.layoutConfig.collectAsState()

    NaphTheme(
        preset = selectedThemePreset,
        amoledBlack = amoledBlack
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isDesktopOrWide = maxWidth >= 720.dp

            if (activePlaybackItem != null) {
                UnifiedVideoPlayerScreen(appState = appState)
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Apple TV Style Compact Auto-Hover Left Sidebar (Home, Search, Library, Profile)
                    if (isDesktopOrWide) {
                        AppleTvHoverSidebar(
                            currentTab = currentTab,
                            activeProfileName = activeProfile.name,
                            activeProfileEmoji = activeProfile.avatarEmoji,
                            activeProfileColor = Color(activeProfile.avatarColorHex),
                            sidebarMode = layoutConfig.sidebarMode,
                            hasActiveP2P = p2pTelemetry != null,
                            onSelectTab = { appState.selectTab(it) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (selectedMediaItem != null) {
                            MediaDetailScreen(
                                item = selectedMediaItem!!,
                                appState = appState
                            )
                        } else {
                            // Instantaneous zero-lag tab switching
                            when (currentTab) {
                                NaphTab.HOME -> HomeScreen(appState = appState)
                                NaphTab.SEARCH -> SearchAndExploreScreen(appState = appState)
                                NaphTab.LIBRARY -> LibraryAndServersScreen(appState = appState)
                                NaphTab.PROFILE -> ProfileAndSettingsScreen(appState = appState)
                            }
                        }

                        if (!isDesktopOrWide && selectedMediaItem == null) {
                            LiquidGlassFloatingNavBar(
                                items = NaphTab.entries,
                                selectedItem = currentTab,
                                labelOf = { it.label },
                                iconOf = { iconForTab(it) },
                                onSelect = { appState.selectTab(it) },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }

            if (showProfilePicker) {
                ProfilePickerOverlay(appState = appState)
            }

            bannerMessage?.let { msg ->
                val bannerShape = RoundedCornerShape(14.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .align(if (isDesktopOrWide) Alignment.BottomEnd else Alignment.TopCenter)
                        .padding(
                            top = if (isDesktopOrWide) 0.dp else 56.dp,
                            bottom = if (isDesktopOrWide) 20.dp else 0.dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                        .clip(bannerShape)
                        .background(Color(0xEB151A28))
                        .border(1.dp, liquidGlassBorderBrush(MaterialTheme.colorScheme.primary), bannerShape)
                        .clickable { appState.dismissBanner() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(msg, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp), color = Color.White)
                }
            }
        }
    }
}

/**
 * Apple TV Style Hover-Expandable Left Sidebar:
 * - Collapsed width: 64.dp (sleek compact icons, giving maximum width to movie posters & hero banner)
 * - Hovered / Expanded width: 164.dp (just wide enough for "Home", "Search", "Library", and "Profile" to read clearly!)
 */
@Composable
private fun AppleTvHoverSidebar(
    currentTab: NaphTab,
    activeProfileName: String,
    activeProfileEmoji: String,
    activeProfileColor: Color,
    sidebarMode: SidebarBehaviorMode,
    hasActiveP2P: Boolean,
    onSelectTab: (NaphTab) -> Unit
) {
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val isHovered by hoverInteractionSource.collectIsHoveredAsState()

    val isExpanded = when (sidebarMode) {
        SidebarBehaviorMode.AUTO_EXPAND_HOVER -> isHovered
        SidebarBehaviorMode.ALWAYS_COMPACT -> false
        SidebarBehaviorMode.ALWAYS_EXPANDED -> true
    }

    val animatedWidth by animateDpAsState(
        targetValue = if (isExpanded) 164.dp else 64.dp,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "sidebar_width"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .width(animatedWidth)
            .fillMaxHeight()
            .hoverable(hoverInteractionSource)
            .background(Color(0xFF0A0C12))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.06f))
            .padding(vertical = 18.dp, horizontal = 8.dp)
    ) {
        // Top Brand Icon (Custom NAPH Prism App Icon -> "NAPH" on hover)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = if (isExpanded) 6.dp else 0.dp)
        ) {
            NaphBrandIcon(size = 34.dp)

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(120)) + expandHorizontally(tween(160), expandFrom = Alignment.Start),
                exit = shrinkHorizontally(tween(140), shrinkTowards = Alignment.Start) + fadeOut(tween(90))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "NAPH",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            letterSpacing = 1.5.sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Centered 4 Primary Tabs: Home, Search, Library, Profile
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            NaphTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = if (isExpanded) 10.dp else 0.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(28.dp)
                    ) {
                        if (tab == NaphTab.PROFILE) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(activeProfileColor.copy(alpha = 0.28f))
                            ) {
                                Text(activeProfileEmoji, fontSize = 13.sp)
                            }
                        } else {
                            Icon(
                                imageVector = iconForTab(tab),
                                contentDescription = tab.label,
                                tint = if (isSelected) Color.White else NaphColors.TextSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn(tween(120)) + expandHorizontally(tween(160), expandFrom = Alignment.Start),
                        exit = shrinkHorizontally(tween(140), shrinkTowards = Alignment.Start) + fadeOut(tween(90))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (tab == NaphTab.PROFILE) activeProfileName.take(12) else tab.label,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                                color = if (isSelected) Color.White else NaphColors.TextSecondary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }
        }

        // Bottom P2P Engine Dot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(horizontal = if (isExpanded) 10.dp else 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (hasActiveP2P) NaphColors.CyanGlow else NaphColors.EmeraldSeed)
            )
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(120)) + expandHorizontally(tween(160), expandFrom = Alignment.Start),
                exit = shrinkHorizontally(tween(140), shrinkTowards = Alignment.Start) + fadeOut(tween(90))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (hasActiveP2P) "P2P Active" else "P2P Ready",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = NaphColors.TextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

private fun iconForTab(tab: NaphTab): ImageVector = when (tab) {
    NaphTab.HOME -> Icons.Default.Home
    NaphTab.SEARCH -> Icons.Default.Search
    NaphTab.LIBRARY -> Icons.Default.VideoLibrary
    NaphTab.PROFILE -> Icons.Default.Person
}
