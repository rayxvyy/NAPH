package com.naph.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.naph.app.ui.components.CinemaPosterCard
import com.naph.app.ui.state.NaphAppState
import com.naph.app.ui.state.ProfileSettingsPage
import com.naph.app.ui.theme.NaphColors
import com.naph.app.ui.theme.naphTextFieldColors

@Composable
fun SearchAndExploreScreen(
    appState: NaphAppState,
    modifier: Modifier = Modifier
) {
    val query by appState.searchQuery.collectAsState()
    val results by appState.searchResults.collectAsState()
    val isSearching by appState.isSearching.collectAsState()
    val installedAddons by appState.installedAddons.collectAsState()
    var selectedTypeFilter by remember { mutableStateOf("ALL") }

    val filtered = remember(results, selectedTypeFilter) {
        if (selectedTypeFilter == "ALL") results
        else results.filter { it.type.equals(selectedTypeFilter, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 28.dp, vertical = 22.dp)
    ) {
        Text("Search & Discover", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Text(
            text = "Search across your installed catalog addons and personal media servers",
            style = MaterialTheme.typography.bodyMedium,
            color = NaphColors.TextSecondary
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { appState.updateSearchQuery(it) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
            },
            placeholder = { Text("Search movies, series, anime, or IMDb ID (tt...)...") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = naphTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("ALL" to "All", "movie" to "Movies", "series" to "Series & Anime").forEach { (key, label) ->
                FilterChip(
                    selected = selectedTypeFilter == key,
                    onClick = { selectedTypeFilter = key },
                    label = { Text(label) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (installedAddons.isEmpty() && results.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("No Catalog Addons Installed Yet", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text(
                        text = "Install a catalog addon in Profile → Content & Discovery → Installed Addons to search across catalogs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NaphColors.TextSecondary
                    )
                    Button(
                        onClick = { appState.navigateSettingsPage(ProfileSettingsPage.ADDONS_MANAGER) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Go to Installed Addons", color = Color.White)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(bottom = 84.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { "${it.type}:${it.id}" }) { item ->
                    CinemaPosterCard(
                        item = item,
                        onClick = { appState.openMediaDetail(item) }
                    )
                }
            }
        }
    }
}
