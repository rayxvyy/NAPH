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
import com.naph.app.core.model.MediaItem
import com.naph.app.core.model.WatchProgressEntry
import com.naph.app.ui.theme.NaphColors

@Composable
fun CinemaPosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(164.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .background(NaphColors.CardSurface)
                .border(1.dp, NaphColors.ElevatedBorder, RoundedCornerShape(14.dp))
        ) {
            if (!item.poster.isNullOrBlank()) {
                AsyncImage(
                    model = item.poster,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC070709)),
                            startY = 180f
                        )
                    )
            )

            // Rating Badge (Top Right)
            if (!item.imdbRating.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xD90E0F14))
                        .border(0.5.dp, NaphColors.GoldRating.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = NaphColors.GoldRating,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = item.imdbRating,
                        style = MaterialTheme.typography.labelSmall,
                        color = NaphColors.TextPrimary
                    )
                }
            }

            // Source / Type Badge (Top Left)
            val badgeText = if (item.sourceOrigin != "Addon") item.sourceOrigin else item.type.uppercase()
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (item.sourceOrigin != "Addon") NaphColors.CyanGlow.copy(alpha = 0.85f)
                        else Color(0xB3151720)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (item.sourceOrigin != "Addon") Color.Black else NaphColors.TextSecondary
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = listOfNotNull(item.releaseInfo, item.genres.firstOrNull()).joinToString(" • "),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = NaphColors.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
            .width(270.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(NaphColors.CardSurface)
            .border(1.dp, NaphColors.ElevatedBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
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
                            colors = listOf(Color(0x33070709), Color(0xE6070709))
                        )
                    )
            )

            // Play Icon Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(NaphColors.ElectricPrimary.copy(alpha = 0.9f))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sub = if (entry.season != null && entry.episode != null) {
                    "S${entry.season}:E${entry.episode} • ${entry.episodeTitle.orEmpty()}"
                } else {
                    "${((entry.durationMs - entry.positionMs) / 60000L).coerceAtLeast(1)}m remaining"
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
            color = NaphColors.ElectricPrimary,
            trackColor = NaphColors.ElevatedBorder,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        )
    }
}

/**
 * Aggregated Ratings Bar (IMDb, Rotten Tomatoes, Metacritic, Trakt, MDBList)
 */
@Composable
fun AggregatedRatingsBar(
    ratings: AggregatedRatings,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        ratings.imdb?.let {
            RatingBadgePill(
                label = "IMDb",
                value = it,
                accentColor = NaphColors.GoldRating
            )
        }
        ratings.rottenTomatoesCritics?.let {
            RatingBadgePill(
                label = "🍅 RT Critics",
                value = it,
                accentColor = Color(0xFFFA320A)
            )
        }
        ratings.rottenTomatoesAudience?.let {
            RatingBadgePill(
                label = "🍿 RT Audience",
                value = it,
                accentColor = Color(0xFFF59E0B)
            )
        }
        ratings.metacritic?.let {
            RatingBadgePill(
                label = "Metacritic",
                value = it,
                accentColor = NaphColors.EmeraldSeed
            )
        }
        ratings.trakt?.let {
            RatingBadgePill(
                label = "Trakt",
                value = it,
                accentColor = Color(0xFFED1C24)
            )
        }
        ratings.mdbListScore?.let {
            RatingBadgePill(
                label = "MDBList",
                value = it,
                accentColor = NaphColors.CyanGlow
            )
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
            .clip(RoundedCornerShape(10.dp))
            .background(NaphColors.CardSurface)
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
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
            color = NaphColors.TextPrimary
        )
    }
}
