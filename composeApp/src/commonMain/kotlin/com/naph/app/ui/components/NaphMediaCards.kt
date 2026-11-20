package com.naph.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.naph.app.core.model.AggregatedRatings
import com.naph.app.core.model.LayoutCustomizationConfig
import com.naph.app.core.model.MediaItem
import com.naph.app.core.model.WatchProgressEntry
import com.naph.app.ui.theme.NaphColors

/**
 * Pure Netflix-Style Poster Card:
 * - Zero addon/catalog badges on the poster artwork ("Hosted by...", etc. completely removed!)
 * - Clean poster artwork with customizable corner radius & card width
 * - Optional title below or rating badge only if enabled by user in Layout Customization Settings
 */
@Composable
fun CinemaPosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    layoutConfig: LayoutCustomizationConfig = LayoutCustomizationConfig(),
    modifier: Modifier = Modifier
) {
    val cornerShape = RoundedCornerShape(layoutConfig.posterCornerRadiusDp.dp)
    val cardWidth = layoutConfig.posterSizePreset.widthDp.dp

    Column(
        modifier = modifier
            .width(cardWidth)
            .clip(cornerShape)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(cornerShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(0.75.dp, Color.White.copy(alpha = 0.10f), cornerShape)
        ) {
            if (!item.poster.isNullOrBlank()) {
                AsyncImage(
                    model = item.poster,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback title only if poster URL is missing
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 3
                    )
                }
            }

            // Optional IMDb Rating Badge (OFF by default so poster is 100% clean like Netflix)
            if (layoutConfig.showRatingBadgeOnPoster && !item.imdbRating.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xD90A0B0E))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = NaphColors.GoldRating,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = item.imdbRating,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White
                    )
                }
            }
        }

        // Optional Title Below Poster (OFF by default for pure Netflix poster look)
        if (layoutConfig.showPosterTitlesBelow) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ContinueWatchingCard(
    entry: WatchProgressEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(260.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp)
        ) {
            AsyncImage(
                model = entry.backdropUrl ?: entry.posterUrl,
                contentDescription = entry.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0x22000000), Color(0xE6070709))
                        )
                    )
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.92f))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sub = if (entry.season != null && entry.episode != null) {
                    "S${entry.season}:E${entry.episode} • ${entry.episodeTitle.orEmpty()}"
                } else {
                    "${((entry.durationMs - entry.positionMs) / 60000L).coerceAtLeast(1)}m left"
                }
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = NaphColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        LinearProgressIndicator(
            progress = { entry.progressFraction },
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .fillMaxWidth()
                .height(3.5.dp)
        )
    }
}

/**
 * Customizable Ratings Bar:
 * Only renders the specific rating providers that the user has toggled ON in Settings!
 * (Default: Only IMDb is ON; Rotten Tomatoes, Metacritic, Trakt, and MDBList are optional toggles in Settings)
 */
@Composable
fun AggregatedRatingsBar(
    ratings: AggregatedRatings,
    layoutConfig: LayoutCustomizationConfig = LayoutCustomizationConfig(),
    modifier: Modifier = Modifier
) {
    val activePills = buildList {
        if (layoutConfig.showImdbRating && !ratings.imdb.isNullOrBlank()) {
            add(Triple("IMDb", ratings.imdb, NaphColors.GoldRating))
        }
        if (layoutConfig.showRottenTomatoesCritics && !ratings.rottenTomatoesCritics.isNullOrBlank()) {
            add(Triple("🍅 RT", ratings.rottenTomatoesCritics, Color(0xFFFA320A)))
        }
        if (layoutConfig.showRottenTomatoesAudience && !ratings.rottenTomatoesAudience.isNullOrBlank()) {
            add(Triple("🍿 Audience", ratings.rottenTomatoesAudience, Color(0xFFF59E0B)))
        }
        if (layoutConfig.showMetacriticRating && !ratings.metacritic.isNullOrBlank()) {
            add(Triple("Metacritic", ratings.metacritic, NaphColors.EmeraldSeed))
        }
        if (layoutConfig.showTraktRating && !ratings.trakt.isNullOrBlank()) {
            add(Triple("Trakt", ratings.trakt, Color(0xFFED1C24)))
        }
        if (layoutConfig.showMdbListRating && !ratings.mdbListScore.isNullOrBlank()) {
            add(Triple("MDBList", ratings.mdbListScore, NaphColors.CyanGlow))
        }
    }

    if (activePills.isEmpty()) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        activePills.forEach { (label, value, accent) ->
            RatingBadgePill(label = label, value = value, accentColor = accent)
        }
    }
}

@Composable
private fun RatingBadgePill(
    label: String,
    value: String,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xCC13151B))
            .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = Color.White
        )
    }
}
