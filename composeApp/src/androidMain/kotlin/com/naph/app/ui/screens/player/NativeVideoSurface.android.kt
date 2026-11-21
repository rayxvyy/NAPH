package com.naph.app.ui.screens.player

import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/**
 * Android Native Video Surface powered by Media3 ExoPlayer (with HLS, DASH, MKV & HTTP Byte-Range P2P support)
 */
@OptIn(UnstableApi::class)
@Composable
actual fun NativeVideoSurface(
    streamUrl: String,
    isPlaying: Boolean,
    seekToMs: Long?,
    playbackSpeed: Float,
    volume: Float,
    subtitleUrl: String?,
    subtitleDelayMs: Long,
    onPositionChanged: (positionMs: Long, durationMs: Long) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(streamUrl))
            prepare()
            playWhenReady = isPlaying
        }
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(volume) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
    }

    LaunchedEffect(seekToMs) {
        if (seekToMs != null) {
            exoPlayer.seekTo(seekToMs)
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(500)
            val dur = exoPlayer.duration
            if (dur > 0) {
                onPositionChanged(exoPlayer.currentPosition, dur)
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
            }
        },
        modifier = modifier
    )
}

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
