package com.naph.app.ui.screens.player

import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/**
 * Android Native Hardware-Accelerated Video Surface powered by Media3 ExoPlayer:
 * - Supports 150% Volume Boost via native `LoudnessEnhancer`
 * - Supports Aspect Ratio / Zoom modes (`fit`, `fill`, `16:9`, `stretch`)
 * - Supports external subtitle tracks & subtitle visibility toggle
 */
@OptIn(UnstableApi::class)
@Composable
actual fun NativeVideoSurface(
    streamUrl: String,
    isPlaying: Boolean,
    seekToMs: Long?,
    playbackSpeed: Float,
    volume: Float,
    aspectMode: String,
    subtitleUrl: String?,
    subtitlesEnabled: Boolean,
    subtitleDelayMs: Long,
    subtitleFontSizeSp: Float,
    cycleAudioTick: Int,
    cycleEmbeddedSubTick: Int,
    onPositionChanged: (positionMs: Long, durationMs: Long) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    val exoPlayer = remember(streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(streamUrl))
            prepare()
            playWhenReady = isPlaying
        }
    }

    val loudnessEnhancer = remember(exoPlayer) {
        runCatching {
            LoudnessEnhancer(exoPlayer.audioSessionId).apply { enabled = false }
        }.getOrNull()
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    // Sync Volume (0–100% via ExoPlayer + 101–150% Boost via Android LoudnessEnhancer)
    LaunchedEffect(volume) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
        runCatching {
            if (volume > 1.01f) {
                val gainMb = ((volume - 1.0f) * 1600f).toInt().coerceIn(0, 1000)
                loudnessEnhancer?.setTargetGain(gainMb)
                loudnessEnhancer?.enabled = true
            } else {
                loudnessEnhancer?.enabled = false
            }
        }
    }

    // Sync Aspect Ratio / Zoom Mode
    LaunchedEffect(aspectMode, playerViewRef) {
        playerViewRef?.resizeMode = when (aspectMode.lowercase()) {
            "fill" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            "stretch" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    // Sync Subtitle Visibility
    LaunchedEffect(subtitlesEnabled) {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesEnabled)
            .build()
    }

    // Load External Subtitle Track when selected
    LaunchedEffect(subtitleUrl) {
        val sub = subtitleUrl?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val pos = exoPlayer.currentPosition
        val mime = when {
            sub.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
            sub.endsWith(".ass", ignoreCase = true) || sub.endsWith(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
            else -> MimeTypes.APPLICATION_SUBRIP
        }
        val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub))
            .setMimeType(mime)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        val updatedItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setSubtitleConfigurations(listOf(subConfig))
            .build()
        exoPlayer.setMediaItem(updatedItem, pos)
        exoPlayer.prepare()
    }

    LaunchedEffect(seekToMs) {
        if (seekToMs != null) {
            exoPlayer.seekTo(seekToMs)
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(400)
            val dur = exoPlayer.duration
            if (dur > 0) {
                onPositionChanged(exoPlayer.currentPosition, dur)
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            runCatching { loudnessEnhancer?.release() }
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                playerViewRef = this
            }
        },
        modifier = modifier
    )
}

actual val isDesktopAwtHost: Boolean = false

actual fun toggleNativeFullscreen(): Boolean = false

actual fun exitNativeFullscreen() = Unit

actual fun registerDesktopVideoInteraction(
    onMouseMove: (() -> Unit)?,
    onDoubleClick: (() -> Unit)?
) = Unit

actual fun launchExternalPlayer(playerName: String, streamUrl: String, title: String): Boolean {
    return runCatching {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(streamUrl), "video/*")
            putExtra("title", title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        intent.action != null
    }.getOrDefault(false)
}
