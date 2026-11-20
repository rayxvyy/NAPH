package com.naph.app.data.torrent

import com.naph.app.core.model.MediaStream
import com.naph.app.core.model.P2PSettings
import com.naph.app.core.model.P2PStreamTelemetry

actual object PlatformP2PStreamer {
    actual fun startNativeP2PStream(
        stream: MediaStream,
        settings: P2PSettings,
        onTelemetryUpdate: (P2PStreamTelemetry) -> Unit,
        onStreamReady: (String) -> Unit,
        onStreamError: (String) -> Unit
    ) {
        val url = if (settings.externalTorrServerUrl.isNotBlank()) {
            "${settings.externalTorrServerUrl.trimEnd('/')}/stream/video.mkv?link=${stream.infoHash}&index=${stream.fileIdx ?: 0}&play"
        } else {
            stream.url ?: ""
        }
        if (url.isNotBlank()) onStreamReady(url) else onStreamError("Configure P2P / TorrServer or select an HTTP stream")
    }

    actual fun stopNativeP2PStream(settings: P2PSettings): Long = 0L
}
