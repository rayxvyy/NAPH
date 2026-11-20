package com.naph.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import com.naph.app.core.model.SidebarBehaviorMode
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
                            Crossfade(targetState = currentTab) { tab ->
                                when (tab) {
                                    NaphTab.HOME -> HomeScreen(appState = appState)
                                    NaphTab.SEARCH -> SearchAndExploreScreen(appState = appState)
                                    NaphTab.LIBRARY -> LibraryAndServersScreen(appState = appState)
                                    NaphTab.PROFILE -> ProfileAndSettingsScreen(appState = appState)
                                }
                            }
                        }

                        if (!isDesktopOrWide && selectedMediaItem == null) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                modifier = Modifier.align(Alignment.BottomCenter)
                            ) {
                                NaphTab.entries.forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentTab == tab,
                                        onClick = { appState.selectTab(tab) },
                                        icon = { Icon(iconForTab(tab), contentDescription = tab.label) },
                                        label = { Text(tab.label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showProfilePicker) {
                ProfilePickerOverlay(appState = appState)
            }

            bannerMessage?.let { msg ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .clickable { appState.dismissBanner() }
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(msg, style = MaterialTheme.typography.bodyMedium, color = Color.White)
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
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "sidebar_width"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .width(animatedWidth)
            .fillMaxHeight()
            .hoverable(hoverInteractionSource)
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.08f))
            .padding(vertical = 20.dp, horizontal = 8.dp)
    ) {
        // Top Brand Icon ("N" -> "NAPH" on hover)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = if (isExpanded) 8.dp else 0.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "N",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(150)) + expandHorizontally(tween(200), expandFrom = Alignment.Start),
                exit = shrinkHorizontally(tween(180), shrinkTowards = Alignment.Start) + fadeOut(tween(100))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "NAPH",
                        style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 1.5.sp),
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = if (isExpanded) 10.dp else 0.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (tab == NaphTab.PROFILE) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(activeProfileColor.copy(alpha = 0.30f))
                            ) {
                                Text(activeProfileEmoji, fontSize = 14.sp)
                            }
                        } else {
                            Icon(
                                imageVector = iconForTab(tab),
                                contentDescription = tab.label,
                                tint = if (isSelected) Color.White else NaphColors.TextSecondary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn(tween(150)) + expandHorizontally(tween(200), expandFrom = Alignment.Start),
                        exit = shrinkHorizontally(tween(180), shrinkTowards = Alignment.Start) + fadeOut(tween(100))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (tab == NaphTab.PROFILE) activeProfileName.take(12) else tab.label,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                color = if (isSelected) Color.White else NaphColors.TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = if (isExpanded) 10.dp else 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (hasActiveP2P) NaphColors.CyanGlow else NaphColors.EmeraldSeed)
            )
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(150)) + expandHorizontally(tween(200), expandFrom = Alignment.Start),
                exit = shrinkHorizontally(tween(180), shrinkTowards = Alignment.Start) + fadeOut(tween(100))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (hasActiveP2P) "P2P Active" else "P2P Ready",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
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
