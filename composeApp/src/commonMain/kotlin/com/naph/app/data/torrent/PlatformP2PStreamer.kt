package com.naph.app.data.torrent

import com.naph.app.core.model.MediaStream
import com.naph.app.core.model.P2PSettings
import com.naph.app.core.model.P2PStreamTelemetry
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-specific native P2P BitTorrent streaming bridge (libtorrent C++20 / NuvioEngine on Desktop)
 */
expect object PlatformP2PStreamer {
    fun startNativeP2PStream(
        stream: MediaStream,
        settings: P2PSettings,
        onTelemetryUpdate: (P2PStreamTelemetry) -> Unit,
        onStreamReady: (String) -> Unit,
        onStreamError: (String) -> Unit
    )

    fun stopNativeP2PStream(settings: P2PSettings): Long
}
