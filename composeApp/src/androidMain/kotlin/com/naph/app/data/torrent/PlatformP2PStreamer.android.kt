package com.naph.app.data.torrent

import android.content.Context
import com.naph.app.core.model.MediaStream
import com.naph.app.core.model.P2PSettings
import com.naph.app.core.model.P2PStreamTelemetry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Android Native On-Device Sequential P2P BitTorrent Engine:
 * Launches the bundled Go `anacrolix/torrent` binary (`libnaph_p2p.so` in `nativeLibraryDir`)
 * which connects directly to DHT/UDP trackers and exposes a local HTTP Byte-Range stream for ExoPlayer!
 */
actual object PlatformP2PStreamer {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var appContext: Context? = null
    private var activeJob: Job? = null
    private var p2pProcess: Process? = null
    private var lastDownloadedBytes: Long = 0L

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun startNativeP2PStream(
        stream: MediaStream,
        settings: P2PSettings,
        onTelemetryUpdate: (P2PStreamTelemetry) -> Unit,
        onStreamReady: (String) -> Unit,
        onStreamError: (String) -> Unit
    ) {
        activeJob?.cancel()
        stopProcess()

        val infoHash = stream.infoHash?.trim()?.lowercase()
        if (infoHash.isNullOrBlank()) {
            val direct = stream.url.orEmpty()
            if (direct.isNotBlank()) onStreamReady(direct)
            else onStreamError("Stream is missing a valid BitTorrent infoHash")
            return
        }

        // External TorrServer override if user configured one
        if (settings.externalTorrServerUrl.isNotBlank()) {
            val fileName = stream.behaviorHints?.filename ?: "stream.mkv"
            val url = "${settings.externalTorrServerUrl.trimEnd('/')}/stream/$fileName?link=$infoHash&index=${stream.fileIdx ?: 0}&play"
            onStreamReady(url)
            return
        }

        val ctx = appContext ?: run {
            onStreamError("Android P2P engine context not initialized")
            return
        }

        activeJob = scope.launch {
            try {
                val p2pBin = File(ctx.applicationInfo.nativeLibraryDir, "libnaph_p2p.so")
                if (!p2pBin.exists()) {
                    onStreamError("Embedded Android P2P binary (libnaph_p2p.so) missing")
                    return@launch
                }

                val cacheDir = File(ctx.cacheDir, "naph_p2p_cache").apply { mkdirs() }
                onTelemetryUpdate(
                    P2PStreamTelemetry(
                        infoHash = infoHash,
                        fileName = stream.behaviorHints?.filename ?: stream.displayDetails.take(48),
                        localStreamUrl = "",
                        isReadyForPlayback = false,
                        bufferProgressPercent = 5f,
                        connectedPeers = 0,
                        totalSeeds = stream.parsedSeeders ?: 0
                    )
                )

                val extraTrackers = stream.sources
                    .filter { it.startsWith("tracker:") }
                    .map { it.removePrefix("tracker:").trim() }
                    .filter { it.isNotBlank() }
                    .joinToString(",")

                val cmd = mutableListOf(
                    p2pBin.absolutePath,
                    "-hash", infoHash,
                    "-fileIdx", (stream.fileIdx ?: -1).toString(),
                    "-cacheDir", cacheDir.absolutePath
                )
                if (extraTrackers.isNotBlank()) {
                    cmd += listOf("-trackers", extraTrackers)
                }

                val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
                p2pProcess = proc

                var readyUrl = ""
                var readyFileName = stream.behaviorHints?.filename ?: "stream.mkv"
                var readyFileSize = 0L

                proc.inputStream.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        if (!isActive) break
                        when {
                            line.startsWith("READY|") -> {
                                val parts = line.split("|")
                                readyUrl = parts.getOrNull(1).orEmpty()
                                readyFileName = parts.getOrNull(2).orEmpty().ifBlank { readyFileName }
                                readyFileSize = parts.getOrNull(3)?.toLongOrNull() ?: 0L
                                if (readyUrl.isNotBlank()) {
                                    onStreamReady(readyUrl)
                                }
                            }
                            line.startsWith("STATS|") -> {
                                val parts = line.split("|")
                                val peers = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                val seeds = parts.getOrNull(2)?.toIntOrNull() ?: 0
                                val downBps = parts.getOrNull(3)?.toLongOrNull() ?: 0L
                                val completed = parts.getOrNull(4)?.toLongOrNull() ?: 0L
                                val total = parts.getOrNull(5)?.toLongOrNull() ?: readyFileSize
                                lastDownloadedBytes = completed
                                val pct = if (total > 0L) ((completed.toDouble() / total.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f) else 0f
                                onTelemetryUpdate(
                                    P2PStreamTelemetry(
                                        infoHash = infoHash,
                                        fileName = readyFileName,
                                        localStreamUrl = readyUrl,
                                        isReadyForPlayback = readyUrl.isNotBlank(),
                                        bufferProgressPercent = pct,
                                        downloadSpeedBytesPerSec = downBps,
                                        connectedPeers = peers,
                                        totalSeeds = seeds.coerceAtLeast(stream.parsedSeeders ?: 0),
                                        downloadedBytes = completed,
                                        totalFileBytes = total,
                                        headerPiecesReady = readyUrl.isNotBlank()
                                    )
                                )
                            }
                            line.startsWith("ERROR|") -> {
                                onStreamError("P2P Engine Error: ${line.removePrefix("ERROR|")}")
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                onStreamError("P2P Engine Error: ${e.message ?: "Failed to start Android P2P engine"}")
            }
        }
    }

    private fun stopProcess() {
        val p = p2pProcess
        p2pProcess = null
        if (p != null) {
            runCatching { p.destroyForcibly() }
        }
    }

    actual fun stopNativeP2PStream(settings: P2PSettings): Long {
        activeJob?.cancel()
        activeJob = null
        stopProcess()
        val cleared = lastDownloadedBytes
        lastDownloadedBytes = 0L
        if (settings.useTempCacheAutoClean && !settings.keepDownloadedFiles) {
            runCatching {
                appContext?.cacheDir?.let { File(it, "naph_p2p_cache").deleteRecursively() }
            }
        }
        return cleared
    }
}
