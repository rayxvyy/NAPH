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
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.DoubleByReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.awt.Canvas
import java.awt.Component
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Window fullscreen & AWT canvas pointer bridge wired from `Main.kt` and `UnifiedVideoPlayerScreen.kt`.
 */
object DesktopWindowController {
    var onToggleFullscreen: (() -> Boolean)? = null
    var onExitFullscreen: (() -> Unit)? = null
    var onCanvasMouseMove: (() -> Unit)? = null
    var onCanvasDoubleClick: (() -> Unit)? = null
}

/**
 * Direct JNA binding to system `libmpv` (`libmpv.so.2` on Linux, `libmpv-2.dll` on Windows, `libmpv.2.dylib` on macOS)
 */
internal interface LibMpvJna : Library {
    fun mpv_create(): Pointer?
    fun mpv_initialize(ctx: Pointer): Int
    fun mpv_terminate_destroy(ctx: Pointer)
    fun mpv_command(ctx: Pointer, args: Array<String?>): Int
    fun mpv_set_option_string(ctx: Pointer, name: String, data: String): Int
    fun mpv_set_property_string(ctx: Pointer, name: String, data: String): Int
    fun mpv_get_property(ctx: Pointer, name: String, format: Int, data: DoubleByReference): Int

    companion object {
        const val MPV_FORMAT_DOUBLE = 5

        val instance: LibMpvJna? by lazy {
            val candidates = listOf("mpv", "libmpv.so.2", "libmpv-2", "libmpv.2.dylib")
            for (name in candidates) {
                val loaded = runCatching {
                    Native.load(name, LibMpvJna::class.java)
                }.getOrNull()
                if (loaded != null) return@lazy loaded
            }
            null
        }
    }
}

internal interface LibC : Library {
    fun setlocale(category: Int, locale: String): String?

    companion object {
        const val LC_NUMERIC = 1
        val instance: LibC? by lazy {
            runCatching { Native.load("c", LibC::class.java) }.getOrNull()
        }
    }
}

/**
 * Process-wide synchronized guard so ONLY ONE `libmpv` instance can ever exist at a time.
 * Completely prevents leaked background instances and double-audio playback!
 */
internal object ActiveDesktopMpvManager {
    private var activeHandle: Pointer? = null
    private var activeWid: Long = 0L

    @Synchronized
    fun destroyCurrent() {
        val old = activeHandle
        activeHandle = null
        activeWid = 0L
        if (old != null) {
            runCatching { LibMpvJna.instance?.mpv_terminate_destroy(old) }
        }
    }

    @Synchronized
    fun getOrCreateForWid(wid: Long): Pointer? {
        val mpv = LibMpvJna.instance ?: return null
        LibC.instance?.setlocale(LibC.LC_NUMERIC, "C")

        // Reuse existing live handle if same AWT Canvas wid (zero duplicate audio!)
        if (activeHandle != null && activeWid == wid) {
            return activeHandle
        }

        // Synchronously terminate any prior handle before creating a new one
        destroyCurrent()

        val os = System.getProperty("os.name").orEmpty().lowercase()
        val contexts = if (os.contains("win")) {
            listOf("d3d11", "winvk", "auto")
        } else {
            listOf("x11egl", "x11vk", "auto")
        }

        for (gpuCtx in contexts) {
            val ctx = mpv.mpv_create() ?: continue
            mpv.mpv_set_option_string(ctx, "wid", wid.toString())
            mpv.mpv_set_option_string(ctx, "osc", "no")
            mpv.mpv_set_option_string(ctx, "osd-level", "0")
            mpv.mpv_set_option_string(ctx, "input-default-bindings", "no")
            mpv.mpv_set_option_string(ctx, "input-vo-keyboard", "no")
            mpv.mpv_set_option_string(ctx, "keep-open", "yes")
            mpv.mpv_set_option_string(ctx, "idle", "yes")
            mpv.mpv_set_option_string(ctx, "vo", "gpu")
            mpv.mpv_set_option_string(ctx, "force-window", "immediate")
            if (gpuCtx != "auto") {
                mpv.mpv_set_option_string(ctx, "gpu-context", gpuCtx)
            }
            // Full GPU hardware decoding (VAAPI / NVDEC on Linux, D3D11VA / DXVA2 on Windows)
            mpv.mpv_set_option_string(ctx, "hwdec", "auto")
            mpv.mpv_set_option_string(ctx, "hwdec-codecs", "all")
            mpv.mpv_set_option_string(ctx, "vd-lavc-software-fallback", "yes")
            // Crisp scaling & smooth audio-synced playback (no X11 wid display-resample stutter)
            mpv.mpv_set_option_string(ctx, "video-sync", "audio")
            mpv.mpv_set_option_string(ctx, "scale", "spline36")
            mpv.mpv_set_option_string(ctx, "cscale", "spline36")
            mpv.mpv_set_option_string(ctx, "dscale", "mitchell")
            mpv.mpv_set_option_string(ctx, "force-seekable", "yes")
            mpv.mpv_set_option_string(ctx, "cache", "yes")
            mpv.mpv_set_option_string(ctx, "demuxer-max-bytes", "400M")
            mpv.mpv_set_option_string(ctx, "demuxer-readahead-secs", "60")
            mpv.mpv_set_option_string(ctx, "volume-max", "200")
            mpv.mpv_set_option_string(ctx, "sub-auto", "fuzzy")

            if (mpv.mpv_initialize(ctx) >= 0) {
                activeHandle = ctx
                activeWid = wid
                return ctx
            } else {
                mpv.mpv_terminate_destroy(ctx)
            }
        }
        return null
    }

    @Synchronized
    fun currentHandle(): Pointer? = activeHandle
}

/**
 * Resolves the native X11 `wid` (Linux) or `HWND` (Windows) from an AWT Heavyweight Canvas
 * so `libmpv` renders video frames directly inside the Compose window!
 */
internal object DesktopNativeViewResolver {
    private val peerField: Field by lazy {
        Component::class.java.getDeclaredField("peer").apply { isAccessible = true }
    }

    fun resolveWindowId(component: Component): Long = runCatching {
        val peer = peerField.get(component) ?: return 0L
        val os = System.getProperty("os.name").orEmpty().lowercase()
        return when {
            os.contains("win") -> (findMethod(peer.javaClass, "getHWnd")?.invoke(peer) as? Number)?.toLong() ?: 0L
            os.contains("linux") -> (findMethod(peer.javaClass, "getWindow")?.invoke(peer) as? Number)?.toLong() ?: 0L
            else -> 0L
        }
    }.getOrDefault(0L)

    private fun findMethod(clazz: Class<*>, name: String): Method? {
        var current: Class<*>? = clazz
        while (current != null) {
            try {
                return current.getDeclaredMethod(name).apply { isAccessible = true }
            } catch (_: NoSuchMethodException) {
                current = current.superclass
            }
        }
        return null
    }
}

/**
 * Windows & Linux Desktop Native Video Surface powered by real hardware-accelerated `libmpv` embedding!
 */
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
    var mpvHandle by remember { mutableStateOf<Pointer?>(null) }
    var canvasReadyWid by remember { mutableStateOf(0L) }
    var loadedStreamUrl by remember { mutableStateOf("") }

    val hostCanvas = remember {
        object : Canvas() {
            init {
                val self = this
                background = java.awt.Color.BLACK
                ignoreRepaint = false
                addComponentListener(object : ComponentAdapter() {
                    override fun componentResized(e: ComponentEvent?) {
                        val wid = DesktopNativeViewResolver.resolveWindowId(self)
                        if (wid != 0L && wid != canvasReadyWid) canvasReadyWid = wid
                    }

                    override fun componentShown(e: ComponentEvent?) {
                        val wid = DesktopNativeViewResolver.resolveWindowId(self)
                        if (wid != 0L && wid != canvasReadyWid) canvasReadyWid = wid
                    }
                })
                addMouseMotionListener(object : MouseMotionAdapter() {
                    override fun mouseMoved(e: MouseEvent?) {
                        DesktopWindowController.onCanvasMouseMove?.invoke()
                    }
                })
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        if (e != null && e.clickCount >= 2) {
                            DesktopWindowController.onCanvasDoubleClick?.invoke()
                        } else {
                            DesktopWindowController.onCanvasMouseMove?.invoke()
                        }
                    }
                })
            }

            override fun addNotify() {
                super.addNotify()
                val wid = DesktopNativeViewResolver.resolveWindowId(this)
                if (wid != 0L && wid != canvasReadyWid) canvasReadyWid = wid
            }
        }
    }

    // Initialize single libmpv handle & load stream cleanly (prevents double-audio!)
    LaunchedEffect(canvasReadyWid, streamUrl) {
        if (canvasReadyWid == 0L || streamUrl.isBlank()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val mpv = LibMpvJna.instance ?: return@withContext
            val ctx = ActiveDesktopMpvManager.getOrCreateForWid(canvasReadyWid) ?: return@withContext
            mpvHandle = ctx
            if (loadedStreamUrl != streamUrl) {
                loadedStreamUrl = streamUrl
                mpv.mpv_command(ctx, arrayOf("loadfile", streamUrl, "replace", null))
                mpv.mpv_set_property_string(ctx, "pause", if (isPlaying) "no" else "yes")
            }
        }
    }

    // Sync Play / Pause
    LaunchedEffect(isPlaying, mpvHandle) {
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            if (ActiveDesktopMpvManager.currentHandle() === ctx) {
                mpv.mpv_set_property_string(ctx, "pause", if (isPlaying) "no" else "yes")
            }
        }
    }

    // Sync Seek
    LaunchedEffect(seekToMs) {
        val targetMs = seekToMs ?: return@LaunchedEffect
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            if (ActiveDesktopMpvManager.currentHandle() === ctx) {
                val sec = (targetMs.toDouble() / 1000.0).toString()
                mpv.mpv_command(ctx, arrayOf("seek", sec, "absolute", null))
            }
        }
    }

    // Sync Subtitle Delay
    LaunchedEffect(subtitleDelayMs, mpvHandle) {
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            if (ActiveDesktopMpvManager.currentHandle() === ctx) {
                val sec = (subtitleDelayMs.toDouble() / 1000.0).toString()
                mpv.mpv_set_property_string(ctx, "sub-delay", sec)
            }
        }
    }

    // Sync Playback Speed
    LaunchedEffect(playbackSpeed, mpvHandle) {
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            if (ActiveDesktopMpvManager.currentHandle() === ctx) {
                mpv.mpv_set_property_string(ctx, "speed", playbackSpeed.toString())
            }
        }
    }

    // Sync Volume (0..150% with Boost)
    LaunchedEffect(volume, mpvHandle) {
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            if (ActiveDesktopMpvManager.currentHandle() === ctx) {
                mpv.mpv_set_property_string(ctx, "volume", (volume * 100f).toInt().coerceIn(0, 150).toString())
            }
        }
    }

    // Sync Aspect Ratio / Zoom Mode (fit, fill, 16:9, stretch)
    LaunchedEffect(aspectMode, mpvHandle) {
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            if (ActiveDesktopMpvManager.currentHandle() !== ctx) return@withContext
            when (aspectMode.lowercase()) {
                "fill" -> {
                    mpv.mpv_set_property_string(ctx, "keepaspect", "yes")
                    mpv.mpv_set_property_string(ctx, "video-aspect-override", "-1")
                    mpv.mpv_set_property_string(ctx, "panscan", "1.0")
                }
                "16:9" -> {
                    mpv.mpv_set_property_string(ctx, "keepaspect", "yes")
                    mpv.mpv_set_property_string(ctx, "panscan", "0.0")
                    mpv.mpv_set_property_string(ctx, "video-aspect-override", "16:9")
                }
                "stretch" -> {
                    mpv.mpv_set_property_string(ctx, "keepaspect", "no")
                    mpv.mpv_set_property_string(ctx, "panscan", "0.0")
                    mpv.mpv_set_property_string(ctx, "video-aspect-override", "-1")
                }
                else -> {
                    mpv.mpv_set_property_string(ctx, "keepaspect", "yes")
                    mpv.mpv_set_property_string(ctx, "panscan", "0.0")
                    mpv.mpv_set_property_string(ctx, "video-aspect-override", "-1")
                }
            }
        }
    }

    // Sync Subtitle Visibility & Font Size
    LaunchedEffect(subtitlesEnabled, subtitleFontSizeSp, mpvHandle) {
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            if (ActiveDesktopMpvManager.currentHandle() === ctx) {
                mpv.mpv_set_property_string(ctx, "sub-visibility", if (subtitlesEnabled) "yes" else "no")
                val mpvFontSize = (subtitleFontSizeSp * 2.2f).toInt().coerceIn(24, 90)
                mpv.mpv_set_property_string(ctx, "sub-font-size", mpvFontSize.toString())
            }
        }
    }

    // Cycle Embedded Audio Tracks
    LaunchedEffect(cycleAudioTick) {
        if (cycleAudioTick <= 0) return@LaunchedEffect
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            if (ActiveDesktopMpvManager.currentHandle() === ctx) {
                mpv.mpv_command(ctx, arrayOf("cycle", "audio", null))
            }
        }
    }

    // Cycle Embedded Subtitle Tracks
    LaunchedEffect(cycleEmbeddedSubTick) {
        if (cycleEmbeddedSubTick <= 0) return@LaunchedEffect
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            if (ActiveDesktopMpvManager.currentHandle() === ctx) {
                mpv.mpv_set_property_string(ctx, "sub-visibility", "yes")
                mpv.mpv_command(ctx, arrayOf("cycle", "sub", null))
            }
        }
    }

    // Load external subtitle URL from Stremio addons when selected
    LaunchedEffect(subtitleUrl, mpvHandle) {
        val sub = subtitleUrl?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            if (ActiveDesktopMpvManager.currentHandle() === ctx) {
                mpv.mpv_set_property_string(ctx, "sub-visibility", "yes")
                mpv.mpv_command(ctx, arrayOf("sub-add", sub, "select", null))
            }
        }
    }

    // Poll real time-pos and duration from libmpv
    LaunchedEffect(mpvHandle) {
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        val posRef = DoubleByReference()
        val durRef = DoubleByReference()
        while (isActive) {
            delay(400)
            val result = withContext(Dispatchers.IO) {
                if (ActiveDesktopMpvManager.currentHandle() !== ctx) return@withContext null
                val posOk = mpv.mpv_get_property(ctx, "time-pos", LibMpvJna.MPV_FORMAT_DOUBLE, posRef) >= 0
                val durOk = mpv.mpv_get_property(ctx, "duration", LibMpvJna.MPV_FORMAT_DOUBLE, durRef) >= 0
                if (posOk && durOk && durRef.value > 0.0) {
                    val p = (posRef.value * 1000.0).toLong().coerceAtLeast(0L)
                    val d = (durRef.value * 1000.0).toLong().coerceAtLeast(1L)
                    p to d
                } else {
                    null
                }
            }
            if (result != null) {
                onPositionChanged(result.first, result.second)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mpvHandle = null
            ActiveDesktopMpvManager.destroyCurrent()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        SwingPanel(
            background = Color.Black,
            factory = { hostCanvas },
            modifier = Modifier.fillMaxSize()
        )
    }
}

actual val isDesktopAwtHost: Boolean = true

actual fun toggleNativeFullscreen(): Boolean {
    return DesktopWindowController.onToggleFullscreen?.invoke() ?: false
}

actual fun exitNativeFullscreen() {
    DesktopWindowController.onExitFullscreen?.invoke()
}

actual fun registerDesktopVideoInteraction(
    onMouseMove: (() -> Unit)?,
    onDoubleClick: (() -> Unit)?
) {
    DesktopWindowController.onCanvasMouseMove = onMouseMove
    DesktopWindowController.onCanvasDoubleClick = onDoubleClick
}

actual fun launchExternalPlayer(playerName: String, streamUrl: String, title: String): Boolean {
    return runCatching {
        val cmd = when (playerName.lowercase()) {
            "mpv" -> listOf("mpv", "--force-media-title=$title", streamUrl)
            "vlc" -> listOf("vlc", "--meta-title=$title", streamUrl)
            "iina" -> listOf("iina", streamUrl)
            else -> listOf("xdg-open", streamUrl)
        }
        ProcessBuilder(cmd).start()
        true
    }.getOrDefault(false)
}
