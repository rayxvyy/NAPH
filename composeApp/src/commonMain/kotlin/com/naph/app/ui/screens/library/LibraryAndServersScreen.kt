package com.naph.app.ui.screens.library

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.naph.app.core.model.ServerType
import com.naph.app.ui.components.CinemaPosterCard
import com.naph.app.ui.state.NaphAppState
import com.naph.app.ui.theme.NaphColors

@Composable
fun LibraryAndServersScreen(
    appState: NaphAppState,
    modifier: Modifier = Modifier
) {
    val activeProfile by appState.activeProfile.collectAsState()
    val watchlistMap by appState.watchlistByProfile.collectAsState()
    val servers by appState.mediaServers.collectAsState()
    val personalItems by appState.personalServerItems.collectAsState()

    val watchlist = watchlistMap[activeProfile.id].orEmpty()

    var serverType by remember { mutableStateOf(ServerType.JELLYFIN) }
    var serverName by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("https://") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
        modifier = modifier
            .fillMaxSize()
            .background(NaphColors.ObsidianBg)
    ) {
        item {
            Column {
                Text("${activeProfile.name}'s Unified Library & Media Servers", style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = "Manage your cloud-synced watchlist and connect personal Jellyfin & Emby servers",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NaphColors.TextMuted
                )
            }
        }

        // 1. Watchlist Section
        item {
            Column {
                Text("Saved in Watchlist (${watchlist.size})", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                if (watchlist.isEmpty()) {
                    Text("Your watchlist is empty. Click '+ Watchlist' on any movie or series.", color = NaphColors.TextMuted)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(watchlist, key = { it.id }) { item ->
                            CinemaPosterCard(item = item, onClick = { appState.openMediaDetail(item) })
                        }
                    }
                }
            }
        }

        // 2. Connect Jellyfin or Emby Server Card
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
                    Icon(Icons.Default.Dns, contentDescription = null, tint = NaphColors.CyanGlow)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Connect Personal Media Server (Jellyfin / Emby)", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Stream your self-hosted movies & series directly inside NAPH alongside Stremio addons",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = serverType == ServerType.JELLYFIN,
                        onClick = { serverType = ServerType.JELLYFIN },
                        label = { Text("Jellyfin Server") }
                    )
                    FilterChip(
                        selected = serverType == ServerType.EMBY,
                        onClick = { serverType = ServerType.EMBY },
                        label = { Text("Emby Server") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = { Text("Server Label (e.g. Home NAS)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("Server URL (e.g. https://jellyfin.myhome.net)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        appState.connectMediaServer(serverType, serverName, serverUrl, username, password)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaphColors.ElectricPrimary)
                ) {
                    Text("Authenticate & Sync ${serverType.name} Library")
                }

                if (servers.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Connected Servers:", style = MaterialTheme.typography.labelSmall, color = NaphColors.EmeraldSeed)
                    servers.forEach { srv ->
                        Text("• ${srv.name} (${srv.type.name}) — ${srv.serverUrl} [User: ${srv.username}]", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // 3. Personal Server Library Items
        if (personalItems.isNotEmpty()) {
            item {
                Column {
                    Text("Personal Server Library (${personalItems.size})", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(personalItems, key = { it.id }) { item ->
                            CinemaPosterCard(item = item, onClick = { appState.openMediaDetail(item) })
                        }
                    }
                }
            }
        }
    }
}
