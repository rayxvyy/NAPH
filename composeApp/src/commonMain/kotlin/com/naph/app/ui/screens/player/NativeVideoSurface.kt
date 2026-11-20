package com.naph.app.ui.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-native video rendering surface:
 * - desktopMain (Windows & Linux): Native libmpv / Process Window / Hardware Video Canvas
 * - androidMain (Android): Media3 ExoPlayer PlayerView + libmpv support
 * - iosMain (iOS): MPVKit / AVPlayer native rendering layer
 */
@Composable
expect fun NativeVideoSurface(
    streamUrl: String,
    isPlaying: Boolean,
    seekToMs: Long?,
    subtitleDelayMs: Long,
    onPositionChanged: (positionMs: Long, durationMs: Long) -> Unit,
    modifier: Modifier = Modifier
)

/**
 * Launches an external desktop/mobile media player (VLC, MPV, IINA, Infuse, Outplayer)
 */
expect fun launchExternalPlayer(playerName: String, streamUrl: String, title: String): Boolean
