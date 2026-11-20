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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.naph.app.ui.components.CinemaPosterCard
import com.naph.app.ui.components.ContinueWatchingCard
import com.naph.app.ui.state.NaphAppState
import com.naph.app.ui.state.ProfileSettingsPage
import com.naph.app.ui.theme.NaphColors

@Composable
fun LibraryAndServersScreen(
    appState: NaphAppState,
    modifier: Modifier = Modifier
) {
    val activeProfile by appState.activeProfile.collectAsState()
    val watchlistMap by appState.watchlistByProfile.collectAsState()
    val progressMap by appState.watchProgressByProfile.collectAsState()
    val personalItems by appState.personalServerItems.collectAsState()
    val servers by appState.mediaServers.collectAsState()

    val watchlist = watchlistMap[activeProfile.id].orEmpty()
    val progressList = progressMap[activeProfile.id].orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("${activeProfile.name}'s Library", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                    Text(
                        text = "Saved watchlist, watch history, and connected personal media servers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NaphColors.TextSecondary
                    )
                }

                OutlinedButton(
                    onClick = { appState.navigateSettingsPage(ProfileSettingsPage.MEDIA_SERVERS) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = NaphColors.CyanGlow)
                    Spacer(Modifier.width(8.dp))
                    Text("Manage Jellyfin / Emby (${servers.size})", color = Color.White)
                }
            }
        }

        // 1. Watchlist Section
        item {
            Column {
                Text("Watchlist (${watchlist.size})", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(Modifier.height(12.dp))
                if (watchlist.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                            .padding(22.dp)
                    ) {
                        Text("Your watchlist is empty.", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Open any movie or series and tap '+ Library' to save it here across all your devices.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NaphColors.TextSecondary
                        )
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(watchlist, key = { "${it.type}:${it.id}" }) { item ->
                            CinemaPosterCard(item = item, onClick = { appState.openMediaDetail(item) })
                        }
                    }
                }
            }
        }

        // 2. Continue Watching / History
        if (progressList.isNotEmpty()) {
            item {
                Column {
                    Text("In Progress & Recent History (${progressList.size})", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(progressList, key = { it.mediaId }) { entry ->
                            ContinueWatchingCard(entry = entry, onClick = {})
                        }
                    }
                }
            }
        }

        // 3. Personal Media Server Library (Jellyfin / Emby)
        if (personalItems.isNotEmpty()) {
            item {
                Column {
                    Text("Jellyfin & Emby Personal Library (${personalItems.size})", style = MaterialTheme.typography.titleLarge, color = Color.White)
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
