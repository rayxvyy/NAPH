package com.naph.app.ui.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-native video rendering surface (Nuvio-grade performance across all OSes):
 * - desktopMain (Windows & Linux): Native libmpv with 400MB demuxer cache, hwdec=auto-safe, speed, volume & sub-add
 * - androidMain (Android): Media3 ExoPlayer PlayerView
 * - iosMain (iOS): Native hardware AVPlayerViewController / MPVKit with PiP, speed & volume control
 */
@Composable
expect fun NativeVideoSurface(
    streamUrl: String,
    isPlaying: Boolean,
    seekToMs: Long?,
    playbackSpeed: Float = 1.0f,
    volume: Float = 1.0f,
    subtitleUrl: String? = null,
    subtitleDelayMs: Long = 0L,
    onPositionChanged: (positionMs: Long, durationMs: Long) -> Unit,
    modifier: Modifier = Modifier
)

/**
 * Launches an external desktop/mobile media player (VLC, MPV, IINA, Infuse, Outplayer)
 */
expect fun launchExternalPlayer(playerName: String, streamUrl: String, title: String): Boolean
