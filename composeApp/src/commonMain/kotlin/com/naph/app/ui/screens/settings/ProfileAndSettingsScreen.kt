package com.naph.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naph.app.core.model.PosterCardSizePreset
import com.naph.app.core.model.ServerType
import com.naph.app.core.model.SidebarBehaviorMode
import com.naph.app.ui.state.NaphAppState
import com.naph.app.ui.state.ProfileSettingsPage
import com.naph.app.ui.theme.NaphColors
import com.naph.app.ui.theme.NaphThemePreset
import com.naph.app.ui.theme.naphTextFieldColors

/**
 * NAPH Structured Profile & Settings Hub (Modeled after Nuvio's SettingsRootPage & Drill-Down Pages)
 *
 * When you tap "Profile" on the left navigation bar, you see clean grouped sections:
 * 1. ACCOUNT: Switch Profile, Account (Built-in NAPH Cloud Sync), Watch Party, Tracking (Trakt.tv & SIMKL)
 * 2. GENERAL: Appearance & Theme, Layout Customization, Content & Discovery (Addons & Media Servers),
 *             Playback & P2P Engine, Integrations (Debrid, TMDB, MDBList), Notifications
 * 3. ABOUT:   About NAPH, Version, Licenses & Credits
 *
 * Clicking any row drills into its dedicated page with a back button — never dumping everything onto the main screen!
 */
@Composable
fun ProfileAndSettingsScreen(
    appState: NaphAppState,
    modifier: Modifier = Modifier
) {
    val currentPage by appState.currentSettingsPage.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sub-page top bar with Back button when drilled into a specific section
        if (currentPage != ProfileSettingsPage.ROOT) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                IconButton(
                    onClick = { appState.navigateSettingsBack() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = currentPage.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        // Render Either Root Menu or Dedicated Sub-Page
        when (currentPage) {
            ProfileSettingsPage.ROOT -> ProfileRootSectionsList(appState)
            ProfileSettingsPage.ACCOUNT_DETAIL -> AccountDetailSubPage(appState)
            ProfileSettingsPage.WATCH_PARTY -> WatchPartySubPage(appState)
            ProfileSettingsPage.TRACKING -> TrackingSubPage(appState)
            ProfileSettingsPage.APPEARANCE -> AppearanceSubPage(appState)
            ProfileSettingsPage.LAYOUT_CUSTOMIZATION -> LayoutCustomizationSubPage(appState)
            ProfileSettingsPage.CONTENT_DISCOVERY -> ContentDiscoverySubPage(appState)
            ProfileSettingsPage.ADDONS_MANAGER -> AddonsManagerSubPage(appState)
            ProfileSettingsPage.MEDIA_SERVERS -> MediaServersSubPage(appState)
            ProfileSettingsPage.PLAYBACK -> PlaybackAndP2PSubPage(appState)
            ProfileSettingsPage.INTEGRATIONS -> IntegrationsSubPage(appState)
            ProfileSettingsPage.NOTIFICATIONS -> NotificationsSubPage(appState)
            ProfileSettingsPage.ABOUT -> AboutSubPage()
        }
    }
}

@Composable
private fun ProfileRootSectionsList(appState: NaphAppState) {
    val activeProfile by appState.activeProfile.collectAsState()
    val isSignedIn by appState.syncRepo.isSignedIn.collectAsState()
    val accountEmail by appState.syncRepo.accountEmail.collectAsState()
    val installedAddons by appState.installedAddons.collectAsState()
    val servers by appState.mediaServers.collectAsState()
    val selectedTheme by appState.selectedThemePreset.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Active Profile Header Banner
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                    .padding(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(activeProfile.avatarColorHex).copy(alpha = 0.25f))
                            .border(2.dp, Color(activeProfile.avatarColorHex), RoundedCornerShape(18.dp))
                    ) {
                        Text(activeProfile.avatarEmoji, fontSize = 30.sp)
                    }
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text(
                            text = activeProfile.name,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Text(
                            text = if (isSignedIn) "NAPH Cloud Sync Active (${accountEmail ?: "Synced"})"
                            else "Local Profile • Sign in to sync across devices",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NaphColors.EmeraldSeed
                        )
                    }
                }

                OutlinedButton(
                    onClick = { appState.toggleProfilePicker(true) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.People, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Switch Profile", color = Color.White)
                }
            }
        }

        // SECTION 1: ACCOUNT
        item {
            SettingsSectionCard(sectionTitle = "ACCOUNT") {
                SettingsRowItem(
                    icon = Icons.Default.People,
                    title = "Switch Profile",
                    subtitle = "Active: ${activeProfile.name} ${activeProfile.avatarEmoji} • Manage household profiles",
                    onClick = { appState.toggleProfilePicker(true) }
                )
                SettingsDivider()
                SettingsRowItem(
                    icon = Icons.Default.Person,
                    title = "Account & Cloud Sync",
                    subtitle = if (isSignedIn) "Signed in as ${accountEmail ?: "NAPH User"} • Built-in cross-platform sync active"
                    else "Sign in to sync your library, addons, and progress across Windows, Linux, iOS & Android",
                    onClick = { appState.navigateSettingsPage(ProfileSettingsPage.ACCOUNT_DETAIL) }
                )
                SettingsDivider()
                SettingsRowItem(
                    icon = Icons.Default.Groups,
                    title = "Watch Party",
                    subtitle = "Create or join a synchronized viewing room with friends",
                    onClick = { appState.navigateSettingsPage(ProfileSettingsPage.WATCH_PARTY) }
                )
                SettingsDivider()
                SettingsRowItem(
                    icon = Icons.Default.Sync,
                    title = "Tracking",
                    subtitle = "Connect Trakt.tv and SIMKL for automatic watch history & scrobbling",
                    onClick = { appState.navigateSettingsPage(ProfileSettingsPage.TRACKING) }
                )
            }
        }

        // SECTION 2: GENERAL
        item {
            SettingsSectionCard(sectionTitle = "GENERAL") {
                SettingsRowItem(
                    icon = Icons.Default.Palette,
                    title = "Appearance",
                    subtitle = "Theme preset (${selectedTheme.label}), high-contrast text & AMOLED black mode",
                    onClick = { appState.navigateSettingsPage(ProfileSettingsPage.APPEARANCE) }
                )
                SettingsDivider()
                SettingsRowItem(
                    icon = Icons.Default.GridView,
                    title = "Layout Customization",
                    subtitle = "Customize Home screen hero banner, catalog titles, detail screen & poster cards",
                    onClick = { appState.navigateSettingsPage(ProfileSettingsPage.LAYOUT_CUSTOMIZATION) }
                )
                SettingsDivider()
                SettingsRowItem(
                    icon = Icons.Default.Explore,
                    title = "Content & Discovery",
                    subtitle = "${installedAddons.size} installed addons • ${servers.size} personal media servers (Jellyfin / Emby)",
                    onClick = { appState.navigateSettingsPage(ProfileSettingsPage.CONTENT_DISCOVERY) }
                )
                SettingsDivider()
                SettingsRowItem(
                    icon = Icons.Default.PlayArrow,
                    title = "Playback & P2P Engine",
                    subtitle = "Video player engine, auto-skip intros, subtitle appearance & P2P temporary cache",
                    onClick = { appState.navigateSettingsPage(ProfileSettingsPage.PLAYBACK) }
                )
                SettingsDivider()
                SettingsRowItem(
                    icon = Icons.Default.Link,
                    title = "Integrations",
                    subtitle = "Debrid services (Real-Debrid, AllDebrid, TorBox, Premiumize), TMDB & MDBList ratings",
                    onClick = { appState.navigateSettingsPage(ProfileSettingsPage.INTEGRATIONS) }
                )
                SettingsDivider()
                SettingsRowItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "New episode release alerts and P2P stream notifications",
                    onClick = { appState.navigateSettingsPage(ProfileSettingsPage.NOTIFICATIONS) }
                )
            }
        }

        // SECTION 3: ABOUT
        item {
            SettingsSectionCard(sectionTitle = "ABOUT") {
                SettingsRowItem(
                    icon = Icons.Default.Info,
                    title = "About NAPH",
                    subtitle = "Version 1.0.0 • Open-Source Multiplatform Media Center (Windows, Linux, iOS, Android)",
                    onClick = { appState.navigateSettingsPage(ProfileSettingsPage.ABOUT) }
                )
            }
        }
    }
}

// ============================================================================
// SUB-PAGES (Clean Drill-Down Views)
// ============================================================================

@Composable
private fun AccountDetailSubPage(appState: NaphAppState) {
    val isSignedIn by appState.syncRepo.isSignedIn.collectAsState()
    val email by appState.syncRepo.accountEmail.collectAsState()
    val syncStatus by appState.syncRepo.cloudSyncStatus.collectAsState()
    var emailInput by remember { mutableStateOf(email.orEmpty()) }
    var passwordInput by remember { mutableStateOf("") }

    SubPageContainer {
        SettingsSectionCard(sectionTitle = "BUILT-IN NAPH CLOUD SYNC") {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = if (isSignedIn) "Signed In & Syncing Across Devices" else "Sign In to NAPH Cloud",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = syncStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NaphColors.EmeraldSeed
                )
                Text(
                    text = "NAPH Cloud Sync is built directly into the app—no external server URLs, Docker containers, or manual API keys needed. Your profiles, installed addons, watch progress, and library sync automatically across Windows, Linux, iOS, and Android.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NaphColors.TextSecondary
                )

                if (isSignedIn) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { appState.syncRepo.signOutNaphAccount() },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Sign Out of NAPH Cloud", color = NaphColors.CrimsonPlay)
                    }
                } else {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        colors = naphTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = naphTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { appState.syncRepo.signInWithNaphAccount(emailInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Sign In & Sync Devices", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchPartySubPage(appState: NaphAppState) {
    val session by appState.watchPartySession.collectAsState()
    var roomCodeInput by remember { mutableStateOf("") }

    SubPageContainer {
        SettingsSectionCard(sectionTitle = "SYNCHRONIZED WATCH PARTY") {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                Text("Watch Together in Real Time", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Host or join a Watch Party room so play, pause, and seeking stay synchronized across everyone's screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NaphColors.TextSecondary
                )

                if (session != null) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, NaphColors.EmeraldSeed, RoundedCornerShape(14.dp))
                            .padding(18.dp)
                    ) {
                        Text("ACTIVE ROOM CODE: ${session!!.roomCode}", style = MaterialTheme.typography.titleLarge, color = NaphColors.EmeraldSeed)
                        Text("Host: ${session!!.hostName} • Participants: ${session!!.participantCount}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = { appState.leaveWatchParty() }) {
                            Text("Leave Watch Party", color = NaphColors.CrimsonPlay)
                        }
                    }
                } else {
                    Button(
                        onClick = { appState.createWatchParty() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create New Watch Party Room", color = Color.White)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = { roomCodeInput = it },
                            label = { Text("Enter Friend's Room Code (e.g. NAPH-4821)") },
                            singleLine = true,
                            colors = naphTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { appState.joinWatchParty(roomCodeInput) }) {
                            Text("Join Room", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingSubPage(appState: NaphAppState) {
    val trackerConfig by appState.trackerConfig.collectAsState()
    var traktToken by remember(trackerConfig) { mutableStateOf(trackerConfig.traktAccessToken) }
    var simklToken by remember(trackerConfig) { mutableStateOf(trackerConfig.simklAccessToken) }
    var autoScrobble by remember(trackerConfig) { mutableStateOf(trackerConfig.autoScrobbleEnabled) }

    SubPageContainer {
        SettingsSectionCard(sectionTitle = "2-WAY WATCH HISTORY & SCROBBLING") {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                SettingsToggleRow(
                    title = "Automatic Real-Time Scrobbling",
                    subtitle = "Automatically mark movies and episodes as watched when playback reaches 80%",
                    checked = autoScrobble,
                    onCheckedChange = { autoScrobble = it }
                )

                OutlinedTextField(
                    value = traktToken,
                    onValueChange = { traktToken = it },
                    label = { Text("Trakt.tv Account Token") },
                    placeholder = { Text("Paste Trakt OAuth token to sync Trakt watchlist & history") },
                    singleLine = true,
                    colors = naphTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = simklToken,
                    onValueChange = { simklToken = it },
                    label = { Text("SIMKL Account Token") },
                    placeholder = { Text("Paste SIMKL token for Anime & TV tracking") },
                    singleLine = true,
                    colors = naphTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        appState.updateTrackerConfig(
                            trackerConfig.copy(
                                traktAccessToken = traktToken,
                                simklAccessToken = simklToken,
                                autoScrobbleEnabled = autoScrobble
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Tracking Settings", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AppearanceSubPage(appState: NaphAppState) {
    val selectedPreset by appState.selectedThemePreset.collectAsState()
    val amoledBlack by appState.amoledBlackEnabled.collectAsState()

    SubPageContainer {
        SettingsSectionCard(sectionTitle = "COLOR THEME PRESETS (HIGH-CONTRAST READABILITY)") {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = "Choose Your Accent & Surface Palette",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "All presets use crisp pure-white primary text (#FFFFFF) and high-contrast borders so every input field and label is crystal clear.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NaphColors.TextSecondary
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NaphThemePreset.entries.forEach { preset ->
                        val isSelected = preset == selectedPreset
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) preset.accent else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { appState.setThemePreset(preset) }
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(preset.accent)
                                )
                                Spacer(Modifier.width(14.dp))
                                Text(preset.label, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = preset.accent)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SettingsToggleRow(
                    title = "Pure AMOLED Pitch-Black Background (#000000)",
                    subtitle = "Use true zero-light OLED black for the main app background",
                    checked = amoledBlack,
                    onCheckedChange = { appState.setAmoledBlack(it) }
                )
            }
        }
    }
}

@Composable
private fun LayoutCustomizationSubPage(appState: NaphAppState) {
    val config by appState.layoutConfig.collectAsState()

    SubPageContainer {
        // 1. SIDEBAR & NAVIGATION BEHAVIOR (Apple TV Auto-Hover vs Always Compact vs Always Expanded)
        SettingsSectionCard(sectionTitle = "LEFT SIDEBAR NAVIGATION STYLE (APPLE TV HOVER)") {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = "Choose how the left sidebar behaves on Desktop",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                SidebarBehaviorMode.entries.forEach { mode ->
                    val isSelected = config.sidebarMode == mode
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { appState.updateLayoutConfig(config.copy(sidebarMode = mode)) }
                            .padding(horizontal = 16.dp, vertical = 13.dp)
                    ) {
                        Text(mode.label, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // 2. NETFLIX POSTER CARD CUSTOMIZATION
        SettingsSectionCard(sectionTitle = "POSTER CARD STYLE (CLEAN NETFLIX LOOK)") {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                Text("Poster Card Width", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PosterCardSizePreset.entries.forEach { sizePreset ->
                        FilterChip(
                            selected = config.posterSizePreset == sizePreset,
                            onClick = { appState.updateLayoutConfig(config.copy(posterSizePreset = sizePreset)) },
                            label = { Text(sizePreset.label) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SettingsToggleRow(
                    title = "Show Title Text Below Poster Cards",
                    subtitle = "Off by default for a pure Netflix poster artwork grid",
                    checked = config.showPosterTitlesBelow,
                    onCheckedChange = { appState.updateLayoutConfig(config.copy(showPosterTitlesBelow = it)) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SettingsToggleRow(
                    title = "Show IMDb Score Badge on Poster Corner",
                    subtitle = "Off by default so posters remain 100% clean without text overlays",
                    checked = config.showRatingBadgeOnPoster,
                    onCheckedChange = { appState.updateLayoutConfig(config.copy(showRatingBadgeOnPoster = it)) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column {
                    Text(
                        text = "Poster Corner Roundness: ${config.posterCornerRadiusDp} dp",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Slider(
                        value = config.posterCornerRadiusDp.toFloat(),
                        onValueChange = { appState.updateLayoutConfig(config.copy(posterCornerRadiusDp = it.toInt())) },
                        valueRange = 4f..22f
                    )
                }
            }
        }

        // 3. OPTIONAL RATING PROVIDERS (User Chooses Which Ratings to Display!)
        SettingsSectionCard(sectionTitle = "RATING PROVIDERS ON MOVIE / SERIES DETAILS") {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                SettingsToggleRow(
                    title = "IMDb Rating",
                    subtitle = "Show official IMDb star score on detail pages",
                    checked = config.showImdbRating,
                    onCheckedChange = { appState.updateLayoutConfig(config.copy(showImdbRating = it)) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    title = "Rotten Tomatoes (Critics Tomatometer 🍅)",
                    subtitle = "Optional: Enable Rotten Tomatoes critics percentage",
                    checked = config.showRottenTomatoesCritics,
                    onCheckedChange = { appState.updateLayoutConfig(config.copy(showRottenTomatoesCritics = it)) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    title = "Rotten Tomatoes (Audience Popcornmeter 🍿)",
                    subtitle = "Optional: Enable Rotten Tomatoes audience score",
                    checked = config.showRottenTomatoesAudience,
                    onCheckedChange = { appState.updateLayoutConfig(config.copy(showRottenTomatoesAudience = it)) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    title = "Metacritic Score",
                    subtitle = "Optional: Enable Metacritic Metascore badge",
                    checked = config.showMetacriticRating,
                    onCheckedChange = { appState.updateLayoutConfig(config.copy(showMetacriticRating = it)) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    title = "Trakt.tv Community Score",
                    subtitle = "Optional: Enable Trakt percentage badge",
                    checked = config.showTraktRating,
                    onCheckedChange = { appState.updateLayoutConfig(config.copy(showTraktRating = it)) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    title = "MDBList Aggregated Score",
                    subtitle = "Optional: Enable MDBList combined score badge",
                    checked = config.showMdbListRating,
                    onCheckedChange = { appState.updateLayoutConfig(config.copy(showMdbListRating = it)) }
                )
            }
        }

        // 4. STREAM LIST & HOME SCREEN OPTIONS
        SettingsSectionCard(sectionTitle = "HOME SCREEN & STREAM LIST DETAILS") {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                SettingsToggleRow(
                    title = "Show Widescreen Hero Showcase Banner on Home",
                    subtitle = "Display featured movie & series artwork stage at the top of the Home feed",
                    checked = config.showHeroShowcase,
                    onCheckedChange = { appState.updateLayoutConfig(config.copy(showHeroShowcase = it)) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    title = "Show File Size (MB / GB) on Stream Cards",
                    subtitle = "Display exact stream file size badge (e.g. 4.2 GB, 18.4 GB)",
                    checked = config.showStreamFileSizeBadge,
                    onCheckedChange = { appState.updateLayoutConfig(config.copy(showStreamFileSizeBadge = it)) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    title = "Show Active P2P Seeders Count on Stream Cards",
                    subtitle = "Display BitTorrent seeder count badge on P2P streams",
                    checked = config.showStreamSeedersBadge,
                    onCheckedChange = { appState.updateLayoutConfig(config.copy(showStreamSeedersBadge = it)) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    title = "Show Video Codec & Audio Format Badges (HEVC, Atmos, BluRay)",
                    subtitle = "Display technical badges parsed from addon stream metadata",
                    checked = config.showStreamCodecAndAudioBadges,
                    onCheckedChange = { appState.updateLayoutConfig(config.copy(showStreamCodecAndAudioBadges = it)) }
                )
            }
        }
    }
}

@Composable
private fun ContentDiscoverySubPage(appState: NaphAppState) {
    val addons by appState.installedAddons.collectAsState()
    val servers by appState.mediaServers.collectAsState()

    SubPageContainer {
        SettingsSectionCard(sectionTitle = "CONTENT SOURCES") {
            SettingsRowItem(
                icon = Icons.Default.Extension,
                title = "Installed Addons (${addons.size})",
                subtitle = "Install Stremio-compatible JSON manifest addons for catalogs, streams & subtitles",
                onClick = { appState.navigateSettingsPage(ProfileSettingsPage.ADDONS_MANAGER) }
            )
            SettingsDivider()
            SettingsRowItem(
                icon = Icons.Default.Dns,
                title = "Media Servers (${servers.size})",
                subtitle = "Connect personal Jellyfin and Emby media servers",
                onClick = { appState.navigateSettingsPage(ProfileSettingsPage.MEDIA_SERVERS) }
            )
        }
    }
}

@Composable
private fun AddonsManagerSubPage(appState: NaphAppState) {
    val addons by appState.installedAddons.collectAsState()
    val isInstalling by appState.isInstallingAddon.collectAsState()
    var manifestUrlInput by remember { mutableStateOf("") }

    SubPageContainer {
        SettingsSectionCard(sectionTitle = "INSTALL STREMIO-COMPATIBLE ADDON") {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = "Add Addon via Manifest URL",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Paste any Stremio v3 manifest.json URL (https://... or stremio://...). Any catalog rows provided by the addon will immediately appear on your Home screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NaphColors.TextSecondary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = manifestUrlInput,
                        onValueChange = { manifestUrlInput = it },
                        label = { Text("Addon Manifest URL") },
                        placeholder = { Text("https://v3-cinemeta.strem.io/manifest.json") },
                        singleLine = true,
                        colors = naphTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            appState.installAddonByManifestUrl(manifestUrlInput)
                            manifestUrlInput = ""
                        },
                        enabled = !isInstalling && manifestUrlInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        if (isInstalling) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Install", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = "Quick Test Community Addons (Optional — click only if you want to install them):",
                    style = MaterialTheme.typography.labelSmall,
                    color = NaphColors.TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        "Cinemeta Catalogs" to "https://v3-cinemeta.strem.io/manifest.json",
                        "CyberFlix Catalogs" to "https://cyberflix.elfhosted.com/manifest.json",
                        "Torrentio Streams" to "https://torrentio.strem.fun/manifest.json",
                        "OpenSubtitles v3" to "https://opensubtitles-v3.strem.io/manifest.json"
                    ).forEach { (label, url) ->
                        OutlinedButton(
                            onClick = { appState.installAddonByManifestUrl(url) },
                            enabled = !isInstalling
                        ) {
                            Text("+ $label", color = NaphColors.CyanGlow, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        SettingsSectionCard(sectionTitle = "YOUR INSTALLED ADDONS (${addons.size})") {
            if (addons.isEmpty()) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text(
                        text = "No addons installed yet.",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "NAPH starts with zero pre-installed addons. Paste any manifest URL above to add your own catalogs, streams, and subtitles.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NaphColors.TextSecondary
                    )
                }
            } else {
                addons.forEachIndexed { idx, addon ->
                    if (idx > 0) SettingsDivider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(addon.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text("v${addon.version}", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
                                Text(
                                    text = "${addon.catalogs.size} Catalogs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaphColors.EmeraldSeed
                                )
                            }
                            if (addon.description.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(addon.description, style = MaterialTheme.typography.bodyMedium, color = NaphColors.TextSecondary)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(addon.manifestUrl, style = MaterialTheme.typography.labelSmall, color = NaphColors.TextMuted)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Switch(
                                checked = addon.isEnabled,
                                onCheckedChange = { appState.toggleAddonEnabled(addon.id, it) }
                            )
                            IconButton(onClick = { appState.removeAddon(addon.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = NaphColors.CrimsonPlay)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaServersSubPage(appState: NaphAppState) {
    val servers by appState.mediaServers.collectAsState()
    var serverType by remember { mutableStateOf(ServerType.JELLYFIN) }
    var serverName by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("https://") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    SubPageContainer {
        SettingsSectionCard(sectionTitle = "CONNECT JELLYFIN OR EMBY SERVER") {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = serverType == ServerType.JELLYFIN,
                        onClick = { serverType = ServerType.JELLYFIN },
                        label = { Text("Jellyfin") }
                    )
                    FilterChip(
                        selected = serverType == ServerType.EMBY,
                        onClick = { serverType = ServerType.EMBY },
                        label = { Text("Emby") }
                    )
                }

                OutlinedTextField(
                    value = serverName,
                    onValueChange = { serverName = it },
                    label = { Text("Server Label (e.g. Home NAS)") },
                    singleLine = true,
                    colors = naphTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Server URL (e.g. https://jellyfin.myhome.net)") },
                    singleLine = true,
                    colors = naphTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        colors = naphTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = naphTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = { appState.connectMediaServer(serverType, serverName, serverUrl, username, password) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Connect ${serverType.name} Server", color = Color.White, fontWeight = FontWeight.Bold)
                }

                if (servers.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    servers.forEach { srv ->
                        Text("• ${srv.name} (${srv.type.name}) — ${srv.serverUrl}", color = NaphColors.EmeraldSeed)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackAndP2PSubPage(appState: NaphAppState) {
    val p2pSettings by appState.p2pSettings.collectAsState()
    val p2pClearedBytes by appState.p2pCacheClearedBytes.collectAsState()
    val activeProfile by appState.activeProfile.collectAsState()

    SubPageContainer {
        SettingsSectionCard(sectionTitle = "SEQUENTIAL P2P TORRENT ENGINE & TEMPORARY CACHE") {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = "Auto-Purged Temporary Cache Session Total: ${(p2pClearedBytes / (1024 * 1024))} MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = NaphColors.CyanGlow
                )

                SettingsToggleRow(
                    title = "Temporary Ring-Buffer Cache (Auto-Delete After Playback)",
                    subtitle = "Streams torrents in a temporary cache folder and automatically deletes pieces when playback finishes so your device storage never fills up.",
                    checked = p2pSettings.useTempCacheAutoClean,
                    onCheckedChange = {
                        appState.updateP2PSettings(
                            p2pSettings.copy(
                                useTempCacheAutoClean = it,
                                keepDownloadedFiles = if (it) false else p2pSettings.keepDownloadedFiles
                            )
                        )
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SettingsToggleRow(
                    title = "Keep Downloaded Video Files on Disk (Optional Power-User Setting)",
                    subtitle = "Save completed torrent video files permanently to a custom folder instead of deleting temporary cache",
                    checked = p2pSettings.keepDownloadedFiles,
                    onCheckedChange = {
                        appState.updateP2PSettings(
                            p2pSettings.copy(
                                keepDownloadedFiles = it,
                                useTempCacheAutoClean = !it
                            )
                        )
                    }
                )

                if (p2pSettings.keepDownloadedFiles) {
                    OutlinedTextField(
                        value = p2pSettings.customSaveDirectory,
                        onValueChange = { appState.updateP2PSettings(p2pSettings.copy(customSaveDirectory = it)) },
                        label = { Text("Custom Download Folder Path") },
                        singleLine = true,
                        colors = naphTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column {
                    Text(
                        text = "Maximum Temporary Buffer Cache Size: ${p2pSettings.maxCacheSizeGb} GB",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Slider(
                        value = p2pSettings.maxCacheSizeGb.toFloat(),
                        onValueChange = { appState.updateP2PSettings(p2pSettings.copy(maxCacheSizeGb = it.toInt())) },
                        valueRange = 2f..24f
                    )
                }

                SettingsToggleRow(
                    title = "Distributed Hash Table (DHT) & Peer Exchange (PEX)",
                    subtitle = "Find peers directly without relying solely on trackers",
                    checked = p2pSettings.enableDht,
                    onCheckedChange = {
                        appState.updateP2PSettings(p2pSettings.copy(enableDht = it, enablePeerExchange = it))
                    }
                )

                OutlinedTextField(
                    value = p2pSettings.externalTorrServerUrl,
                    onValueChange = { appState.updateP2PSettings(p2pSettings.copy(externalTorrServerUrl = it)) },
                    label = { Text("Optional External TorrServer URL (Leave empty for built-in NAPH P2P Engine)") },
                    placeholder = { Text("http://127.0.0.1:8090") },
                    singleLine = true,
                    colors = naphTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SettingsSectionCard(sectionTitle = "PLAYER & SMART AUTO-SKIP (INTRODB / ANISKIP)") {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                SettingsToggleRow(
                    title = "Auto-Skip Intros, Recaps & Credits (IntroDB / AniSkip)",
                    subtitle = "Automatically skip opening intros and ending credits during playback",
                    checked = activeProfile.autoSkipIntros,
                    onCheckedChange = {
                        appState.switchProfile(activeProfile.copy(autoSkipIntros = it))
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    title = "Auto-Play Next Episode",
                    subtitle = "Automatically queue the next episode when an episode finishes",
                    checked = activeProfile.autoPlayNextEpisode,
                    onCheckedChange = {
                        appState.switchProfile(activeProfile.copy(autoPlayNextEpisode = it))
                    }
                )
            }
        }
    }
}

@Composable
private fun IntegrationsSubPage(appState: NaphAppState) {
    val debridConfig by appState.debridConfig.collectAsState()
    val trackerConfig by appState.trackerConfig.collectAsState()

    var rdKey by remember(debridConfig) { mutableStateOf(debridConfig.realDebridApiKey) }
    var adKey by remember(debridConfig) { mutableStateOf(debridConfig.allDebridApiKey) }
    var tbKey by remember(debridConfig) { mutableStateOf(debridConfig.torBoxApiKey) }
    var pmKey by remember(debridConfig) { mutableStateOf(debridConfig.premiumizeApiKey) }
    var preferDebrid by remember(debridConfig) { mutableStateOf(debridConfig.preferDebridOverP2P) }

    var mdbListKey by remember(trackerConfig) { mutableStateOf(trackerConfig.mdbListApiKey) }
    var tmdbKey by remember(trackerConfig) { mutableStateOf(trackerConfig.tmdbApiKey) }

    SubPageContainer {
        SettingsSectionCard(sectionTitle = "DEBRID SERVICES (REAL-DEBRID, ALLDEBRID, TORBOX, PREMIUMIZE)") {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                SettingsToggleRow(
                    title = "Prefer Cached Debrid Streams over Pure P2P",
                    subtitle = "When off, NAPH uses its native Sequential P2P Torrent Engine by default",
                    checked = preferDebrid,
                    onCheckedChange = { preferDebrid = it }
                )

                OutlinedTextField(
                    value = rdKey,
                    onValueChange = { rdKey = it },
                    label = { Text("Real-Debrid API Token") },
                    singleLine = true,
                    colors = naphTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tbKey,
                    onValueChange = { tbKey = it },
                    label = { Text("TorBox API Key") },
                    singleLine = true,
                    colors = naphTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = adKey,
                    onValueChange = { adKey = it },
                    label = { Text("AllDebrid API Key") },
                    singleLine = true,
                    colors = naphTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pmKey,
                    onValueChange = { pmKey = it },
                    label = { Text("Premiumize API Key") },
                    singleLine = true,
                    colors = naphTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        appState.updateDebridConfig(
                            debridConfig.copy(
                                realDebridApiKey = rdKey,
                                allDebridApiKey = adKey,
                                torBoxApiKey = tbKey,
                                premiumizeApiKey = pmKey,
                                preferDebridOverP2P = preferDebrid
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Debrid Configuration", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        SettingsSectionCard(sectionTitle = "METADATA & AGGREGATED RATINGS (MDBLIST & TMDB)") {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                OutlinedTextField(
                    value = mdbListKey,
                    onValueChange = { mdbListKey = it },
                    label = { Text("MDBList API Key (IMDb, Rotten Tomatoes, Metacritic & Trakt Scores)") },
                    singleLine = true,
                    colors = naphTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tmdbKey,
                    onValueChange = { tmdbKey = it },
                    label = { Text("TMDB API Key (Optional Artwork & Episode Enrichment)") },
                    singleLine = true,
                    colors = naphTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        appState.updateTrackerConfig(
                            trackerConfig.copy(mdbListApiKey = mdbListKey, tmdbApiKey = tmdbKey)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Metadata Keys", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NotificationsSubPage(appState: NaphAppState) {
    val newEps by appState.newEpisodeNotifications.collectAsState()
    val p2pDone by appState.p2pCompletionNotifications.collectAsState()

    SubPageContainer {
        SettingsSectionCard(sectionTitle = "NOTIFICATION PREFERENCES") {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                SettingsToggleRow(
                    title = "New Episode Release Alerts",
                    subtitle = "Notify when a series in your Library or Continue Watching airs a new episode",
                    checked = newEps,
                    onCheckedChange = { appState.setNotifications(it, p2pDone) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    title = "P2P Swarm & Stream Alerts",
                    subtitle = "Show status banner when P2P temporary cache is automatically purged",
                    checked = p2pDone,
                    onCheckedChange = { appState.setNotifications(newEps, it) }
                )
            }
        }
    }
}

@Composable
private fun AboutSubPage() {
    SubPageContainer {
        SettingsSectionCard(sectionTitle = "ABOUT NAPH") {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                Text("NAPH v1.0.0", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Text(
                    text = "Built with Kotlin Multiplatform & Compose Multiplatform for Windows, Linux, iOS, and Android.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = NaphColors.TextSecondary
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("• Playback Engines: libmpv (Desktop/iOS MPVKit) + AndroidX Media3 ExoPlayer", style = MaterialTheme.typography.bodyMedium)
                Text("• P2P Core: Sequential BitTorrent Piece Scheduler with Auto-Cleaning Temporary Ring Buffer", style = MaterialTheme.typography.bodyMedium)
                Text("• Addon Protocol: Stremio v3 JSON Manifest Compatible", style = MaterialTheme.typography.bodyMedium)
                Text("• Cloud Infrastructure: Built-in NAPH Cloud Cross-Platform Sync", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ============================================================================
// REUSABLE NUVIO-STYLE SETTINGS GROUP COMPONENTS
// ============================================================================

@Composable
private fun SubPageContainer(content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
    sectionTitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = sectionTitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
            content = content
        )
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NaphColors.TextSecondary
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = NaphColors.TextSecondary
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = NaphColors.TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
        modifier = Modifier.padding(start = 78.dp)
    )
}
