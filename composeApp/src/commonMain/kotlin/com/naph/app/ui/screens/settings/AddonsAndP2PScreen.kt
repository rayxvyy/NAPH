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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import com.naph.app.ui.state.NaphAppState
import com.naph.app.ui.theme.NaphColors

@Composable
fun AddonsAndP2PScreen(
    appState: NaphAppState,
    modifier: Modifier = Modifier
) {
    val addons by appState.installedAddons.collectAsState()
    val p2pSettings by appState.p2pSettings.collectAsState()
    val p2pClearedBytes by appState.p2pCacheClearedBytes.collectAsState()

    var manifestUrlInput by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier
            .fillMaxSize()
            .background(NaphColors.ObsidianBg)
    ) {
        item {
            Column {
                Text("Stremio JSON Manifest Addons & P2P Engine", style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = "Install community addons (manifest.json / stremio://) and tune NAPH's sequential P2P streaming engine",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NaphColors.TextMuted
                )
            }
        }

        // 1. Install Stremio Addon Card
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
                    Icon(Icons.Default.Extension, contentDescription = null, tint = NaphColors.ElectricPrimary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Install Stremio-Compatible Addon", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Paste any manifest.json HTTPS URL or stremio:// addon link for instant catalogs, P2P streams & subtitles",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = manifestUrlInput,
                        onValueChange = { manifestUrlInput = it },
                        placeholder = { Text("https://.../manifest.json or stremio://...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            appState.installAddonByManifestUrl(manifestUrlInput)
                            manifestUrlInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NaphColors.ElectricPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Install Manifest")
                    }
                }

                // Quick 1-Click Popular Community Addon Presets
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        "CyberFlix Catalog" to "https://cyberflix.elfhosted.com/manifest.json",
                        "Anime Kitsu Catalog" to "https://anime-kitsu.strem.fun/manifest.json",
                        "SubDL Subtitles" to "https://subdl.strem.top/manifest.json"
                    ).forEach { (label, url) ->
                        OutlinedButton(onClick = { appState.installAddonByManifestUrl(url) }) {
                            Text("+ $label", color = NaphColors.CyanGlow)
                        }
                    }
                }
            }
        }

        // 2. Installed Addons List
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Installed Addons (${addons.size})", style = MaterialTheme.typography.titleLarge)
                addons.forEach { addon ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NaphColors.CardSurface)
                            .border(1.dp, NaphColors.ElevatedBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(addon.name, style = MaterialTheme.typography.titleMedium)
                                Text("v${addon.version}", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(addon.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(addon.manifestUrl, style = MaterialTheme.typography.labelSmall, color = NaphColors.TextMuted)
                        }

                        if (!addon.id.contains("cinemeta")) {
                            IconButton(onClick = { appState.removeAddon(addon.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Addon", tint = NaphColors.CrimsonPlay)
                            }
                        }
                    }
                }
            }
        }

        // 3. P2P Sequential Torrent Engine & Temporary Cache Controls (#1 Priority)
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NaphColors.CardSurface)
                    .border(1.dp, NaphColors.CyanGlow.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .padding(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = NaphColors.CyanGlow)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("NAPH Sequential P2P Torrent Engine & Temporary Cache", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Configured for Windows, Linux, Android & iOS (Sideload IPA) • Auto-Purged Cache Session Total: ${(p2pClearedBytes / (1024 * 1024))} MB",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NaphColors.CyanGlow
                        )
                    }
                }

                // Temporary Cache Toggle (Default ON like Nuvio)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Temporary Ring-Buffer Cache (Auto-Delete After Playback)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Streams torrents in a temporary cache folder and automatically deletes data when playback stops so your PC/Phone storage never fills up.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
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
                }

                // Optional Power-User Setting: Keep Downloaded Files
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Power-User Mode: Keep Downloaded Video Files on Disk", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Optional: Disable temporary cache auto-deletion and keep completed torrent files in a custom folder.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
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
                }

                if (p2pSettings.keepDownloadedFiles) {
                    OutlinedTextField(
                        value = p2pSettings.customSaveDirectory,
                        onValueChange = { appState.updateP2PSettings(p2pSettings.copy(customSaveDirectory = it)) },
                        label = { Text("Custom Torrent Download Directory (e.g. ~/Videos/NAPH)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Max Temp Cache Size Slider
                Column {
                    Text(
                        text = "Maximum Temporary Ring-Buffer Size: ${p2pSettings.maxCacheSizeGb} GB",
                        style = MaterialTheme.typography.labelSmall,
                        color = NaphColors.TextPrimary
                    )
                    Slider(
                        value = p2pSettings.maxCacheSizeGb.toFloat(),
                        onValueChange = {
                            appState.updateP2PSettings(p2pSettings.copy(maxCacheSizeGb = it.toInt()))
                        },
                        valueRange = 2f..24f
                    )
                }

                // DHT & Peer Exchange Toggles
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Distributed Hash Table (DHT)", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = p2pSettings.enableDht,
                            onCheckedChange = { appState.updateP2PSettings(p2pSettings.copy(enableDht = it)) }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Peer Exchange (PEX)", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = p2pSettings.enablePeerExchange,
                            onCheckedChange = { appState.updateP2PSettings(p2pSettings.copy(enablePeerExchange = it)) }
                        )
                    }
                }

                // External TorrServer Override (Optional for iOS/Desktop Power Users)
                OutlinedTextField(
                    value = p2pSettings.externalTorrServerUrl,
                    onValueChange = { appState.updateP2PSettings(p2pSettings.copy(externalTorrServerUrl = it)) },
                    label = { Text("Optional External TorrServer URL (Leave empty to use built-in NAPH P2P Engine)") },
                    placeholder = { Text("http://127.0.0.1:8090") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
