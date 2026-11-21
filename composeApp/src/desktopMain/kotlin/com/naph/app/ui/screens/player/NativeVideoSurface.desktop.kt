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
import java.lang.reflect.Field
import java.lang.reflect.Method

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
 * Windows & Linux Desktop Native Video Surface powered by real in-window `libmpv` embedding!
 */
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
    var mpvHandle by remember { mutableStateOf<Pointer?>(null) }
    var canvasReadyWid by remember { mutableStateOf(0L) }

    val hostCanvas = remember {
        object : Canvas() {
            init {
                val self = this
                background = java.awt.Color.BLACK
                ignoreRepaint = false
                addComponentListener(object : ComponentAdapter() {
                    override fun componentResized(e: ComponentEvent?) {
                        val wid = DesktopNativeViewResolver.resolveWindowId(self)
                        if (wid != 0L) canvasReadyWid = wid
                    }

                    override fun componentShown(e: ComponentEvent?) {
                        val wid = DesktopNativeViewResolver.resolveWindowId(self)
                        if (wid != 0L) canvasReadyWid = wid
                    }
                })
            }

            override fun addNotify() {
                super.addNotify()
                val wid = DesktopNativeViewResolver.resolveWindowId(this)
                if (wid != 0L) canvasReadyWid = wid
            }
        }
    }

    // Initialize libmpv and attach to the AWT Canvas X11 wid / Windows HWND
    LaunchedEffect(canvasReadyWid, streamUrl) {
        if (canvasReadyWid == 0L || streamUrl.isBlank()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val mpv = LibMpvJna.instance ?: return@withContext
            LibC.instance?.setlocale(LibC.LC_NUMERIC, "C")

            // Clean up any previous handle before opening new stream
            mpvHandle?.let { old ->
                runCatching { mpv.mpv_terminate_destroy(old) }
                mpvHandle = null
            }

            // Try x11egl first, fallback to x11vk / auto
            val contexts = listOf("x11egl", "x11vk", "auto")
            var initializedCtx: Pointer? = null

            for (gpuCtx in contexts) {
                val ctx = mpv.mpv_create() ?: continue
                mpv.mpv_set_option_string(ctx, "wid", canvasReadyWid.toString())
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
                mpv.mpv_set_option_string(ctx, "hwdec", "auto-safe")
                mpv.mpv_set_option_string(ctx, "vd-lavc-software-fallback", "yes")
                mpv.mpv_set_option_string(ctx, "force-seekable", "yes")
                // Nuvio-grade streaming & buffering tuning for smooth 4K/P2P playback
                mpv.mpv_set_option_string(ctx, "cache", "yes")
                mpv.mpv_set_option_string(ctx, "demuxer-max-bytes", "400M")
                mpv.mpv_set_option_string(ctx, "demuxer-readahead-secs", "60")
                mpv.mpv_set_option_string(ctx, "video-sync", "display-resample")

                if (mpv.mpv_initialize(ctx) >= 0) {
                    initializedCtx = ctx
                    break
                } else {
                    mpv.mpv_terminate_destroy(ctx)
                }
            }

            if (initializedCtx != null) {
                mpvHandle = initializedCtx
                mpv.mpv_command(initializedCtx, arrayOf("loadfile", streamUrl, null))
                mpv.mpv_set_property_string(initializedCtx, "pause", if (isPlaying) "no" else "yes")
            }
        }
    }

    // Sync Play / Pause
    LaunchedEffect(isPlaying, mpvHandle) {
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            mpv.mpv_set_property_string(ctx, "pause", if (isPlaying) "no" else "yes")
        }
    }

    // Sync Seek
    LaunchedEffect(seekToMs) {
        val targetMs = seekToMs ?: return@LaunchedEffect
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val sec = (targetMs.toDouble() / 1000.0).toString()
            mpv.mpv_command(ctx, arrayOf("seek", sec, "absolute", null))
        }
    }

    // Sync Subtitle Delay
    LaunchedEffect(subtitleDelayMs, mpvHandle) {
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val sec = (subtitleDelayMs.toDouble() / 1000.0).toString()
            mpv.mpv_set_property_string(ctx, "sub-delay", sec)
        }
    }

    // Sync Playback Speed
    LaunchedEffect(playbackSpeed, mpvHandle) {
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            mpv.mpv_set_property_string(ctx, "speed", playbackSpeed.toString())
        }
    }

    // Sync Volume (0..100)
    LaunchedEffect(volume, mpvHandle) {
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            mpv.mpv_set_property_string(ctx, "volume", (volume * 100f).toInt().coerceIn(0, 100).toString())
        }
    }

    // Load external subtitle URL from Stremio addons when selected
    LaunchedEffect(subtitleUrl, mpvHandle) {
        val sub = subtitleUrl?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            mpv.mpv_command(ctx, arrayOf("sub-add", sub, "select", null))
        }
    }

    // Poll real time-pos and duration from libmpv on background IO thread so UI thread is never blocked
    LaunchedEffect(mpvHandle) {
        val ctx = mpvHandle ?: return@LaunchedEffect
        val mpv = LibMpvJna.instance ?: return@LaunchedEffect
        val posRef = DoubleByReference()
        val durRef = DoubleByReference()
        while (isActive) {
            delay(500)
            val result = withContext(Dispatchers.IO) {
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
            val ctx = mpvHandle
            mpvHandle = null
            if (ctx != null) {
                Thread {
                    runCatching { LibMpvJna.instance?.mpv_terminate_destroy(ctx) }
                }.start()
            }
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
