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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naph.app.ui.components.CinemaPosterCard
import com.naph.app.ui.components.ContinueWatchingCard
import com.naph.app.ui.components.ModernGlassButton
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
    val layoutConfig by appState.layoutConfig.collectAsState()

    val watchlist = watchlistMap[activeProfile.id].orEmpty()
    val progressList = progressMap[activeProfile.id].orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
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
                    Text("${activeProfile.name}'s Library", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 22.sp), color = Color.White)
                    Text(
                        text = "Saved watchlist, watch history, and connected personal media servers",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                        color = NaphColors.TextSecondary
                    )
                }

                ModernGlassButton(
                    text = "Jellyfin / Emby (${servers.size})",
                    icon = Icons.Default.Dns,
                    iconTint = NaphColors.CyanGlow,
                    onClick = { appState.navigateSettingsPage(ProfileSettingsPage.MEDIA_SERVERS) }
                )
            }
        }

        // 1. Watchlist Section
        item {
            Column {
                Text("Watchlist (${watchlist.size})", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(10.dp))
                if (watchlist.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF10131C))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(18.dp)
                    ) {
                        Text("Your watchlist is empty.", style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp), color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Open any movie or series and tap '+ Library' to save it here.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                            color = NaphColors.TextSecondary
                        )
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(watchlist, key = { "${it.type}:${it.id}" }) { item ->
                            CinemaPosterCard(
                                item = item,
                                layoutConfig = layoutConfig,
                                onClick = { appState.openMediaDetail(item) }
                            )
                        }
                    }
                }
            }
        }

        // 2. Continue Watching / History
        if (progressList.isNotEmpty()) {
            item {
                Column {
                    Text("In Progress & Recent History (${progressList.size})", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
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
                    Text("Jellyfin & Emby Personal Library (${personalItems.size})", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(personalItems, key = { it.id }) { item ->
                            CinemaPosterCard(
                                item = item,
                                layoutConfig = layoutConfig,
                                onClick = { appState.openMediaDetail(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}
