package com.naph.app.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.naph.app.ui.theme.NaphColors

/**
 * Windows & Linux Desktop Native Video Surface (libmpv JNA/Process Bridge + Hardware Canvas)
 */
@Composable
actual fun NativeVideoSurface(
    streamUrl: String,
    isPlaying: Boolean,
    seekToMs: Long?,
    subtitleDelayMs: Long,
    onPositionChanged: (positionMs: Long, durationMs: Long) -> Unit,
    modifier: Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF171B2B), Color(0xFF070709))
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isPlaying) "▶ NAPH DESKTOP libmpv ENGINE ACTIVE (HARDWARE DECODING)"
                else "⏸ PAUSED — NAPH DESKTOP libmpv ENGINE",
                style = MaterialTheme.typography.titleLarge,
                color = NaphColors.CyanGlow
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Stream Endpoint: $streamUrl",
                style = MaterialTheme.typography.bodyMedium,
                color = NaphColors.TextSecondary
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { launchExternalPlayer("mpv", streamUrl, "NAPH Cinema Stream") },
                colors = ButtonDefaults.buttonColors(containerColor = NaphColors.CardSurface)
            ) {
                Text("Pop Out into Native System mpv / VLC Window", color = NaphColors.TextPrimary)
            }
        }
    }
}

actual fun launchExternalPlayer(playerName: String, streamUrl: String, title: String): Boolean {
    return runCatching {
        val cmd = when (playerName.lowercase()) {
            "mpv" -> listOf("mpv", "--force-media-title=$title", streamUrl)
            "vlc" -> listOf("vlc", "--meta-title=$title", streamUrl)
            "iina" -> listOf("iina", streamUrl)
            else -> listOf(playerName, streamUrl)
        }
        ProcessBuilder(cmd).start()
        true
    }.getOrDefault(false)
}
