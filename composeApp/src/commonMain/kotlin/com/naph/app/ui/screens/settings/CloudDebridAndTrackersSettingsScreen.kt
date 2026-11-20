package com.naph.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.naph.app.ui.state.NaphAppState
import com.naph.app.ui.theme.NaphColors

@Composable
fun CloudDebridAndTrackersSettingsScreen(
    appState: NaphAppState,
    modifier: Modifier = Modifier
) {
    val debridConfig by appState.debridConfig.collectAsState()
    val trackerConfig by appState.trackerConfig.collectAsState()
    val supabaseUrl by appState.syncRepo.supabaseUrl.collectAsState()
    val supabaseAnonKey by appState.syncRepo.supabaseAnonKey.collectAsState()
    val cloudStatus by appState.syncRepo.cloudSyncStatus.collectAsState()

    var sbUrlInput by remember(supabaseUrl) { mutableStateOf(supabaseUrl) }
    var sbKeyInput by remember(supabaseAnonKey) { mutableStateOf(supabaseAnonKey) }

    var rdKey by remember(debridConfig) { mutableStateOf(debridConfig.realDebridApiKey) }
    var adKey by remember(debridConfig) { mutableStateOf(debridConfig.allDebridApiKey) }
    var tbKey by remember(debridConfig) { mutableStateOf(debridConfig.torBoxApiKey) }
    var pmKey by remember(debridConfig) { mutableStateOf(debridConfig.premiumizeApiKey) }
    var preferDebrid by remember(debridConfig) { mutableStateOf(debridConfig.preferDebridOverP2P) }

    var traktToken by remember(trackerConfig) { mutableStateOf(trackerConfig.traktAccessToken) }
    var simklToken by remember(trackerConfig) { mutableStateOf(trackerConfig.simklAccessToken) }
    var mdbListKey by remember(trackerConfig) { mutableStateOf(trackerConfig.mdbListApiKey) }
    var autoScrobble by remember(trackerConfig) { mutableStateOf(trackerConfig.autoScrobbleEnabled) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier
            .fillMaxSize()
            .background(NaphColors.ObsidianBg)
    ) {
        item {
            Column {
                Text("NAPH Cloud Sync, Debrid & Trackers", style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = "Configure 24/7 Free Supabase Cloud Sync, Real-Debrid / AllDebrid / TorBox / Premiumize, Trakt.tv, SIMKL & MDBList",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NaphColors.TextMuted
                )
            }
        }

        // 1. Free 24/7 Supabase Cloud Sync (No Local Docker Required)
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NaphColors.CardSurface)
                    .border(1.dp, NaphColors.ElectricPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = NaphColors.ElectricPrimary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("NAPH Cloud Sync (Supabase Free Managed Cloud Tier)", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Status: $cloudStatus",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NaphColors.EmeraldSeed
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = sbUrlInput,
                        onValueChange = { sbUrlInput = it },
                        label = { Text("Supabase Project URL (https://<project-ref>.supabase.co)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sbKeyInput,
                        onValueChange = { sbKeyInput = it },
                        label = { Text("Supabase Publishable / Anon Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        appState.syncRepo.configureSupabase(sbUrlInput, sbKeyInput)
                        appState.showBanner("Connected NAPH Cloud Sync to Supabase Free Tier")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaphColors.ElectricPrimary)
                ) {
                    Text("Save & Connect Supabase Cloud Sync")
                }
            }
        }

        // 2. Debrid Services Integration (Real-Debrid, AllDebrid, TorBox, Premiumize)
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NaphColors.CardSurface)
                    .border(1.dp, NaphColors.EmeraldSeed.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .padding(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = NaphColors.EmeraldSeed)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Debrid Services (Real-Debrid, AllDebrid, TorBox, Premiumize)", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Optional: Resolve P2P torrent infoHashes via cached Debrid servers for instant 4K HDR HTTP streaming",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Prefer Debrid Cache over Pure P2P When Available", style = MaterialTheme.typography.titleMedium)
                        Text("If off, NAPH uses its built-in Sequential P2P Torrent Engine first (#1 Priority)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = preferDebrid,
                        onCheckedChange = { preferDebrid = it }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = rdKey,
                        onValueChange = { rdKey = it },
                        label = { Text("Real-Debrid API Token") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tbKey,
                        onValueChange = { tbKey = it },
                        label = { Text("TorBox API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = adKey,
                        onValueChange = { adKey = it },
                        label = { Text("AllDebrid API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pmKey,
                        onValueChange = { pmKey = it },
                        label = { Text("Premiumize API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

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
                    colors = ButtonDefaults.buttonColors(containerColor = NaphColors.EmeraldSeed)
                ) {
                    Text("Save Debrid Providers", color = NaphColors.ObsidianBg)
                }
            }
        }

        // 3. 2-Way Trackers (Trakt.tv, SIMKL) & MDBList Aggregated Ratings Key
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NaphColors.CardSurface)
                    .border(1.dp, NaphColors.ElevatedBorder, RoundedCornerShape(16.dp))
                    .padding(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = NaphColors.CyanGlow)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("2-Way Trackers (Trakt.tv & SIMKL) + MDBList Ratings", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Automatically scrobble watched movies/episodes and pull live IMDb, Rotten Tomatoes, Metacritic & Trakt scores",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Real-Time Auto-Scrobbling to Trakt.tv & SIMKL", style = MaterialTheme.typography.titleMedium)
                    Switch(checked = autoScrobble, onCheckedChange = { autoScrobble = it })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = traktToken,
                        onValueChange = { traktToken = it },
                        label = { Text("Trakt.tv OAuth Access Token") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = simklToken,
                        onValueChange = { simklToken = it },
                        label = { Text("SIMKL OAuth Access Token") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = mdbListKey,
                    onValueChange = { mdbListKey = it },
                    label = { Text("MDBList API Key (For Live Aggregated IMDb / Rotten Tomatoes / Metacritic Scores)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        appState.updateTrackerConfig(
                            trackerConfig.copy(
                                traktAccessToken = traktToken,
                                simklAccessToken = simklToken,
                                mdbListApiKey = mdbListKey,
                                autoScrobbleEnabled = autoScrobble
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaphColors.ElectricPrimary)
                ) {
                    Text("Save Tracker & Rating Sync Settings")
                }
            }
        }
    }
}
