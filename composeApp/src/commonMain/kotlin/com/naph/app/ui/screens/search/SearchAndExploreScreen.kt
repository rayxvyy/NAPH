package com.naph.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naph.app.ui.components.CinemaPosterCard
import com.naph.app.ui.state.NaphAppState
import com.naph.app.ui.theme.NaphColors

@Composable
fun SearchAndExploreScreen(
    appState: NaphAppState,
    modifier: Modifier = Modifier
) {
    val query by appState.searchQuery.collectAsState()
    val results by appState.searchResults.collectAsState()
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "movie", "series"

    val filtered = remember(results, selectedTypeFilter) {
        if (selectedTypeFilter == "ALL") results
        else results.filter { it.type.equals(selectedTypeFilter, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NaphColors.ObsidianBg)
            .padding(horizontal = 28.dp, vertical = 22.dp)
    ) {
        Text("Global Search & Addon Catalogs", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = "Search simultaneously across all installed Stremio catalog addons, Jellyfin, and Emby libraries",
            style = MaterialTheme.typography.bodyMedium,
            color = NaphColors.TextMuted
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { appState.updateSearchQuery(it) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NaphColors.CyanGlow) },
            placeholder = { Text("Search movies, series, anime, IMDb ID (tt...), or genre...") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("ALL" to "All Media", "movie" to "Movies", "series" to "Series & Anime").forEach { (key, label) ->
                FilterChip(
                    selected = selectedTypeFilter == key,
                    onClick = { selectedTypeFilter = key },
                    label = { Text(label) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(bottom = 84.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.id }) { item ->
                CinemaPosterCard(
                    item = item,
                    onClick = { appState.openMediaDetail(item) }
                )
            }
        }
    }
}
