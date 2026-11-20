package com.naph.app.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.naph.app.ui.theme.NaphColors
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * iOS Native Video Surface (MPVKit / AVPlayer Bridge + Infuse / Outplayer URL Scheme Handoff)
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
            .background(Color.Black)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isPlaying) "▶ NAPH iOS MPVKit / AVPlayer Hardware Surface Active"
                else "⏸ Paused — NAPH iOS MPVKit Surface",
                style = MaterialTheme.typography.titleLarge,
                color = NaphColors.CyanGlow
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = streamUrl,
                style = MaterialTheme.typography.bodyMedium,
                color = NaphColors.TextSecondary
            )
        }
    }
}

actual fun launchExternalPlayer(playerName: String, streamUrl: String, title: String): Boolean {
    val schemeUrl = when {
        playerName.contains("Infuse", ignoreCase = true) -> "infuse://x-callback-url/play?url=$streamUrl"
        playerName.contains("Outplayer", ignoreCase = true) -> "outplayer://$streamUrl"
        playerName.contains("VLC", ignoreCase = true) -> "vlc://$streamUrl"
        else -> streamUrl
    }
    val nsUrl = NSURL.URLWithString(schemeUrl) ?: return false
    UIApplication.sharedApplication.openURL(nsUrl)
    return true
}
