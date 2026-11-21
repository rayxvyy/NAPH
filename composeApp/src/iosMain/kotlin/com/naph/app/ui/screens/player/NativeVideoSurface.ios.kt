package com.naph.app.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import platform.AVFoundation.*
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.*
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIView

/**
 * Optional MPVKit Native View Hook for MKV / Dolby Vision / libass raw rendering on iOS.
 * Falls back automatically to Apple's hardware-accelerated AVPlayerViewController when MPVKit is not registered.
 */
interface IosMpvSurfaceController {
    val view: UIView
    fun loadStream(url: String)
    fun setPlaying(playing: Boolean)
    fun seekToMs(ms: Long)
    fun setSubtitleDelayMs(delayMs: Long)
    fun currentPositionMs(): Long
    fun currentDurationMs(): Long
    fun destroy()
}

object IosVideoEngineRegistry {
    var mpvControllerFactory: (() -> IosMpvSurfaceController)? = null
}

/**
 * Real iOS Native Video Surface:
 * - Embeds either MPVKit (if registered by the Swift host) or Apple's hardware-accelerated AVPlayerViewController
 *   directly inside the Compose Multiplatform view hierarchy via `UIKitView`.
 * - Synchronizes play/pause, seeking, subtitle delay, and 500ms position/duration telemetry with NAPH's modern player bar.
 */
@OptIn(ExperimentalForeignApi::class)
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
    val mpvController = remember { IosVideoEngineRegistry.mpvControllerFactory?.invoke() }
    val avPlayer = remember { if (mpvController == null) AVPlayer() else null }
    val avPlayerViewController = remember {
        if (mpvController == null) {
            AVPlayerViewController().apply {
                this.player = avPlayer
                this.showsPlaybackControls = false
                this.allowsPictureInPicturePlayback = true
                this.view.backgroundColor = UIColor.blackColor
            }
        } else null
    }

    var lastLoadedUrl by remember { mutableStateOf("") }

    // Load or switch stream URL
    LaunchedEffect(streamUrl) {
        if (streamUrl.isBlank() || streamUrl == lastLoadedUrl) return@LaunchedEffect
        lastLoadedUrl = streamUrl
        if (mpvController != null) {
            mpvController.loadStream(streamUrl)
            mpvController.setPlaying(isPlaying)
        } else {
            val nsUrl = NSURL.URLWithString(streamUrl)
            if (nsUrl != null) {
                val item = AVPlayerItem.playerItemWithURL(nsUrl)
                avPlayer?.replaceCurrentItemWithPlayerItem(item)
                if (isPlaying) avPlayer?.play() else avPlayer?.pause()
            }
        }
    }

    // Sync Play / Pause & Playback Speed
    LaunchedEffect(isPlaying, playbackSpeed) {
        if (mpvController != null) {
            mpvController.setPlaying(isPlaying)
        } else {
            if (isPlaying) {
                avPlayer?.play()
                avPlayer?.rate = playbackSpeed
            } else {
                avPlayer?.pause()
            }
        }
    }

    // Sync Volume
    LaunchedEffect(volume) {
        avPlayer?.volume = volume.coerceIn(0f, 1f)
    }

    // Sync Seek
    LaunchedEffect(seekToMs) {
        val targetMs = seekToMs ?: return@LaunchedEffect
        if (mpvController != null) {
            mpvController.seekToMs(targetMs)
        } else {
            val targetSeconds = targetMs.toDouble() / 1000.0
            avPlayer?.seekToTime(CMTimeMakeWithSeconds(targetSeconds, 1000))
        }
    }

    // Sync Subtitle Delay
    LaunchedEffect(subtitleDelayMs) {
        mpvController?.setSubtitleDelayMs(subtitleDelayMs)
    }

    // Poll real position & duration every 500ms
    LaunchedEffect(mpvController, avPlayer) {
        while (isActive) {
            delay(500)
            if (mpvController != null) {
                val pos = mpvController.currentPositionMs()
                val dur = mpvController.currentDurationMs()
                if (dur > 0L) {
                    onPositionChanged(pos.coerceAtLeast(0L), dur.coerceAtLeast(1L))
                }
            } else if (avPlayer != null) {
                val currentSec = CMTimeGetSeconds(avPlayer.currentTime())
                val itemDur = avPlayer.currentItem?.duration
                val durSec = if (itemDur != null) CMTimeGetSeconds(itemDur) else 0.0
                if (!currentSec.isNaN() && !durSec.isNaN() && durSec > 0.0) {
                    onPositionChanged(
                        (currentSec * 1000.0).toLong().coerceAtLeast(0L),
                        (durSec * 1000.0).toLong().coerceAtLeast(1L)
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mpvController?.destroy()
            avPlayer?.pause()
            avPlayer?.replaceCurrentItemWithPlayerItem(null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        UIKitView(
            factory = {
                mpvController?.view ?: avPlayerViewController!!.view
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

actual fun launchExternalPlayer(playerName: String, streamUrl: String, title: String): Boolean {
    val schemeUrl = when {
        playerName.contains("Infuse", ignoreCase = true) -> "infuse://x-callback-url/play?url=$streamUrl"
        playerName.contains("Outplayer", ignoreCase = true) -> "outplayer://$streamUrl"
        playerName.contains("VLC", ignoreCase = true) -> "vlc-x-callback://x-callback-url/stream?url=$streamUrl"
        else -> streamUrl
    }
    val nsUrl = NSURL.URLWithString(schemeUrl) ?: return false
    UIApplication.sharedApplication.openURL(nsUrl)
    return true
}
