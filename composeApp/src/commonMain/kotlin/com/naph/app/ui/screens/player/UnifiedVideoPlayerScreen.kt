package com.naph.app.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
    AUDIO_SPEED,
    EXTERNAL
}

private enum class DragGestureAxis {
    NONE,
    HORIZONTAL_SEEK,
    VERTICAL_BRIGHTNESS,
    VERTICAL_VOLUME
}

private val PlezyTopScrim = Brush.verticalGradient(
    colors = listOf(Color.Black.copy(alpha = 0.72f), Color.Black.copy(alpha = 0.22f), Color.Transparent)
)

private val PlezyBottomScrim = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.30f), Color.Black.copy(alpha = 0.80f))
)

/**
 * Plezy-Style Minimal Cinema Player + Moonfin Gestures + 150% Volume Boost:
 * - Desktop (Linux & Windows):
 *   - Real OS window fullscreen toggle (`Fullscreen` icon / `F` key)
 *   - Visible Volume icon + Mute + compact Volume/Boost slider (0–150%) right in the bottom bar
 *   - Docked 340dp right-hand side panel for Subtitles / Audio+Boost+Speed / External Player so heavyweight
 *     AWT `SwingPanel` (`libmpv`) can NEVER cover or block clicks on player menus!
 * - Mobile (iOS & Android):
 *   - True edge-to-edge fullscreen video with `interactive = false` on `UIKitView` + `zIndex` layering
 *   - Clean borderless Plezy top bar with Volume/Boost, Subtitles, Aspect, Lock & External icons
 *   - Centered translucent transport circles (`-10s`, big `Play/Pause`, `+10s`) + razor-thin `3.5dp` Plezy scrubber
 *   - Full Moonfin swipe gestures: left vertical=Brightness, right vertical=Volume (0–150% Boost), horizontal=scrub, double-tap ±10s, long-press=2x
 *   - In-tree `zIndex(100f)` modal sheets so every button & menu works 100% reliably on iPhone!
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

    var baseSpeed by remember { mutableStateOf(1.0f) }
    var isTemp2x by remember { mutableStateOf(false) }
    val effectiveSpeed = if (isTemp2x) 2.0f else baseSpeed

    var volume by remember { mutableStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }
    var brightness by remember { mutableStateOf(1.0f) }
    val effectiveVolume = if (isMuted) 0f else volume

    val aspectModes = remember { listOf("fit", "fill", "16:9", "stretch") }
    var aspectIndex by remember { mutableStateOf(0) }
    val aspectMode = aspectModes[aspectIndex]

    var subtitlesEnabled by remember { mutableStateOf(true) }
    var cycleAudioTick by remember { mutableStateOf(0) }
    var cycleEmbeddedSubTick by remember { mutableStateOf(0) }

    var showControls by remember { mutableStateOf(true) }
    var isDesktopFullscreen by remember { mutableStateOf(false) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var hudInteractionTick by remember { mutableStateOf(0) }
    var activeSheet by remember { mutableStateOf(PlayerSheet.NONE) }

    var gestureHudLabel by remember { mutableStateOf<String?>(null) }
    var gestureHudProgress by remember { mutableStateOf<Float?>(null) }
    var gestureHudIcon by remember { mutableStateOf<ImageVector?>(null) }
    var gestureHudTick by remember { mutableStateOf(0) }
    var seekPreviewMs by remember { mutableStateOf<Long?>(null) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    DisposableEffect(Unit) {
        if (isDesktopAwtHost) {
            registerDesktopVideoInteraction(
                onMouseMove = {
                    showControls = true
                    hudInteractionTick++
                },
                onDoubleClick = {
                    isDesktopFullscreen = toggleNativeFullscreen()
                    showControls = true
                    hudInteractionTick++
                }
            )
        }
        onDispose {
            if (isDesktopAwtHost) {
                registerDesktopVideoInteraction(null, null)
            }
            exitNativeFullscreen()
        }
    }

    // Auto-hide HUD after 3s while playing:
    // - On mobile: always auto-hides
    // - On Desktop (Linux/Windows): auto-hides in Fullscreen for 100% edge-to-edge cinema!
    LaunchedEffect(showControls, isPlaying, activeSheet, hudInteractionTick, isDesktopFullscreen) {
        val shouldAutoHide = if (isDesktopAwtHost) isDesktopFullscreen else true
        if (shouldAutoHide && showControls && isPlaying && activeSheet == PlayerSheet.NONE) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(gestureHudTick) {
        if (gestureHudLabel != null && seekPreviewMs == null && !isTemp2x) {
            delay(1000)
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

    fun exitPlayer() {
        exitNativeFullscreen()
        appState.stopPlaybackAndSaveProgress(positionMs, durationMs)
    }

    val activeSkipSegment: SkipSegment? = remember(positionMs, skipSegments) {
        skipSegments.firstOrNull { seg -> positionMs in seg.startMs..seg.endMs }
    }

    LaunchedEffect(activeSkipSegment, activeProfile.autoSkipIntros) {
        val matchingSegment = activeSkipSegment
        if (matchingSegment != null && activeProfile.autoSkipIntros && matchingSegment.type == SkipSegmentType.INTRO) {
            positionMs = matchingSegment.endMs
            requestedSeekMs = matchingSegment.endMs
        }
    }

    val subtitleLine = remember(episode, currentStream, p2pTelemetry) {
        buildString {
            if (episode != null) {
                append("S${episode!!.season} · E${episode!!.episode}")
                if (episode!!.title.isNotBlank()) append(" · ${episode!!.title}")
                append(" · ")
            }
            append(currentStream.detectedResolution)
            if (currentStream.infoHash != null && p2pTelemetry != null) {
                append(" · ${p2pTelemetry!!.connectedPeers} peers")
            }
        }
    }

    // Reusable sheet body rendered EITHER in the Desktop side-panel (immune to AWT SwingPanel!) OR iOS overlay!
    @Composable
    fun SheetBodyContent(sheet: PlayerSheet) {
        when (sheet) {
            PlayerSheet.SUBTITLES -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModernFilterPill(
                        label = if (!subtitlesEnabled) "Off ✓" else "Turn Off",
                        selected = !subtitlesEnabled,
                        onClick = {
                            subtitlesEnabled = false
                            appState.selectSubtitleTrack(null)
                        }
                    )
                    ModernFilterPill(
                        label = "Cycle Embedded Sub",
                        selected = false,
                        onClick = {
                            subtitlesEnabled = true
                            cycleEmbeddedSubTick++
                        }
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "ADDON SUBTITLES (${subtitles.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = NaphColors.TextMuted
                )
                Spacer(Modifier.height(8.dp))

                if (subtitles.isEmpty()) {
                    Text(
                        "No external addon subtitles found. Click 'Cycle Embedded Sub' above for built-in MKV/MP4 tracks.",
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
                                        fontSize = 11.sp
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sync Delay", color = NaphColors.TextSecondary, fontSize = 13.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            fontSize = 12.sp
                        )
                        ModernFilterPill(
                            label = "+100ms",
                            selected = false,
                            onClick = { appState.updateSubtitleConfig(subConfig.copy(delayOffsetMs = subConfig.delayOffsetMs + 100L)) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ASS/SSA Styling", color = NaphColors.TextSecondary, fontSize = 13.sp)
                    NaphSwitch(
                        checked = subConfig.preserveAssStyling,
                        onCheckedChange = { appState.updateSubtitleConfig(subConfig.copy(preserveAssStyling = it)) }
                    )
                }
            }

            PlayerSheet.AUDIO_SPEED -> {
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
                        activeTrackColor = if (effectiveVolume > 1.01f) NaphColors.AmberWarn else Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModernFilterPill(label = "Mute", selected = isMuted, onClick = { isMuted = !isMuted })
                    ModernFilterPill(label = "100%", selected = !isMuted && pct in 95..105, onClick = { isMuted = false; volume = 1.0f })
                    ModernFilterPill(label = "125%", selected = !isMuted && pct in 120..130, onClick = { isMuted = false; volume = 1.25f })
                    ModernFilterPill(label = "150%", selected = !isMuted && pct >= 145, onClick = { isMuted = false; volume = 1.5f })
                }

                Spacer(Modifier.height(14.dp))
                ModernPrimaryButton(
                    text = "Cycle Audio Track (MPV)",
                    icon = Icons.Default.Audiotrack,
                    onClick = { cycleAudioTick++ },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(Modifier.height(12.dp))

                Text("PLAYBACK SPEED", style = MaterialTheme.typography.labelSmall, color = NaphColors.TextMuted)
                Spacer(Modifier.height(8.dp))
                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    speeds.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { spd ->
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernFilterPill(
                                        label = "${spd}x",
                                        selected = abs(baseSpeed - spd) < 0.01f,
                                        onClick = { baseSpeed = spd },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PlayerSheet.EXTERNAL -> {
                val url = playableUrl.orEmpty()
                val players = listOf("MPV", "VLC", "IINA", "Infuse", "Outplayer")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    players.forEach { playerName ->
                        ModernGlassButton(
                            text = "Open in $playerName",
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            onClick = {
                                launchExternalPlayer(playerName, url, media.name)
                                activeSheet = PlayerSheet.NONE
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            PlayerSheet.NONE -> Unit
        }
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
                            true
                        }
                        Key.DirectionDown -> {
                            isMuted = false
                            volume = (volume - 0.1f).coerceIn(0f, 1.5f)
                            true
                        }
                        Key.S -> {
                            activeSheet = if (activeSheet == PlayerSheet.SUBTITLES) PlayerSheet.NONE else PlayerSheet.SUBTITLES
                            true
                        }
                        Key.A -> {
                            activeSheet = if (activeSheet == PlayerSheet.AUDIO_SPEED) PlayerSheet.NONE else PlayerSheet.AUDIO_SPEED
                            true
                        }
                        Key.V -> {
                            aspectIndex = (aspectIndex + 1) % aspectModes.size
                            true
                        }
                        Key.F -> {
                            isDesktopFullscreen = toggleNativeFullscreen()
                            showControls = true
                            hudInteractionTick++
                            true
                        }
                        Key.Escape -> {
                            if (activeSheet != PlayerSheet.NONE) {
                                activeSheet = PlayerSheet.NONE
                            } else if (isDesktopFullscreen) {
                                exitNativeFullscreen()
                                isDesktopFullscreen = false
                                showControls = true
                            } else {
                                exitPlayer()
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
            // =========================================================================
            // DESKTOP (LINUX & WINDOWS):
            // - True edge-to-edge cinema in Fullscreen (bars auto-hide after 3s idle, reappear instantly on mouse move!)
            // - Docked 340dp right-hand side panel for Subtitles / Volume / Speed (never occluded by AWT SwingPanel!)
            // =========================================================================
            val showDesktopBars = !isDesktopFullscreen || showControls || activeSheet != PlayerSheet.NONE || !isPlaying
            Column(modifier = Modifier.fillMaxSize()) {
                if (showDesktopBars) {
                    PlezyTopHeader(
                        title = media.name,
                        subtitle = subtitleLine,
                        isLocked = false,
                        showLockIcon = false,
                        showFullscreenIcon = true,
                        subtitlesActive = subtitlesEnabled && (selectedSubtitle != null || cycleEmbeddedSubTick > 0),
                        isBoosted = !isMuted && volume > 1.02f,
                        isMuted = isMuted,
                        onBack = { exitPlayer() },
                        onOpenAudioSpeed = {
                            activeSheet = if (activeSheet == PlayerSheet.AUDIO_SPEED) PlayerSheet.NONE else PlayerSheet.AUDIO_SPEED
                            hudInteractionTick++
                        },
                        onOpenSubtitles = {
                            activeSheet = if (activeSheet == PlayerSheet.SUBTITLES) PlayerSheet.NONE else PlayerSheet.SUBTITLES
                            hudInteractionTick++
                        },
                        onCycleAspect = {
                            aspectIndex = (aspectIndex + 1) % aspectModes.size
                            hudInteractionTick++
                        },
                        onToggleLock = {},
                        onToggleFullscreen = {
                            isDesktopFullscreen = toggleNativeFullscreen()
                            showControls = true
                            hudInteractionTick++
                        },
                        onOpenExternal = {
                            activeSheet = if (activeSheet == PlayerSheet.EXTERNAL) PlayerSheet.NONE else PlayerSheet.EXTERNAL
                            hudInteractionTick++
                        },
                        modifier = Modifier.background(Color(0xFF0A0C10))
                    )
                }

                // Video + Optional Right-Hand Docked Sheet (guaranteed 100% visible & clickable on Linux/Windows!)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
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

                    if (activeSheet != PlayerSheet.NONE) {
                        Column(
                            modifier = Modifier
                                .width(340.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF10141E))
                                .border(1.dp, Color.White.copy(alpha = 0.1f))
                                .verticalScroll(rememberScrollState())
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = when (activeSheet) {
                                        PlayerSheet.SUBTITLES -> "Subtitles"
                                        PlayerSheet.AUDIO_SPEED -> "Volume, Audio & Speed"
                                        PlayerSheet.EXTERNAL -> "External Player"
                                        PlayerSheet.NONE -> ""
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                PlezyCleanIconButton(
                                    icon = Icons.Default.Close,
                                    contentDescription = "Close",
                                    iconSize = 18.dp,
                                    onClick = { activeSheet = PlayerSheet.NONE }
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            SheetBodyContent(activeSheet)
                        }
                    }
                }

                // Clean Plezy Desktop Bottom Bar (auto-hides in Fullscreen after 3s idle for 100% edge-to-edge video!)
                if (showDesktopBars) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0A0C10))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    PlezyScrubberSection(
                        positionMs = seekPreviewMs ?: positionMs,
                        durationMs = durationMs,
                        volume = effectiveVolume,
                        isMuted = isMuted,
                        playbackSpeed = effectiveSpeed,
                        activeSkipSegment = activeSkipSegment,
                        onSeek = { target ->
                            positionMs = target
                            requestedSeekMs = target
                        },
                        onSkipSegment = { seg ->
                            positionMs = seg.endMs
                            requestedSeekMs = seg.endMs
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Visible Volume / Mute + Inline 0–150% Boost Scrubber
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.width(210.dp)
                        ) {
                            PlezyCleanIconButton(
                                icon = if (isMuted || effectiveVolume <= 0.01f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Mute / Unmute",
                                tint = if (!isMuted && effectiveVolume > 1.02f) NaphColors.AmberWarn else Color.White,
                                onClick = { isMuted = !isMuted }
                            )
                            PlezyRazorScrubber(
                                fraction = (effectiveVolume / 1.5f).coerceIn(0f, 1f),
                                activeColor = if (!isMuted && effectiveVolume > 1.02f) NaphColors.AmberWarn else Color.White,
                                onSeekFraction = { frac ->
                                    val newVol = (frac * 1.5f).coerceIn(0f, 1.5f)
                                    volume = newVol
                                    isMuted = newVol <= 0.01f
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(effectiveVolume * 100f).roundToInt()}%",
                                color = if (!isMuted && effectiveVolume > 1.02f) NaphColors.AmberWarn else Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(38.dp)
                            )
                        }

                        // Center: Transport Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PlezyCleanIconButton(
                                icon = Icons.Default.FastRewind,
                                contentDescription = "-10s",
                                iconSize = 24.dp,
                                onClick = { performRelativeSeek(-10_000L) }
                            )
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .clickable { isPlaying = !isPlaying },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            PlezyCleanIconButton(
                                icon = Icons.Default.FastForward,
                                contentDescription = "+10s",
                                iconSize = 24.dp,
                                onClick = { performRelativeSeek(10_000L) }
                            )
                            if (episode != null) {
                                PlezyCleanIconButton(
                                    icon = Icons.Default.SkipNext,
                                    contentDescription = "Next Episode",
                                    iconSize = 24.dp,
                                    onClick = { appState.playNextEpisodeInQueue() }
                                )
                            }
                        }

                        // Right: Quick Aspect & Speed badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.width(210.dp)
                        ) {
                            Text(
                                text = "${aspectMode.uppercase()} · ${effectiveSpeed}x",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        activeSheet = if (activeSheet == PlayerSheet.AUDIO_SPEED) PlayerSheet.NONE else PlayerSheet.AUDIO_SPEED
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                }
            }
        } else {
            // =========================================================================
            // iOS & ANDROID: True Edge-to-Edge Fullscreen Plezy Cinema Player + Moonfin Gestures
            // =========================================================================
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val screenHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

                // Layer 0: Native Hardware-Accelerated Video Surface (`interactive = false` on iOS!)
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
                        modifier = Modifier.fillMaxSize().zIndex(0f)
                    )
                } else {
                    P2pBufferingOverlay(
                        title = currentStream.displayDetails,
                        statusText = if (p2pTelemetry != null) "Buffering P2P (${p2pTelemetry!!.connectedPeers} peers)..." else "Connecting to stream...",
                        progressPercent = p2pTelemetry?.bufferProgressPercent?.roundToInt() ?: 0
                    )
                }

                // Layer 1: Software brightness overlay
                if (brightness < 0.99f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f)
                            .background(Color.Black.copy(alpha = (1f - brightness).coerceIn(0f, 0.85f)))
                    )
                }

                // Layer 2: Moonfin Fullscreen Touch & Swipe Gesture Surface
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                        .pointerInput(screenWidthPx, durationMs, isScreenLocked) {
                            detectTapGestures(
                                onTap = {
                                    showControls = !showControls
                                    hudInteractionTick++
                                },
                                onDoubleTap = { offset ->
                                    if (isScreenLocked) return@detectTapGestures
                                    if (offset.x < screenWidthPx * 0.42f) {
                                        performRelativeSeek(-10_000L)
                                    } else if (offset.x > screenWidthPx * 0.58f) {
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
                                    if (isScreenLocked) return@detectTapGestures
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
                        .pointerInput(screenWidthPx, screenHeightPx, durationMs, isScreenLocked) {
                            if (isScreenLocked) return@pointerInput
                            var axis = DragGestureAxis.NONE
                            var startPosMs = 0L
                            var accumDx = 0f
                            var startBrightness = 1f
                            var startVolume = 1f
                            var accumDy = 0f

                            detectDragGestures(
                                onDragStart = {
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
                                    }
                                    axis = DragGestureAxis.NONE
                                    gestureHudTick++
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

                // Layer 10: Floating Gesture Feedback Pill
                AnimatedVisibility(
                    visible = gestureHudLabel != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(10f)
                        .statusBarsPadding()
                        .padding(top = 60.dp)
                ) {
                    val isBoostedHud = gestureHudLabel?.startsWith("Boost") == true
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
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
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        if (gestureHudProgress != null) {
                            Box(
                                modifier = Modifier
                                    .width(116.dp)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(gestureHudProgress!!.coerceIn(0f, 1f))
                                        .clip(CircleShape)
                                        .background(if (isBoostedHud) NaphColors.AmberWarn else Color.White)
                                )
                            }
                        }
                    }
                }

                // Layer 20: Plezy HUD (Top Bar + Floating Center Transport + Razor-Thin Bottom Scrubber)
                if (isScreenLocked) {
                    AnimatedVisibility(
                        visible = showControls,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .zIndex(20f)
                            .statusBarsPadding()
                            .displayCutoutPadding()
                            .padding(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable {
                                    isScreenLocked = false
                                    showControls = true
                                    hudInteractionTick++
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = "Unlock", tint = Color.White, modifier = Modifier.size(18.dp))
                                Text("Unlock", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                } else {
                    AnimatedVisibility(
                        visible = showControls,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .zIndex(20f)
                    ) {
                        PlezyTopHeader(
                            title = media.name,
                            subtitle = subtitleLine,
                            isLocked = isScreenLocked,
                            showLockIcon = true,
                            showFullscreenIcon = false,
                            subtitlesActive = subtitlesEnabled && (selectedSubtitle != null || cycleEmbeddedSubTick > 0),
                            isBoosted = !isMuted && volume > 1.02f,
                            isMuted = isMuted,
                            onBack = { exitPlayer() },
                            onOpenAudioSpeed = { activeSheet = PlayerSheet.AUDIO_SPEED },
                            onOpenSubtitles = { activeSheet = PlayerSheet.SUBTITLES },
                            onCycleAspect = {
                                aspectIndex = (aspectIndex + 1) % aspectModes.size
                                triggerHudFeedback("Aspect: ${aspectModes[aspectIndex].uppercase()}", icon = Icons.Default.AspectRatio)
                                hudInteractionTick++
                            },
                            onToggleLock = {
                                isScreenLocked = true
                                showControls = false
                            },
                            onToggleFullscreen = {},
                            onOpenExternal = { activeSheet = PlayerSheet.EXTERNAL },
                            modifier = Modifier
                                .background(PlezyTopScrim)
                                .statusBarsPadding()
                                .displayCutoutPadding()
                        )
                    }

                    AnimatedVisibility(
                        visible = showControls,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .zIndex(20f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(26.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .clickable { performRelativeSeek(-10_000L) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FastRewind, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(28.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.50f))
                                    .clickable {
                                        isPlaying = !isPlaying
                                        hudInteractionTick++
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .clickable { performRelativeSeek(10_000L) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FastForward, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(28.dp))
                            }

                            if (episode != null) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .clickable { appState.playNextEpisodeInQueue() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Next Episode", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showControls,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(20f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PlezyBottomScrim)
                                .navigationBarsPadding()
                                .displayCutoutPadding()
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            PlezyScrubberSection(
                                positionMs = seekPreviewMs ?: positionMs,
                                durationMs = durationMs,
                                volume = effectiveVolume,
                                isMuted = isMuted,
                                playbackSpeed = effectiveSpeed,
                                activeSkipSegment = activeSkipSegment,
                                onSeek = { target ->
                                    positionMs = target
                                    requestedSeekMs = target
                                    hudInteractionTick++
                                },
                                onSkipSegment = { seg ->
                                    positionMs = seg.endMs
                                    requestedSeekMs = seg.endMs
                                }
                            )
                        }
                    }
                }

                // Layer 100: In-Tree Compose Modal Sheet for iOS/Android (100% reliable touch above UIKitView!)
                if (activeSheet != PlayerSheet.NONE) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(100f)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .clickable { activeSheet = PlayerSheet.NONE }
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 440.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xF5111622))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                                .clickable(enabled = false) {}
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = when (activeSheet) {
                                        PlayerSheet.SUBTITLES -> "Subtitles"
                                        PlayerSheet.AUDIO_SPEED -> "Volume, Audio & Speed"
                                        PlayerSheet.EXTERNAL -> "External Player"
                                        PlayerSheet.NONE -> ""
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                PlezyCleanIconButton(
                                    icon = Icons.Default.Close,
                                    contentDescription = "Close",
                                    iconSize = 18.dp,
                                    onClick = { activeSheet = PlayerSheet.NONE }
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            SheetBodyContent(activeSheet)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlezyTopHeader(
    title: String,
    subtitle: String,
    isLocked: Boolean,
    showLockIcon: Boolean,
    showFullscreenIcon: Boolean,
    subtitlesActive: Boolean,
    isBoosted: Boolean,
    isMuted: Boolean,
    onBack: () -> Unit,
    onOpenAudioSpeed: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onCycleAspect: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenExternal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlezyCleanIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            iconSize = 22.dp,
            onClick = onBack
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Visible Volume / Boost button on all platforms
            PlezyCleanIconButton(
                icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Volume & Boost",
                tint = if (isBoosted) NaphColors.AmberWarn else Color.White,
                onClick = onOpenAudioSpeed
            )
            PlezyCleanIconButton(
                icon = Icons.Default.Tune,
                contentDescription = "Audio & Speed",
                onClick = onOpenAudioSpeed
            )
            PlezyCleanIconButton(
                icon = Icons.Default.Subtitles,
                contentDescription = "Subtitles",
                tint = if (subtitlesActive) NaphColors.CyanGlow else Color.White,
                onClick = onOpenSubtitles
            )
            PlezyCleanIconButton(
                icon = Icons.Default.AspectRatio,
                contentDescription = "Aspect Ratio",
                onClick = onCycleAspect
            )
            if (showFullscreenIcon) {
                PlezyCleanIconButton(
                    icon = Icons.Default.Fullscreen,
                    contentDescription = "Toggle Fullscreen (F)",
                    onClick = onToggleFullscreen
                )
            }
            if (showLockIcon) {
                PlezyCleanIconButton(
                    icon = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Lock Screen",
                    onClick = onToggleLock
                )
            }
            PlezyCleanIconButton(
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "External Player",
                onClick = onOpenExternal
            )
        }
    }
}

@Composable
private fun PlezyScrubberSection(
    positionMs: Long,
    durationMs: Long,
    volume: Float,
    isMuted: Boolean,
    playbackSpeed: Float,
    activeSkipSegment: SkipSegment?,
    onSeek: (Long) -> Unit,
    onSkipSegment: (SkipSegment) -> Unit
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val fraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
    val volPct = (volume * 100f).roundToInt()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (activeSkipSegment != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .clickable { onSkipSegment(activeSkipSegment) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = activeSkipSegment.type.label,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        PlezyRazorScrubber(
            fraction = fraction,
            onSeekFraction = { newFrac ->
                if (durationMs > 0L) {
                    onSeek((newFrac * durationMs).toLong())
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatPlayerTime(positionMs),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isMuted && volPct > 102) {
                    Text(
                        text = "${volPct}%",
                        color = NaphColors.AmberWarn,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (abs(playbackSpeed - 1.0f) > 0.01f) {
                    Text(
                        text = "${playbackSpeed}x",
                        color = NaphColors.CyanGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "-${formatPlayerTime(remainingMs)}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PlezyRazorScrubber(
    fraction: Float,
    onSeekFraction: (Float) -> Unit,
    activeColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    var widthPx by remember { mutableStateOf(1f) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val displayFrac = (dragFraction ?: fraction).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(widthPx) {
                detectTapGestures { offset ->
                    onSeekFraction((offset.x / widthPx).coerceIn(0f, 1f))
                }
            }
            .pointerInput(widthPx) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragFraction = (offset.x / widthPx).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragFraction?.let { onSeekFraction(it) }
                        dragFraction = null
                    },
                    onDragCancel = {
                        dragFraction = null
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
            val trackHeight = 3.5.dp.toPx()
            val centerY = size.height / 2f
            val corner = CornerRadius(trackHeight / 2f, trackHeight / 2f)

            drawRoundRect(
                color = Color.White.copy(alpha = 0.30f),
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(size.width, trackHeight),
                cornerRadius = corner
            )

            val activeW = (size.width * displayFrac).coerceIn(0f, size.width)
            if (activeW > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2f),
                    size = Size(activeW, trackHeight),
                    cornerRadius = corner
                )
            }

            val thumbW = 3.dp.toPx()
            val thumbH = 16.dp.toPx()
            val thumbX = activeW.coerceIn(thumbW / 2f, size.width - thumbW / 2f) - thumbW / 2f
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(thumbX, centerY - thumbH / 2f),
                size = Size(thumbW, thumbH),
                cornerRadius = CornerRadius(thumbW / 2f, thumbW / 2f)
            )
        }
    }
}

@Composable
private fun PlezyCleanIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    iconSize: Dp = 21.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
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
                modifier = Modifier.size(36.dp)
            )
            CircularProgressIndicator(
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.size(32.dp)
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
    val ss = seconds.toString().padStart(2, '0')
    return if (hours > 0L) {
        "$hours:${minutes.toString().padStart(2, '0')}:$ss"
    } else {
        "$minutes:$ss"
    }
}
