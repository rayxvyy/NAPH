package com.naph.app.ui.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-native video rendering surface across Linux, Windows, iOS, and Android:
 * - desktopMain (Windows & Linux): Native libmpv with 400MB demuxer cache, hwdec=auto-safe, 150% volume boost,
 *   aspect ratio/zoom modes, embedded audio/subtitle cycling & external sub-add
 * - iosMain (iOS): Native NuvioMedia/MPVKit (`gpu-next` + `MoltenVK` + `VideoToolbox` + `libass`) with 150% volume boost,
 *   aspect ratio/zoom modes, embedded audio/subtitle cycling & external sub-add (falls back to AVPlayer if needed)
 * - androidMain (Android): Media3 ExoPlayer PlayerView
 */
@Composable
expect fun NativeVideoSurface(
    streamUrl: String,
    isPlaying: Boolean,
    seekToMs: Long?,
    playbackSpeed: Float = 1.0f,
    volume: Float = 1.0f,
    aspectMode: String = "fit",
    subtitleUrl: String? = null,
    subtitlesEnabled: Boolean = true,
    subtitleDelayMs: Long = 0L,
    subtitleFontSizeSp: Float = 22f,
    cycleAudioTick: Int = 0,
    cycleEmbeddedSubTick: Int = 0,
    onPositionChanged: (positionMs: Long, durationMs: Long) -> Unit,
    modifier: Modifier = Modifier
)

/**
 * True on Desktop JVM where `NativeVideoSurface` embeds a heavyweight AWT Canvas (`SwingPanel`)
 * that paints above sibling Compose overlays in the same window.
 */
expect val isDesktopAwtHost: Boolean

/**
 * Toggles real OS window fullscreen on Desktop (Linux/Windows). No-op on mobile where playback is already fullscreen.
 */
expect fun toggleNativeFullscreen()

/**
 * Exits OS window fullscreen on Desktop when leaving the player.
 */
expect fun exitNativeFullscreen()

/**
 * Launches an external desktop/mobile media player (VLC, MPV, IINA, Infuse, Outplayer)
 */
expect fun launchExternalPlayer(playerName: String, streamUrl: String, title: String): Boolean
