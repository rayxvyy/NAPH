package com.naph.app

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    // Left Navigation Sidebar: Home, Search, Library, Profile
                    if (isDesktopOrWide) {
                        DesktopNavigationRail(
                            currentTab = currentTab,
                            activeProfileName = activeProfile.name,
                            activeProfileEmoji = activeProfile.avatarEmoji,
                            activeProfileColor = Color(activeProfile.avatarColorHex),
                            sidebarExpanded = layoutConfig.sidebarAlwaysExpanded,
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

@Composable
private fun DesktopNavigationRail(
    currentTab: NaphTab,
    activeProfileName: String,
    activeProfileEmoji: String,
    activeProfileColor: Color,
    sidebarExpanded: Boolean,
    hasActiveP2P: Boolean,
    onSelectTab: (NaphTab) -> Unit
) {
    val railWidth = if (sidebarExpanded) 216.dp else 76.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .width(railWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline)
            .padding(vertical = 22.dp, horizontal = 12.dp)
    ) {
        // Top Brand Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "N",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            if (sidebarExpanded) {
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "NAPH",
                        style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 1.8.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "MEDIA CENTER",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Centered 4 Primary Tabs: Home, Search, Library, Profile (Exactly like Nuvio!)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            NaphTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 14.dp, vertical = 13.dp)
                ) {
                    if (tab == NaphTab.PROFILE) {
                        // Show active profile avatar badge on Profile tab
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(activeProfileColor.copy(alpha = 0.30f))
                        ) {
                            Text(activeProfileEmoji, fontSize = 13.sp)
                        }
                    } else {
                        Icon(
                            imageVector = iconForTab(tab),
                            contentDescription = tab.label,
                            tint = if (isSelected) Color.White else NaphColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (sidebarExpanded) {
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) Color.White else NaphColors.TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (tab == NaphTab.PROFILE) {
                                Text(
                                    text = activeProfileName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom P2P Swarm Status Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (hasActiveP2P) NaphColors.CyanGlow else NaphColors.EmeraldSeed)
            )
            if (sidebarExpanded) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (hasActiveP2P) "P2P Swarm Active" else "P2P Engine Ready",
                    style = MaterialTheme.typography.labelSmall,
                    color = NaphColors.TextSecondary
                )
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
