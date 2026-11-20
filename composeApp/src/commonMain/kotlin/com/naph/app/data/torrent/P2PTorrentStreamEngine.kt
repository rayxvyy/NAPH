package com.naph.app.data.torrent

import com.naph.app.core.model.MediaStream
import com.naph.app.core.model.P2PSettings
import com.naph.app.core.model.P2PStreamTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NAPH Sequential P2P Torrent Streaming Engine (#1 Priority)
 * Bridges directly to PlatformP2PStreamer (Native C++20 libtorrent / NuvioEngine on Desktop).
 */
class P2PTorrentStreamEngine {
    private val _telemetry = MutableStateFlow<P2PStreamTelemetry?>(null)
    val telemetry: StateFlow<P2PStreamTelemetry?> = _telemetry.asStateFlow()

    private val _cacheClearedCountBytes = MutableStateFlow(0L)
    val cacheClearedCountBytes: StateFlow<Long> = _cacheClearedCountBytes.asStateFlow()

    fun startSequentialStream(
        stream: MediaStream,
        settings: P2PSettings,
        onStreamReady: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        stopAndCleanSession(settings)

        PlatformP2PStreamer.startNativeP2PStream(
            stream = stream,
            settings = settings,
            onTelemetryUpdate = { tel ->
                _telemetry.value = tel
            },
            onStreamReady = { localHttpRangeUrl ->
                onStreamReady(localHttpRangeUrl)
            },
            onStreamError = { err ->
                onError(err)
            }
        )
    }

    fun stopAndCleanSession(settings: P2PSettings) {
        val cleared = PlatformP2PStreamer.stopNativeP2PStream(settings)
        _telemetry.value = null
        if (cleared > 0L) {
            _cacheClearedCountBytes.value += cleared
        }
    }
}
