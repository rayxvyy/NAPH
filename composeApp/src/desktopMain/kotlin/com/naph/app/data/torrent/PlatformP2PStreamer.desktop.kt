package com.naph.app.data.torrent

import com.naph.app.core.model.MediaStream
import com.naph.app.core.model.P2PSettings
import com.naph.app.core.model.P2PStreamTelemetry
import com.nuvio.engine.NuvioEngine
import com.nuvio.engine.NuvioEngineConfig
import com.nuvio.engine.NuvioEngineRuntime
import com.nuvio.engine.NuvioTorrentProfile
import com.nuvio.engine.NuvioUploadMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Desktop Real C++20 libtorrent Sequential P2P Streaming Engine (`libnuvio_engine.so` / `nuvio_engine.dll`)
 *
 * Connects to live BitTorrent DHT & UDP trackers, downloads video pieces sequentially,
 * and spins up a real local HTTP Byte-Range stream server (`http://127.0.0.1:<port>/...`)
 * that `libmpv` plays directly!
 */
actual object PlatformP2PStreamer {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var activeJob: Job? = null
    private var engine: NuvioEngine? = null
    private var windowsP2pProcess: Process? = null
    private var currentStreamId: String? = null
    private var lastDownloadedBytes: Long = 0L

    private val defaultTrackers = listOf(
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://open.demonii.com:1337/announce",
        "udp://open.stealth.si:80/announce",
        "udp://tracker.torrent.eu.org:451/announce",
        "udp://exodus.desync.com:6969/announce",
        "udp://tracker.publictracker.xyz:6969/announce",
        "udp://zer0day.ch:1337/announce",
        "http://tracker.renfei.net:8080/announce"
    )

    private fun resolveNativeLibraryFile(): File {
        val osName = System.getProperty("os.name").orEmpty().lowercase()
        val (dir, libName) = when {
            osName.contains("win") -> "windows" to "nuvio_engine.dll"
            osName.contains("mac") -> "macos" to "libnuvio_engine.dylib"
            else -> "linux" to "libnuvio_engine.so"
        }

        val candidates = listOf(
            File("composeApp/src/desktopMain/resources/native/$dir/$libName"),
            File("src/desktopMain/resources/native/$dir/$libName"),
            File("/tmp/NuvioDesktop/composeApp/src/desktopMain/native/$dir/engine/$libName")
        )
        candidates.firstOrNull { it.isFile }?.let { return it }

        val resourceUrl = PlatformP2PStreamer::class.java.getResource("/native/$dir/$libName")
            ?: error("Native P2P library /native/$dir/$libName not found")
        val tempDir = File(System.getProperty("java.io.tmpdir"), "naph-p2p-native/$dir").apply { mkdirs() }
        val extracted = File(tempDir, libName)
        if (!extracted.exists() || extracted.length() == 0L) {
            resourceUrl.openStream().use { input ->
                extracted.outputStream().use { output -> input.copyTo(output) }
            }
            extracted.setExecutable(true)
        }
        return extracted
    }

    private fun getOrCreateEngine(settings: P2PSettings): NuvioEngine {
        engine?.let { return it }
        val libFile = resolveNativeLibraryFile()
        val runtime = NuvioEngineRuntime.load(libFile)
        val baseDir = if (settings.keepDownloadedFiles && settings.customSaveDirectory.isNotBlank()) {
            File(settings.customSaveDirectory)
        } else {
            File(System.getProperty("java.io.tmpdir"), "naph-p2p-cache")
        }
        val stateDir = File(baseDir, "state").apply { mkdirs() }
        val cacheDir = File(baseDir, "cache").apply { mkdirs() }

        val created = runtime.create(
            NuvioEngineConfig(
                dataDirectory = stateDir,
                cacheDirectory = cacheDir,
                diskCacheCapacityBytes = settings.maxCacheSizeGb.toLong() * 1024L * 1024L * 1024L,
                torrentProfile = NuvioTorrentProfile.Fast,
                uploadMode = if (settings.maxUploadSpeedKbps > 0) NuvioUploadMode.Unlimited else NuvioUploadMode.Disabled,
                streamInactivityTimeoutMilliseconds = 0
            )
        )
        engine = created
        return created
    }

    actual fun startNativeP2PStream(
        stream: MediaStream,
        settings: P2PSettings,
        onTelemetryUpdate: (P2PStreamTelemetry) -> Unit,
        onStreamReady: (String) -> Unit,
        onStreamError: (String) -> Unit
    ) {
        activeJob?.cancel()
        val infoHash = stream.infoHash?.trim()?.lowercase() ?: run {
            onStreamError("Stream is missing a valid BitTorrent infoHash")
            return
        }

        // External TorrServer override if user configured one
        if (settings.externalTorrServerUrl.isNotBlank()) {
            val fileName = stream.behaviorHints?.filename ?: "stream.mkv"
            val url = "${settings.externalTorrServerUrl.trimEnd('/')}/stream/$fileName?link=$infoHash&index=${stream.fileIdx ?: 0}&play"
            onStreamReady(url)
            return
        }

        val isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("win")
        if (isWindows) {
            startWindowsBundledGoP2P(infoHash, stream, settings, onTelemetryUpdate, onStreamReady, onStreamError)
            return
        }

        activeJob = scope.launch {
            try {
                val activeEngine = getOrCreateEngine(settings)
                currentStreamId?.let { oldId ->
                    runCatching { activeEngine.stopStream(oldId) }
                    currentStreamId = null
                }

                // Publish initial connecting telemetry
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
                    ?.filter { it.startsWith("tracker:") }
                    ?.map { it.removePrefix("tracker:") }
                    .orEmpty()
                val allTrackers = (defaultTrackers + extraTrackers).distinct()
                val magnetUri = buildString {
                    append("magnet:?xt=urn:btih:")
                    append(infoHash)
                    allTrackers.forEach { tr -> append("&tr=").append(java.net.URLEncoder.encode(tr, "UTF-8")) }
                }

                val torrentId = activeEngine.addMagnet(magnetUri)
                val prepared = activeEngine.prepareStream(
                    torrentId = torrentId,
                    fileIndex = stream.fileIdx,
                    filenameHint = stream.behaviorHints?.filename
                )
                currentStreamId = prepared.id

                // Local HTTP Range URL is ready for libmpv!
                onStreamReady(prepared.url)

                while (isActive) {
                    val engineStats = activeEngine.stats.value
                    val routeStats = runCatching { activeEngine.currentStreamStats(prepared.id) }.getOrNull()
                    lastDownloadedBytes = engineStats.totalPayloadDownloadBytes

                    onTelemetryUpdate(
                        P2PStreamTelemetry(
                            infoHash = infoHash,
                            fileName = stream.behaviorHints?.filename ?: "Stream #${prepared.fileIndex}",
                            localStreamUrl = prepared.url,
                            isReadyForPlayback = true,
                            bufferProgressPercent = ((routeStats?.bufferProgress ?: 1f) * 100f).coerceIn(0f, 100f),
                            downloadSpeedBytesPerSec = engineStats.downloadRateBytesPerSecond,
                            uploadSpeedBytesPerSec = engineStats.uploadRateBytesPerSecond,
                            connectedPeers = engineStats.connectedPeers,
                            totalSeeds = engineStats.connectedSeeds.coerceAtLeast(stream.parsedSeeders ?: 0),
                            downloadedBytes = routeStats?.verifiedFileBytes ?: engineStats.totalPayloadDownloadBytes,
                            totalFileBytes = prepared.fileSize,
                            headerPiecesReady = true
                        )
                    )
                    delay(400)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                onStreamError("P2P Engine Error: ${e.message ?: "Failed to connect to BitTorrent swarm"}")
            }
        }
    }

    private fun extractWindowsP2pExecutable(): File? = runCatching {
        val stream = PlatformP2PStreamer::class.java.classLoader?.getResourceAsStream("native/windows/naph_p2p_win.exe")
            ?: return@runCatching null
        val outDir = File(System.getProperty("user.home"), ".naph_data/native").apply { mkdirs() }
        val outFile = File(outDir, "naph_p2p_win.exe")
        if (!outFile.exists() || outFile.length() == 0L) {
            stream.use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
        } else {
            stream.close()
        }
        outFile
    }.getOrNull()

    private fun startWindowsBundledGoP2P(
        infoHash: String,
        stream: MediaStream,
        settings: P2PSettings,
        onTelemetryUpdate: (P2PStreamTelemetry) -> Unit,
        onStreamReady: (String) -> Unit,
        onStreamError: (String) -> Unit
    ) {
        stopWindowsProcess()
        activeJob = scope.launch {
            try {
                val exe = extractWindowsP2pExecutable()
                    ?: throw IllegalStateException("Bundled Windows P2P engine (naph_p2p_win.exe) not found")
                val cacheDir = File(System.getProperty("java.io.tmpdir"), "naph_p2p_cache").apply { mkdirs() }
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

                val proc = ProcessBuilder(
                    exe.absolutePath,
                    "-hash", infoHash,
                    "-fileIdx", (stream.fileIdx ?: -1).toString(),
                    "-cacheDir", cacheDir.absolutePath
                ).redirectErrorStream(true).start()
                windowsP2pProcess = proc

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
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                onStreamError("P2P Engine Error: ${e.message ?: "Failed to start Windows P2P engine"}")
            }
        }
    }

    private fun stopWindowsProcess() {
        val p = windowsP2pProcess
        windowsP2pProcess = null
        if (p != null) {
            runCatching { p.destroyForcibly() }
        }
    }

    actual fun stopNativeP2PStream(settings: P2PSettings): Long {
        activeJob?.cancel()
        activeJob = null
        stopWindowsProcess()
        val cleared = lastDownloadedBytes
        lastDownloadedBytes = 0L

        val sid = currentStreamId
        currentStreamId = null
        val eng = engine
        if (eng != null) {
            scope.launch {
                if (sid != null) {
                    runCatching { eng.stopStream(sid) }
                }
                if (settings.useTempCacheAutoClean && !settings.keepDownloadedFiles) {
                    runCatching { eng.reclaimDiskCache(0L) }
                }
            }
        }
        return cleared
    }
}
