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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.sp
import com.naph.app.ui.components.CinemaPosterCard
import com.naph.app.ui.components.ModernFilterPill
import com.naph.app.ui.components.ModernPrimaryButton
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
    val layoutConfig by appState.layoutConfig.collectAsState()
    var selectedTypeFilter by remember { mutableStateOf("ALL") }

    val filtered = remember(results, selectedTypeFilter) {
        if (selectedTypeFilter == "ALL") results
        else results.filter { it.type.equals(selectedTypeFilter, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text("Search & Discover", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 22.sp), color = Color.White)
        Text(
            text = "Search across your installed catalog addons and personal media servers",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
            color = NaphColors.TextSecondary
        )

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { appState.updateSearchQuery(it) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            },
            placeholder = { Text("Search movies, series, anime, or IMDb ID (tt...)...") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = naphTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL" to "All", "movie" to "Movies", "series" to "Series & Anime").forEach { (key, label) ->
                ModernFilterPill(
                    label = label,
                    selected = selectedTypeFilter == key,
                    onClick = { selectedTypeFilter = key }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (installedAddons.isEmpty() && results.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("No Catalog Addons Installed Yet", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(
                        text = "Install a catalog addon in Profile → Content & Discovery → Installed Addons to search across catalogs.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                        color = NaphColors.TextSecondary
                    )
                    ModernPrimaryButton(
                        text = "Go to Installed Addons",
                        icon = Icons.Default.Extension,
                        onClick = { appState.navigateSettingsPage(ProfileSettingsPage.ADDONS_MANAGER) }
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = layoutConfig.posterSizePreset.widthDp.dp),
                contentPadding = PaddingValues(bottom = 72.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = filtered,
                    key = { "${it.type}:${it.id}" },
                    contentType = { "poster_card" }
                ) { item ->
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
