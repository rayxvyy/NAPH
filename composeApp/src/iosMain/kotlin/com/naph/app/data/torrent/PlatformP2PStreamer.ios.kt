package com.naph.app.data.torrent

import com.naph.app.core.model.MediaStream
import com.naph.app.core.model.P2PSettings
import com.naph.app.core.model.P2PStreamTelemetry
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory

/**
 * Native Swift / C++20 libtorrent-rasterbar Bridge Interface for iOS.
 * The iOS host target (`iosApp`) registers its native C++ `libtorrent` / `NuvioEngine` implementation here at startup.
 */
interface IosNativeTorrentEngineBridge {
    fun startSequentialMagnetStream(
        magnetUri: String,
        infoHash: String,
        fileIndex: Int,
        saveDirectory: String,
        maxDownloadKbps: Int,
        maxUploadKbps: Int,
        enableDht: Boolean
    ): String?

    fun querySwarmTelemetry(infoHash: String): P2PStreamTelemetry?
    fun stopAndRemoveStream(infoHash: String, deleteFiles: Boolean): Long
}

@OptIn(ExperimentalForeignApi::class)
actual object PlatformP2PStreamer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeJob: Job? = null
    private var activeInfoHash: String? = null

    /**
     * Registered by the Swift host (`NaphTorrentBridge.swift`) on iOS app launch.
     */
    var nativeBridge: IosNativeTorrentEngineBridge? = null

    private val defaultTrackers = listOf(
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://open.stealth.si:80/announce",
        "udp://tracker.torrent.eu.org:451/announce",
        "udp://tracker.bittor.pw:1337/announce",
        "udp://public.popcorn-tracker.org:6969/announce",
        "udp://tracker.dler.org:6969/announce",
        "udp://exodus.desync.com:6969",
        "http://tracker.openbittorrent.com:80/announce"
    )

    private fun resolveIosCacheDirectory(settings: P2PSettings): String {
        val baseDir = if (settings.keepDownloadedFiles && settings.customSaveDirectory.isNotBlank()) {
            settings.customSaveDirectory
        } else {
            NSTemporaryDirectory().trimEnd('/') + "/naph_p2p_cache"
        }
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = baseDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
        return baseDir
    }

    private fun buildMagnetUri(infoHash: String, displayName: String, sources: List<String>?): String {
        val trackers = buildList {
            sources?.forEach { src ->
                if (src.startsWith("tracker:")) {
                    add(src.removePrefix("tracker:"))
                } else if (src.startsWith("udp://") || src.startsWith("http://") || src.startsWith("https://")) {
                    add(src)
                }
            }
            addAll(defaultTrackers)
        }.distinct()

        val trParams = trackers.joinToString("") { "&tr=$it" }
        return "magnet:?xt=urn:btih:$infoHash&dn=${displayName.replace(" ", "+")}$trParams"
    }

    actual fun startNativeP2PStream(
        stream: MediaStream,
        settings: P2PSettings,
        onTelemetryUpdate: (P2PStreamTelemetry) -> Unit,
        onStreamReady: (String) -> Unit,
        onStreamError: (String) -> Unit
    ) {
        activeJob?.cancel()
        val hash = stream.infoHash?.lowercase()?.trim().orEmpty()
        if (hash.isBlank()) {
            val direct = stream.url.orEmpty()
            if (direct.isNotBlank()) onStreamReady(direct) else onStreamError("Stream missing infoHash and URL")
            return
        }

        activeInfoHash = hash
        val fileIdx = stream.fileIdx ?: 0
        val fileName = stream.cleanTitleLine
        val totalSize = stream.behaviorHints?.videoSize ?: (2_100L * 1024L * 1024L)
        val cacheDir = resolveIosCacheDirectory(settings)

        // 1. If user configured an external / local TorrServer daemon on iOS
        if (settings.externalTorrServerUrl.isNotBlank()) {
            val torrUrl = "${settings.externalTorrServerUrl.trimEnd('/')}/stream/${fileName.replace(" ", "_")}?link=$hash&index=$fileIdx&play"
            onTelemetryUpdate(
                P2PStreamTelemetry(
                    infoHash = hash,
                    fileName = fileName,
                    localStreamUrl = torrUrl,
                    isReadyForPlayback = true,
                    bufferProgressPercent = 100f,
                    totalFileBytes = totalSize,
                    headerPiecesReady = true
                )
            )
            onStreamReady(torrUrl)
            return
        }

        // 2. Native iOS C++20 libtorrent-rasterbar / NuvioEngine Bridge
        activeJob = scope.launch {
            val magnetUri = buildMagnetUri(hash, fileName, stream.sources)
            val bridge = nativeBridge
            if (bridge != null) {
                val localHttpUrl = bridge.startSequentialMagnetStream(
                    magnetUri = magnetUri,
                    infoHash = hash,
                    fileIndex = fileIdx,
                    saveDirectory = cacheDir,
                    maxDownloadKbps = settings.maxDownloadSpeedKbps,
                    maxUploadKbps = settings.maxUploadSpeedKbps,
                    enableDht = settings.enableDht
                )
                if (!localHttpUrl.isNullOrBlank()) {
                    var notifiedReady = false
                    while (isActive) {
                        val tel = bridge.querySwarmTelemetry(hash)
                        if (tel != null) {
                            onTelemetryUpdate(tel.copy(localStreamUrl = localHttpUrl))
                            if (!notifiedReady && (tel.isReadyForPlayback || tel.headerPiecesReady)) {
                                notifiedReady = true
                                onStreamReady(localHttpUrl)
                            }
                        } else if (!notifiedReady) {
                            notifiedReady = true
                            onStreamReady(localHttpUrl)
                        }
                        delay(750)
                    }
                    return@launch
                }
            }

            // 3. Fallback if stream also carries a direct HTTP / Debrid mirror URL
            if (!stream.url.isNullOrBlank()) {
                onStreamReady(stream.url)
            } else {
                onStreamError("Torrent streams on iOS need a TorrServer URL (Settings → Playback) or a Debrid account for now.")
            }
        }
    }

    actual fun stopNativeP2PStream(settings: P2PSettings): Long {
        activeJob?.cancel()
        activeJob = null

        val hash = activeInfoHash
        activeInfoHash = null
        val shouldDelete = settings.useTempCacheAutoClean && !settings.keepDownloadedFiles

        var freedBytes = 0L
        if (hash != null) {
            freedBytes += nativeBridge?.stopAndRemoveStream(hash, shouldDelete) ?: 0L
        }

        if (shouldDelete) {
            val cacheDir = NSTemporaryDirectory().trimEnd('/') + "/naph_p2p_cache"
            NSFileManager.defaultManager.removeItemAtPath(cacheDir, error = null)
        }
        return freedBytes
    }
}
