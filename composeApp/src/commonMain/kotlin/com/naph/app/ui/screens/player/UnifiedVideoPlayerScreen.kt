package com.naph.app.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.naph.app.core.model.SkipSegment
import com.naph.app.core.model.SkipSegmentType
import com.naph.app.core.model.SubtitleTrack
import com.naph.app.ui.components.ModernFilterPill
import com.naph.app.ui.components.ModernGlassButton
import com.naph.app.ui.components.ModernPrimaryButton
import com.naph.app.ui.components.NaphSwitch
import com.naph.app.ui.state.NaphAppState
import com.naph.app.ui.theme.NaphColors
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class PlayerSheet {
    NONE,
    SUBTITLES,
    AUDIO,
    SPEED,
    EXTERNAL,
    CAST
}

private enum class DragGestureAxis {
    NONE,
    HORIZONTAL_SEEK,
    VERTICAL_BRIGHTNESS,
    VERTICAL_VOLUME
}

private val TopScrimBrush = Brush.verticalGradient(
    colors = listOf(Color.Black.copy(alpha = 0.82f), Color.Black.copy(alpha = 0.25f), Color.Transparent)
)

private val BottomScrimBrush = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f), Color.Black.copy(alpha = 0.92f))
)

/**
 * Plezy-Style Minimal Cinema Player + Moonfin Gestures + 150% Volume Boost:
 * - Clean minimal bottom bar (slim scrubber, timestamps, Play/Pause, -10s/+10s, Next Ep, Boost, Speed, Aspect, Audio, Subtitles)
 * - Moonfin touch gestures on mobile: single-tap HUD toggle, double-tap left/right ±10s, horizontal scrub,
 *   vertical swipe left=Brightness / right=Volume (0-150% Boost), long-press=temporary 2x speed
 * - All modal sheets (Subtitles, Audio, Speed, External Player, Cast) render inside `Dialog(...)` so they
 *   ALWAYS appear cleanly above heavyweight `SwingPanel` (Linux/Windows) and `UIKitView` (iOS)
 */
@Composable
fun UnifiedVideoPlayerScreen(
    appState: NaphAppState,
    modifier: Modifier = Modifier
) {
    val item by appState.activePlaybackItem.collectAsState()
    val episode by appState.activePlaybackEpisode.collectAsState()
    val stream by appState.activeStream.collectAsState()
    val playableUrl by appState.activePlayableUrl.collectAsState()
    val p2pTelemetry by appState.p2pTelemetry.collectAsState()
    val skipSegments by appState.activeSkipSegments.collectAsState()
    val activeProfile by appState.activeProfile.collectAsState()
    val subtitles by appState.availableSubtitles.collectAsState()
    val selectedSubtitle by appState.selectedSubtitleTrack.collectAsState()
    val subConfig by appState.subtitleConfig.collectAsState()

    val media = item ?: return
    val currentStream = stream ?: return

    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var requestedSeekMs by remember { mutableStateOf<Long?>(null) }

    // Playback speed & long-press 2x
    var baseSpeed by remember { mutableStateOf(1.0f) }
    var isTemp2x by remember { mutableStateOf(false) }
    val effectiveSpeed = if (isTemp2x) 2.0f else baseSpeed

    // Volume (0.0f .. 1.5f for 150% Boost) & Brightness (0.15f .. 1.0f)
    var volume by remember { mutableStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }
    var brightness by remember { mutableStateOf(1.0f) }
    val effectiveVolume = if (isMuted) 0f else volume

    // Aspect ratio mode & track cycling
    val aspectModes = remember { listOf("fit", "fill", "16:9", "stretch") }
    var aspectIndex by remember { mutableStateOf(0) }
    val aspectMode = aspectModes[aspectIndex]

    var subtitlesEnabled by remember { mutableStateOf(true) }
    var cycleAudioTick by remember { mutableStateOf(0) }
    var cycleEmbeddedSubTick by remember { mutableStateOf(0) }

    // Auto-hiding HUD & active Dialog sheet
    var showControls by remember { mutableStateOf(true) }
    var hudInteractionTick by remember { mutableStateOf(0) }
    var activeSheet by remember { mutableStateOf(PlayerSheet.NONE) }

    // Gesture visual HUD feedback
    var gestureHudLabel by remember { mutableStateOf<String?>(null) }
    var gestureHudProgress by remember { mutableStateOf<Float?>(null) }
    var gestureHudIcon by remember { mutableStateOf<ImageVector?>(null) }
    var gestureHudTick by remember { mutableStateOf(0) }
    var seekPreviewMs by remember { mutableStateOf<Long?>(null) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    // Auto-hide controls after 3.5s of inactivity while playing (on touch/overlay mode)
    LaunchedEffect(showControls, isPlaying, activeSheet, hudInteractionTick) {
        if (showControls && isPlaying && activeSheet == PlayerSheet.NONE && !isDesktopAwtHost) {
            delay(3500)
            showControls = false
        }
    }

    // Auto-clear transient gesture HUD pill
    LaunchedEffect(gestureHudTick) {
        if (gestureHudLabel != null && seekPreviewMs == null && !isTemp2x) {
            delay(1100)
            gestureHudLabel = null
            gestureHudProgress = null
            gestureHudIcon = null
        }
    }

    fun triggerHudFeedback(label: String, progress: Float? = null, icon: ImageVector? = null) {
        gestureHudLabel = label
        gestureHudProgress = progress
        gestureHudIcon = icon
        gestureHudTick++
    }

    fun performRelativeSeek(deltaMs: Long) {
        val base = seekPreviewMs ?: positionMs
        val maxDur = if (durationMs > 0L) durationMs else Long.MAX_VALUE
        val target = (base + deltaMs).coerceIn(0L, maxDur)
        positionMs = target
        requestedSeekMs = target
        hudInteractionTick++
        val sign = if (deltaMs >= 0) "+" else "-"
        triggerHudFeedback(
            label = "${sign}${abs(deltaMs) / 1000}s",
            icon = if (deltaMs >= 0) Icons.Default.FastForward else Icons.Default.FastRewind
        )
    }

    fun cycleVolumeBoost() {
        isMuted = false
        volume = when {
            volume < 1.1f -> 1.25f
            volume < 1.4f -> 1.50f
            else -> 1.0f
        }
        val pct = (volume * 100f).roundToInt()
        triggerHudFeedback(
            label = if (pct > 100) "Boost $pct%" else "Volume $pct%",
            progress = (volume / 1.5f).coerceIn(0f, 1f),
            icon = Icons.AutoMirrored.Filled.VolumeUp
        )
    }

    val activeSkipSegment: SkipSegment? = remember(positionMs, skipSegments) {
        skipSegments.firstOrNull { seg -> positionMs in seg.startMs..seg.endMs }
    }

    LaunchedEffect(activeSkipSegment, activeProfile.autoSkipIntros) {
        val matchingSegment = activeSkipSegment
        if (matchingSegment != null && activeProfile.autoSkipIntros && matchingSegment.type == SkipSegmentType.INTRO) {
            positionMs = matchingSegment.endMs
            requestedSeekMs = matchingSegment.endMs
            appState.showBanner("Auto-Skipped Intro")
        }
    }

    val displayTitle = remember(media.name, episode) {
        if (episode != null) "${media.name} • S${episode!!.season}E${episode!!.episode}" else media.name
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Spacebar -> {
                            isPlaying = !isPlaying
                            hudInteractionTick++
                            true
                        }
                        Key.DirectionLeft -> {
                            performRelativeSeek(-10_000L)
                            true
                        }
                        Key.DirectionRight -> {
                            performRelativeSeek(10_000L)
                            true
                        }
                        Key.DirectionUp -> {
                            isMuted = false
                            volume = (volume + 0.1f).coerceIn(0f, 1.5f)
                            triggerHudFeedback("Volume ${(volume * 100).roundToInt()}%", volume / 1.5f, Icons.AutoMirrored.Filled.VolumeUp)
                            true
                        }
                        Key.DirectionDown -> {
                            isMuted = false
                            volume = (volume - 0.1f).coerceIn(0f, 1.5f)
                            triggerHudFeedback("Volume ${(volume * 100).roundToInt()}%", volume / 1.5f, Icons.AutoMirrored.Filled.VolumeUp)
                            true
                        }
                        Key.B -> {
                            cycleVolumeBoost()
                            true
                        }
                        Key.S -> {
                            activeSheet = if (activeSheet == PlayerSheet.SUBTITLES) PlayerSheet.NONE else PlayerSheet.SUBTITLES
                            true
                        }
                        Key.A -> {
                            activeSheet = if (activeSheet == PlayerSheet.AUDIO) PlayerSheet.NONE else PlayerSheet.AUDIO
                            true
                        }
                        Key.V -> {
                            aspectIndex = (aspectIndex + 1) % aspectModes.size
                            triggerHudFeedback("Aspect: ${aspectModes[aspectIndex].uppercase()}", icon = Icons.Default.AspectRatio)
                            true
                        }
                        Key.F -> {
                            showControls = !showControls
                            true
                        }
                        Key.Escape -> {
                            if (activeSheet != PlayerSheet.NONE) {
                                activeSheet = PlayerSheet.NONE
                            } else {
                                appState.stopPlaybackAndSaveProgress(positionMs, durationMs)
                            }
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        if (isDesktopAwtHost) {
            // DESKTOP (LINUX & WINDOWS): Banded layout so heavyweight AWT `SwingPanel` never occludes the Plezy controls!
            Column(modifier = Modifier.fillMaxSize()) {
                if (showControls) {
                    PlezyTopBar(
                        title = displayTitle,
                        streamBadge = currentStream.detectedResolution,
                        p2pPeers = p2pTelemetry?.connectedPeers ?: 0,
                        isP2p = currentStream.infoHash != null,
                        onBack = { appState.stopPlaybackAndSaveProgress(positionMs, durationMs) },
                        onToggleZenMode = { showControls = false },
                        onOpenExternal = { activeSheet = PlayerSheet.EXTERNAL },
                        onOpenCast = { activeSheet = PlayerSheet.CAST },
                        isDesktop = true
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black)
                ) {
                    if (playableUrl != null) {
                        NativeVideoSurface(
                            streamUrl = playableUrl!!,
                            isPlaying = isPlaying,
                            seekToMs = requestedSeekMs,
                            playbackSpeed = effectiveSpeed,
                            volume = effectiveVolume,
                            aspectMode = aspectMode,
                            subtitleUrl = selectedSubtitle?.url,
                            subtitlesEnabled = subtitlesEnabled,
                            subtitleDelayMs = subConfig.delayOffsetMs,
                            subtitleFontSizeSp = subConfig.fontSizeSp,
                            cycleAudioTick = cycleAudioTick,
                            cycleEmbeddedSubTick = cycleEmbeddedSubTick,
                            onPositionChanged = { pos, dur ->
                                if (seekPreviewMs == null) positionMs = pos
                                if (dur > 0L) durationMs = dur
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        P2pBufferingOverlay(
                            title = currentStream.displayDetails,
                            statusText = if (p2pTelemetry != null) "Buffering P2P (${p2pTelemetry!!.connectedPeers} peers)..." else "Connecting to stream...",
                            progressPercent = p2pTelemetry?.bufferProgressPercent?.roundToInt() ?: 0
                        )
                    }
                }

                if (showControls) {
                    PlezyMinimalBottomBar(
                        positionMs = seekPreviewMs ?: positionMs,
                        durationMs = durationMs,
                        isPlaying = isPlaying,
                        hasNextEpisode = episode != null,
                        volume = effectiveVolume,
                        isMuted = isMuted,
                        playbackSpeed = effectiveSpeed,
                        aspectMode = aspectMode,
                        subtitlesActive = subtitlesEnabled && (selectedSubtitle != null || cycleEmbeddedSubTick > 0),
                        activeSkipSegment = activeSkipSegment,
                        onSeek = { target ->
                            positionMs = target
                            requestedSeekMs = target
                        },
                        onPlayPause = { isPlaying = !isPlaying },
                        onRewind10 = { performRelativeSeek(-10_000L) },
                        onForward10 = { performRelativeSeek(10_000L) },
                        onNextEpisode = { appState.playNextEpisodeInQueue() },
                        onCycleBoost = { cycleVolumeBoost() },
                        onCycleAspect = {
                            aspectIndex = (aspectIndex + 1) % aspectModes.size
                        },
                        onOpenSpeed = { activeSheet = PlayerSheet.SPEED },
                        onOpenAudio = { activeSheet = PlayerSheet.AUDIO },
                        onOpenSubtitles = { activeSheet = PlayerSheet.SUBTITLES },
                        onSkipSegment = { seg ->
                            positionMs = seg.endMs
                            requestedSeekMs = seg.endMs
                        }
                    )
                } else {
                    // Thin 28dp restore strip at bottom when desktop user hides bars with F or Fullscreen button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .background(Color.Black)
                            .clickable { showControls = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                        )
                    }
                }
            }
        } else {
            // iOS & ANDROID: True edge-to-edge fullscreen video + Moonfin gesture layer + auto-hiding Plezy floating bars
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val screenHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

                if (playableUrl != null) {
                    NativeVideoSurface(
                        streamUrl = playableUrl!!,
                        isPlaying = isPlaying,
                        seekToMs = requestedSeekMs,
                        playbackSpeed = effectiveSpeed,
                        volume = effectiveVolume,
                        aspectMode = aspectMode,
                        subtitleUrl = selectedSubtitle?.url,
                        subtitlesEnabled = subtitlesEnabled,
                        subtitleDelayMs = subConfig.delayOffsetMs,
                        subtitleFontSizeSp = subConfig.fontSizeSp,
                        cycleAudioTick = cycleAudioTick,
                        cycleEmbeddedSubTick = cycleEmbeddedSubTick,
                        onPositionChanged = { pos, dur ->
                            if (seekPreviewMs == null) positionMs = pos
                            if (dur > 0L) durationMs = dur
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    P2pBufferingOverlay(
                        title = currentStream.displayDetails,
                        statusText = if (p2pTelemetry != null) "Buffering P2P (${p2pTelemetry!!.connectedPeers} peers)..." else "Connecting to stream...",
                        progressPercent = p2pTelemetry?.bufferProgressPercent?.roundToInt() ?: 0
                    )
                }

                // Software brightness dimming overlay (controlled by vertical left-side swipe)
                if (brightness < 0.99f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = (1f - brightness).coerceIn(0f, 0.85f)))
                    )
                }

                // Moonfin Fullscreen Touch Gesture Layer:
                // - Single tap: toggle Plezy HUD
                // - Double-tap left/right: -10s / +10s
                // - Long press: temporary 2.0x speed
                // - Horizontal drag: scrub timeline backward/forward
                // - Vertical drag left half: Brightness (15%-100%)
                // - Vertical drag right half: Volume (0%-150% Boost)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(screenWidthPx, durationMs) {
                            detectTapGestures(
                                onTap = {
                                    showControls = !showControls
                                    hudInteractionTick++
                                },
                                onDoubleTap = { offset ->
                                    if (offset.x < screenWidthPx * 0.45f) {
                                        performRelativeSeek(-10_000L)
                                    } else if (offset.x > screenWidthPx * 0.55f) {
                                        performRelativeSeek(10_000L)
                                    } else {
                                        isPlaying = !isPlaying
                                        triggerHudFeedback(
                                            if (isPlaying) "Playing" else "Paused",
                                            icon = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause
                                        )
                                    }
                                },
                                onLongPress = {
                                    isTemp2x = true
                                    gestureHudLabel = "2.0x Speed"
                                    gestureHudProgress = null
                                    gestureHudIcon = Icons.Default.Speed
                                },
                                onPress = {
                                    tryAwaitRelease()
                                    if (isTemp2x) {
                                        isTemp2x = false
                                        gestureHudLabel = null
                                        gestureHudIcon = null
                                    }
                                }
                            )
                        }
                        .pointerInput(screenWidthPx, screenHeightPx, durationMs) {
                            var axis = DragGestureAxis.NONE
                            var startPosMs = 0L
                            var accumDx = 0f
                            var startBrightness = 1f
                            var startVolume = 1f
                            var accumDy = 0f

                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    axis = DragGestureAxis.NONE
                                    startPosMs = positionMs
                                    accumDx = 0f
                                    accumDy = 0f
                                    startBrightness = brightness
                                    startVolume = if (isMuted) 0f else volume
                                },
                                onDragEnd = {
                                    if (axis == DragGestureAxis.HORIZONTAL_SEEK && seekPreviewMs != null) {
                                        val finalTarget = seekPreviewMs!!
                                        positionMs = finalTarget
                                        requestedSeekMs = finalTarget
                                        seekPreviewMs = null
                                        gestureHudTick++
                                    } else {
                                        gestureHudTick++
                                    }
                                    axis = DragGestureAxis.NONE
                                },
                                onDragCancel = {
                                    seekPreviewMs = null
                                    axis = DragGestureAxis.NONE
                                    gestureHudTick++
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    accumDx += dragAmount.x
                                    accumDy += dragAmount.y

                                    if (axis == DragGestureAxis.NONE && (abs(accumDx) > 14f || abs(accumDy) > 14f)) {
                                        axis = if (abs(accumDx) > abs(accumDy)) {
                                            DragGestureAxis.HORIZONTAL_SEEK
                                        } else if (change.position.x < screenWidthPx * 0.5f) {
                                            DragGestureAxis.VERTICAL_BRIGHTNESS
                                        } else {
                                            DragGestureAxis.VERTICAL_VOLUME
                                        }
                                    }

                                    when (axis) {
                                        DragGestureAxis.HORIZONTAL_SEEK -> {
                                            val deltaMs = ((accumDx / screenWidthPx) * 120_000f).toLong()
                                            val maxDur = if (durationMs > 0L) durationMs else Long.MAX_VALUE
                                            val preview = (startPosMs + deltaMs).coerceIn(0L, maxDur)
                                            seekPreviewMs = preview
                                            val sign = if (deltaMs >= 0) "+" else "-"
                                            gestureHudLabel = "${formatPlayerTime(preview)} (${sign}${abs(deltaMs) / 1000}s)"
                                            gestureHudProgress = if (durationMs > 0L) (preview.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else null
                                            gestureHudIcon = if (deltaMs >= 0) Icons.Default.FastForward else Icons.Default.FastRewind
                                        }
                                        DragGestureAxis.VERTICAL_BRIGHTNESS -> {
                                            val delta = (-accumDy / (screenHeightPx * 0.55f))
                                            val newBrightness = (startBrightness + delta).coerceIn(0.15f, 1.0f)
                                            brightness = newBrightness
                                            gestureHudLabel = "Brightness ${(newBrightness * 100f).roundToInt()}%"
                                            gestureHudProgress = newBrightness
                                            gestureHudIcon = Icons.Default.BrightnessHigh
                                        }
                                        DragGestureAxis.VERTICAL_VOLUME -> {
                                            val delta = (-accumDy / (screenHeightPx * 0.55f)) * 1.5f
                                            val newVol = (startVolume + delta).coerceIn(0f, 1.5f)
                                            isMuted = newVol <= 0.01f
                                            volume = newVol
                                            val pct = (newVol * 100f).roundToInt()
                                            gestureHudLabel = if (pct > 100) "Boost $pct%" else "Volume $pct%"
                                            gestureHudProgress = (newVol / 1.5f).coerceIn(0f, 1f)
                                            gestureHudIcon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp
                                        }
                                        DragGestureAxis.NONE -> Unit
                                    }
                                }
                            )
                        }
                )

                // Floating Gesture Feedback Pill (center-top)
                AnimatedVisibility(
                    visible = gestureHudLabel != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 66.dp)
                ) {
                    val isBoostedHud = gestureHudLabel?.startsWith("Boost") == true
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xD80A0D14))
                            .border(
                                1.dp,
                                if (isBoostedHud) NaphColors.AmberWarn.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (gestureHudIcon != null) {
                                Icon(
                                    imageVector = gestureHudIcon!!,
                                    contentDescription = null,
                                    tint = if (isBoostedHud) NaphColors.AmberWarn else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = gestureHudLabel.orEmpty(),
                                color = if (isBoostedHud) NaphColors.AmberWarn else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        if (gestureHudProgress != null) {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(gestureHudProgress!!.coerceIn(0f, 1f))
                                        .clip(CircleShape)
                                        .background(if (isBoostedHud) NaphColors.AmberWarn else NaphColors.CyanGlow)
                                )
                            }
                        }
                    }
                }

                // Auto-hiding Floating Top Bar
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TopScrimBrush)
                            .statusBarsPadding()
                    ) {
                        PlezyTopBar(
                            title = displayTitle,
                            streamBadge = currentStream.detectedResolution,
                            p2pPeers = p2pTelemetry?.connectedPeers ?: 0,
                            isP2p = currentStream.infoHash != null,
                            onBack = { appState.stopPlaybackAndSaveProgress(positionMs, durationMs) },
                            onToggleZenMode = { showControls = false },
                            onOpenExternal = { activeSheet = PlayerSheet.EXTERNAL },
                            onOpenCast = { activeSheet = PlayerSheet.CAST },
                            isDesktop = false
                        )
                    }
                }

                // Auto-hiding Plezy Minimal Bottom Bar
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BottomScrimBrush)
                            .navigationBarsPadding()
                    ) {
                        PlezyMinimalBottomBar(
                            positionMs = seekPreviewMs ?: positionMs,
                            durationMs = durationMs,
                            isPlaying = isPlaying,
                            hasNextEpisode = episode != null,
                            volume = effectiveVolume,
                            isMuted = isMuted,
                            playbackSpeed = effectiveSpeed,
                            aspectMode = aspectMode,
                            subtitlesActive = subtitlesEnabled && (selectedSubtitle != null || cycleEmbeddedSubTick > 0),
                            activeSkipSegment = activeSkipSegment,
                            onSeek = { target ->
                                positionMs = target
                                requestedSeekMs = target
                                hudInteractionTick++
                            },
                            onPlayPause = {
                                isPlaying = !isPlaying
                                hudInteractionTick++
                            },
                            onRewind10 = { performRelativeSeek(-10_000L) },
                            onForward10 = { performRelativeSeek(10_000L) },
                            onNextEpisode = { appState.playNextEpisodeInQueue() },
                            onCycleBoost = {
                                cycleVolumeBoost()
                                hudInteractionTick++
                            },
                            onCycleAspect = {
                                aspectIndex = (aspectIndex + 1) % aspectModes.size
                                triggerHudFeedback("Aspect: ${aspectModes[aspectIndex].uppercase()}", icon = Icons.Default.AspectRatio)
                                hudInteractionTick++
                            },
                            onOpenSpeed = { activeSheet = PlayerSheet.SPEED },
                            onOpenAudio = { activeSheet = PlayerSheet.AUDIO },
                            onOpenSubtitles = { activeSheet = PlayerSheet.SUBTITLES },
                            onSkipSegment = { seg ->
                                positionMs = seg.endMs
                                requestedSeekMs = seg.endMs
                            }
                        )
                    }
                }
            }
        }

        // =========================================================================
        // DIALOG-BACKED MODAL SHEETS (Guaranteed above SwingPanel & UIKitView!)
        // =========================================================================
        when (activeSheet) {
            PlayerSheet.SUBTITLES -> {
                PlayerModalDialog(
                    title = "Subtitles",
                    onDismiss = { activeSheet = PlayerSheet.NONE }
                ) {
                    // Quick Off / Embedded Track Cycle row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModernFilterPill(
                            label = if (!subtitlesEnabled) "Subtitles Off ✓" else "Turn Off",
                            selected = !subtitlesEnabled,
                            onClick = {
                                subtitlesEnabled = false
                                appState.selectSubtitleTrack(null)
                            }
                        )
                        ModernFilterPill(
                            label = "Cycle Embedded Track",
                            selected = false,
                            onClick = {
                                subtitlesEnabled = true
                                cycleEmbeddedSubTick++
                                appState.showBanner("Cycled embedded subtitle track")
                            }
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        "ADDON SUBTITLE TRACKS (${subtitles.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = NaphColors.TextMuted
                    )
                    Spacer(Modifier.height(8.dp))

                    if (subtitles.isEmpty()) {
                        Text(
                            "No external addon subtitles found for this stream. Use 'Cycle Embedded Track' above for built-in MKV/MP4 subtitles.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NaphColors.TextSecondary
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            subtitles.forEach { track: SubtitleTrack ->
                                val isSelected = subtitlesEnabled && selectedSubtitle?.id == track.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) NaphColors.ElectricPrimary.copy(alpha = 0.22f) else NaphColors.CardSurface)
                                        .border(
                                            1.dp,
                                            if (isSelected) NaphColors.ElectricPrimary else Color.White.copy(alpha = 0.08f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            subtitlesEnabled = true
                                            appState.selectSubtitleTrack(track)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            track.label.ifBlank { track.lang.uppercase() },
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            "${track.lang.uppercase()} • ${track.format.uppercase()}",
                                            color = NaphColors.TextMuted,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isSelected) {
                                        Text("ACTIVE", color = NaphColors.CyanGlow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(Modifier.height(12.dp))

                    // Delay ±100ms
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sync Delay", color = NaphColors.TextSecondary, fontSize = 13.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ModernFilterPill(
                                label = "-100ms",
                                selected = false,
                                onClick = { appState.updateSubtitleConfig(subConfig.copy(delayOffsetMs = subConfig.delayOffsetMs - 100L)) }
                            )
                            Text(
                                "${if (subConfig.delayOffsetMs >= 0) "+" else ""}${subConfig.delayOffsetMs}ms",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            ModernFilterPill(
                                label = "+100ms",
                                selected = false,
                                onClick = { appState.updateSubtitleConfig(subConfig.copy(delayOffsetMs = subConfig.delayOffsetMs + 100L)) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Font Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Font Size", color = NaphColors.TextSecondary, fontSize = 13.sp)
                        Text("${subConfig.fontSizeSp.roundToInt()}sp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Slider(
                        value = subConfig.fontSizeSp,
                        onValueChange = { appState.updateSubtitleConfig(subConfig.copy(fontSizeSp = it)) },
                        valueRange = 14f..36f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = NaphColors.CyanGlow,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )

                    // ASS/SSA styling toggle using high-contrast NaphSwitch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Preserve ASS/SSA Styling", color = NaphColors.TextSecondary, fontSize = 13.sp)
                        NaphSwitch(
                            checked = subConfig.preserveAssStyling,
                            onCheckedChange = { appState.updateSubtitleConfig(subConfig.copy(preserveAssStyling = it)) }
                        )
                    }
                }
            }

            PlayerSheet.AUDIO -> {
                PlayerModalDialog(
                    title = "Audio & Volume Boost",
                    onDismiss = { activeSheet = PlayerSheet.NONE }
                ) {
                    val pct = (effectiveVolume * 100f).roundToInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Volume & Boost", color = NaphColors.TextSecondary, fontSize = 13.sp)
                        Text(
                            if (pct > 100) "BOOST $pct%" else "$pct%",
                            color = if (pct > 100) NaphColors.AmberWarn else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Slider(
                        value = effectiveVolume,
                        onValueChange = {
                            volume = it
                            isMuted = it <= 0.01f
                        },
                        valueRange = 0f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = if (effectiveVolume > 1.01f) NaphColors.AmberWarn else Color.White,
                            activeTrackColor = if (effectiveVolume > 1.01f) NaphColors.AmberWarn else NaphColors.CyanGlow,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModernFilterPill(label = "Mute", selected = isMuted, onClick = { isMuted = !isMuted })
                        ModernFilterPill(label = "100%", selected = !isMuted && pct in 95..105, onClick = { isMuted = false; volume = 1.0f })
                        ModernFilterPill(label = "125% Boost", selected = !isMuted && pct in 120..130, onClick = { isMuted = false; volume = 1.25f })
                        ModernFilterPill(label = "150% Boost", selected = !isMuted && pct >= 145, onClick = { isMuted = false; volume = 1.5f })
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(Modifier.height(14.dp))

                    Text("EMBEDDED AUDIO TRACKS", style = MaterialTheme.typography.labelSmall, color = NaphColors.TextMuted)
                    Spacer(Modifier.height(8.dp))
                    ModernPrimaryButton(
                        text = "Cycle Audio Track (MPV)",
                        icon = Icons.Default.Audiotrack,
                        onClick = {
                            cycleAudioTick++
                            appState.showBanner("Switched audio track")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            PlayerSheet.SPEED -> {
                PlayerModalDialog(
                    title = "Playback Speed",
                    onDismiss = { activeSheet = PlayerSheet.NONE }
                ) {
                    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        speeds.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { spd ->
                                    val selected = abs(baseSpeed - spd) < 0.01f
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernFilterPill(
                                            label = "${spd}x",
                                            selected = selected,
                                            onClick = {
                                                baseSpeed = spd
                                                activeSheet = PlayerSheet.NONE
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            PlayerSheet.EXTERNAL -> {
                PlayerModalDialog(
                    title = "Open in External Player",
                    onDismiss = { activeSheet = PlayerSheet.NONE }
                ) {
                    val url = playableUrl.orEmpty()
                    val players = listOf("MPV", "VLC", "IINA", "Infuse", "Outplayer")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        players.forEach { playerName ->
                            ModernGlassButton(
                                text = "Open in $playerName",
                                icon = Icons.AutoMirrored.Filled.OpenInNew,
                                onClick = {
                                    val ok = launchExternalPlayer(playerName, url, displayTitle)
                                    appState.showBanner(
                                        if (ok) "Launched $playerName" else "$playerName is not installed on this device"
                                    )
                                    activeSheet = PlayerSheet.NONE
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            PlayerSheet.CAST -> {
                PlayerModalDialog(
                    title = "Cast & AirPlay",
                    onDismiss = { activeSheet = PlayerSheet.NONE }
                ) {
                    val devices = listOf("Living Room Apple TV", "Bedroom Chromecast", "Smart TV (DLNA)")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        devices.forEach { dev ->
                            ModernGlassButton(
                                text = dev,
                                icon = Icons.Default.Cast,
                                onClick = {
                                    appState.showBanner("Casting '${media.name}' to $dev")
                                    activeSheet = PlayerSheet.NONE
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            PlayerSheet.NONE -> Unit
        }
    }
}

@Composable
private fun PlezyTopBar(
    title: String,
    streamBadge: String,
    p2pPeers: Int,
    isP2p: Boolean,
    onBack: () -> Unit,
    onToggleZenMode: () -> Unit,
    onOpenExternal: () -> Unit,
    onOpenCast: () -> Unit,
    isDesktop: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDesktop) Color(0xFF0A0D14) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = streamBadge,
                    color = NaphColors.CyanGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isP2p) {
                    Text(
                        text = "• P2P ($p2pPeers peers)",
                        color = NaphColors.EmeraldSeed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (isDesktop) {
            PlezyIconChip(icon = Icons.Default.Fullscreen, contentDescription = "Hide Controls (F)", onClick = onToggleZenMode)
        }
        PlezyIconChip(icon = Icons.Default.Cast, contentDescription = "Cast", onClick = onOpenCast)
        PlezyIconChip(icon = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "External Player", onClick = onOpenExternal)
    }
}

@Composable
private fun PlezyMinimalBottomBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    hasNextEpisode: Boolean,
    volume: Float,
    isMuted: Boolean,
    playbackSpeed: Float,
    aspectMode: String,
    subtitlesActive: Boolean,
    activeSkipSegment: SkipSegment?,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onNextEpisode: () -> Unit,
    onCycleBoost: () -> Unit,
    onCycleAspect: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onSkipSegment: (SkipSegment) -> Unit
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val fraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
    val volPct = (volume * 100f).roundToInt()
    val isBoosted = !isMuted && volPct > 100

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDesktopAwtHost) Color(0xFF0A0D14) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Optional Skip Intro / Credits pill floating just above scrubber
        if (activeSkipSegment != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NaphColors.ElectricPrimary)
                        .clickable { onSkipSegment(activeSkipSegment) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = activeSkipSegment.type.label,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Slim Plezy Scrubber Row with elapsed / remaining timestamps
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = formatPlayerTime(positionMs),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Slider(
                value = fraction,
                onValueChange = { newFrac ->
                    if (durationMs > 0L) {
                        onSeek((newFrac * durationMs).toLong())
                    }
                },
                modifier = Modifier.weight(1f).height(22.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                )
            )
            Text(
                text = "-${formatPlayerTime(remainingMs)}",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Plezy Minimal Transport & Quick Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Transport Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlezyIconChip(icon = Icons.Default.FastRewind, contentDescription = "-10s", onClick = onRewind10)

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }

                PlezyIconChip(icon = Icons.Default.FastForward, contentDescription = "+10s", onClick = onForward10)

                if (hasNextEpisode) {
                    PlezyIconChip(icon = Icons.Default.SkipNext, contentDescription = "Next Episode", onClick = onNextEpisode)
                }
            }

            // Minimal Action Pills (scrollable on narrow iPhones so nothing ever clips)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlezyTextPill(
                    label = if (isBoosted) "${volPct}%" else "Boost",
                    active = isBoosted,
                    activeColor = NaphColors.AmberWarn,
                    onClick = onCycleBoost
                )
                PlezyTextPill(
                    label = "${playbackSpeed}x",
                    active = abs(playbackSpeed - 1.0f) > 0.01f,
                    activeColor = NaphColors.CyanGlow,
                    onClick = onOpenSpeed
                )
                PlezyTextPill(
                    label = aspectMode.uppercase(),
                    active = aspectMode != "fit",
                    activeColor = NaphColors.CyanGlow,
                    onClick = onCycleAspect
                )
                PlezyIconChip(
                    icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.Default.Audiotrack,
                    contentDescription = "Audio & Boost",
                    highlighted = isBoosted,
                    onClick = onOpenAudio
                )
                PlezyIconChip(
                    icon = Icons.Default.Subtitles,
                    contentDescription = "Subtitles",
                    highlighted = subtitlesActive,
                    onClick = onOpenSubtitles
                )
            }
        }
    }
}

@Composable
private fun PlezyIconChip(
    icon: ImageVector,
    contentDescription: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (highlighted) NaphColors.CyanGlow.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.1f))
            .border(
                1.dp,
                if (highlighted) NaphColors.CyanGlow.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.12f),
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (highlighted) NaphColors.CyanGlow else Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun PlezyTextPill(
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (active) activeColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
            .border(
                1.dp,
                if (active) activeColor.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(17.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) activeColor else Color.White.copy(alpha = 0.92f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PlayerModalDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onDismiss)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xF5111622))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
                    .clickable(enabled = false) {}
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}

@Composable
private fun P2pBufferingOverlay(
    title: String,
    statusText: String,
    progressPercent: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudQueue,
                contentDescription = null,
                tint = NaphColors.CyanGlow,
                modifier = Modifier.size(38.dp)
            )
            CircularProgressIndicator(
                color = NaphColors.CyanGlow,
                trackColor = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(34.dp)
            )
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (progressPercent > 0) "$statusText ($progressPercent%)" else statusText,
                color = NaphColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

private fun formatPlayerTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')
    return if (hours > 0L) "$hours:$mm:$ss" else "$mm:$ss"
}
