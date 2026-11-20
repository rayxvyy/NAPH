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
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import com.naph.app.ui.screens.settings.AddonsAndP2PScreen
import com.naph.app.ui.screens.settings.CloudDebridAndTrackersSettingsScreen
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

    NaphTheme {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(NaphColors.ObsidianBg)
        ) {
            val isDesktopOrWide = maxWidth >= 720.dp

            // If Video Player is Active -> Fullscreen Unified Video Player
            if (activePlaybackItem != null) {
                UnifiedVideoPlayerScreen(appState = appState)
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Desktop / Tablet Adaptive Left Cinema Navigation Rail
                    if (isDesktopOrWide) {
                        DesktopNavigationRail(
                            currentTab = currentTab,
                            activeProfileName = activeProfile.name,
                            activeProfileEmoji = activeProfile.avatarEmoji,
                            activeProfileColor = Color(activeProfile.avatarColorHex),
                            hasActiveP2P = p2pTelemetry != null,
                            onSelectTab = { appState.selectTab(it) },
                            onOpenProfilePicker = { appState.toggleProfilePicker(true) }
                        )
                    }

                    // Main Content Viewport
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
                                    NaphTab.ADDONS -> AddonsAndP2PScreen(appState = appState)
                                    NaphTab.SETTINGS -> CloudDebridAndTrackersSettingsScreen(appState = appState)
                                }
                            }
                        }

                        // Mobile Floating Bottom Navigation Bar (iOS & Android Portrait)
                        if (!isDesktopOrWide && selectedMediaItem == null) {
                            NavigationBar(
                                containerColor = NaphColors.DeepSurface.copy(alpha = 0.96f),
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

            // Netflix / Apple TV Style "Who's Watching?" Multi-Profile Overlay
            if (showProfilePicker) {
                ProfilePickerOverlay(appState = appState)
            }

            // Floating Status Toast Banner
            bannerMessage?.let { msg ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NaphColors.CardSurface)
                        .border(1.dp, NaphColors.CyanGlow, RoundedCornerShape(12.dp))
                        .clickable { appState.dismissBanner() }
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = NaphColors.CyanGlow)
                    Text(msg, style = MaterialTheme.typography.bodyMedium, color = NaphColors.TextPrimary)
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
    hasActiveP2P: Boolean,
    onSelectTab: (NaphTab) -> Unit,
    onOpenProfilePicker: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(NaphColors.DeepSurface)
            .border(width = 1.dp, color = NaphColors.ElevatedBorder)
            .padding(vertical = 22.dp, horizontal = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // NAPH Brand Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NaphColors.ElectricPrimary)
                ) {
                    Text(
                        text = "N",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "NAPH",
                        style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 1.8.sp),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "P2P • 4K CINEMA",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = NaphColors.CyanGlow
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            NaphTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) NaphColors.ElectricPrimary.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) NaphColors.ElectricPrimary.copy(alpha = 0.6f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = iconForTab(tab),
                        contentDescription = tab.label,
                        tint = if (isSelected) NaphColors.CyanGlow else NaphColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = if (isSelected) NaphColors.TextPrimary else NaphColors.TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Bottom Active Profile Switcher & P2P Engine Status Indicator
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NaphColors.CardSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (hasActiveP2P) NaphColors.CyanGlow else NaphColors.EmeraldSeed)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (hasActiveP2P) "P2P Swarm Active" else "P2P Engine Standby",
                    style = MaterialTheme.typography.labelSmall,
                    color = NaphColors.TextSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NaphColors.CardSurface)
                    .border(1.dp, NaphColors.ElevatedBorder, RoundedCornerShape(12.dp))
                    .clickable { onOpenProfilePicker() }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(activeProfileColor.copy(alpha = 0.25f))
                ) {
                    Text(activeProfileEmoji, fontSize = 18.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeProfileName,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                        maxLines = 1
                    )
                    Text(
                        text = "Switch Profile",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = NaphColors.CyanGlow
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
    NaphTab.ADDONS -> Icons.Default.Extension
    NaphTab.SETTINGS -> Icons.Default.Settings
}
