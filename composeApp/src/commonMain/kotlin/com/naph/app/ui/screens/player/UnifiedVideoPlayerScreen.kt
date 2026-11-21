package com.naph.app.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naph.app.core.model.SkipSegment
import com.naph.app.core.model.SkipSegmentType
import com.naph.app.ui.components.ModernFilterPill
import com.naph.app.ui.components.ModernGlassButton
import com.naph.app.ui.components.ModernIconPill
import com.naph.app.ui.components.ModernPrimaryButton
import com.naph.app.ui.components.liquidGlassBorderBrush
import com.naph.app.ui.state.NaphAppState
import com.naph.app.ui.theme.NaphColors
import kotlin.math.roundToInt

/**
 * Nuvio-Inspired Liquid Glass Cinema Video Player (Responsive for Linux, Windows & iPhone):
 * - Top safe-area Liquid Glass bar with title, resolution/P2P pill, Speed selector, Subtitles, Cast & External Player
 * - Center full-bleed hardware video surface (`libmpv` 400MB cache on Linux/Windows, `AVPlayer` PiP on iOS)
 * - Bottom safe-area Nuvio transport dock with thin timeline scrubber, elapsed/remaining time,
 *   centered Liquid Glass transport pills (-10s, Play/Pause, +10s, Next Episode) & Volume mute toggle
 * - Desktop keyboard shortcuts: Space (Play/Pause), Left/Right (±10s), Escape (Back)
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

    val speedOptions = remember { listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f) }
    var speedIndex by remember { mutableStateOf(1) } // 1.0x default
    val playbackSpeed = speedOptions[speedIndex]

    var isMuted by remember { mutableStateOf(false) }
    val effectiveVolume = if (isMuted) 0f else 1f

    var showSubtitleDrawer by remember { mutableStateOf(false) }
    var showExternalPlayerModal by remember { mutableStateOf(false) }
    var showCastModal by remember { mutableStateOf(false) }
    var autoSkippedIds by remember { mutableStateOf(setOf<SkipSegmentType>()) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    // Automatic IntroDB / AniSkip Check
    LaunchedEffect(positionMs) {
        val matchingSegment = skipSegments.find { positionMs in it.startMs..it.endMs }
        if (matchingSegment != null &&
            activeProfile.autoSkipIntros &&
            matchingSegment.type !in autoSkippedIds &&
            positionMs >= matchingSegment.startMs + 2000L
        ) {
            autoSkippedIds = autoSkippedIds + matchingSegment.type
            positionMs = matchingSegment.endMs + 500L
            requestedSeekMs = positionMs
            appState.showBanner("Auto-Skipped ${matchingSegment.type.label.removePrefix("Skip ")}")
        }
    }

    val activeSkipSegment: SkipSegment? = remember(positionMs, skipSegments) {
        skipSegments.find { positionMs in it.startMs..it.endMs }
    }

    val hasNextEpisode = remember(media, episode) {
        if (episode == null) false
        else {
            val idx = media.videos.indexOfFirst { it.id == episode!!.id }
            idx >= 0 && idx + 1 < media.videos.size
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Spacebar -> {
                            isPlaying = !isPlaying
                            true
                        }
                        Key.DirectionLeft -> {
                            val t = (positionMs - 10_000L).coerceAtLeast(0L)
                            positionMs = t
                            requestedSeekMs = t
                            true
                        }
                        Key.DirectionRight -> {
                            val t = if (durationMs > 0L) (positionMs + 10_000L).coerceAtMost(durationMs) else positionMs + 10_000L
                            positionMs = t
                            requestedSeekMs = t
                            true
                        }
                        Key.Escape -> {
                            appState.stopPlaybackAndSaveProgress(positionMs, durationMs)
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        val isCompact = maxWidth < 640.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. NUVIO LIQUID GLASS TOP HEADER BAR (Safe-Area Aware for iPhone Dynamic Island & Desktop)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0A0D14), Color(0xFF06070B))
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = if (isCompact) 12.dp else 18.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ModernIconPill(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = { appState.stopPlaybackAndSaveProgress(positionMs, durationMs) },
                        size = 34.dp
                    )

                    Spacer(Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = media.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = if (isCompact) 13.5.sp else 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val metaLine = buildString {
                            if (episode != null) {
                                append("S${episode!!.season}:E${episode!!.episode} • ")
                            }
                            append(currentStream.detectedResolution)
                            currentStream.detectedFileSizeLabel?.let { append(" • $it") }
                            p2pTelemetry?.let { tel ->
                                append(" • ${formatBytesPerSec(tel.downloadSpeedBytesPerSec)} (${tel.connectedPeers}p)")
                            }
                        }
                        Text(
                            text = metaLine,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = NaphColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Right Liquid Glass Control Pills (Speed, Subtitles, Cast, External Player)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    // Nuvio-style Playback Speed Pill
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xCC1A2030))
                            .border(1.dp, liquidGlassBorderBrush(), RoundedCornerShape(16.dp))
                            .clickable { speedIndex = (speedIndex + 1) % speedOptions.size }
                            .padding(horizontal = 10.dp)
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (playbackSpeed != 1.0f) NaphColors.CyanGlow else Color.White
                        )
                    }

                    ModernIconPill(
                        icon = Icons.Default.Subtitles,
                        contentDescription = "Subtitles",
                        isActive = showSubtitleDrawer,
                        onClick = { showSubtitleDrawer = !showSubtitleDrawer },
                        size = 32.dp
                    )

                    if (!isCompact) {
                        ModernIconPill(
                            icon = Icons.Default.Cast,
                            contentDescription = "Cast / AirPlay",
                            tint = NaphColors.CyanGlow,
                            onClick = { showCastModal = true },
                            size = 32.dp
                        )
                    }

                    ModernIconPill(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "External Player",
                        onClick = { showExternalPlayerModal = true },
                        size = 32.dp
                    )
                }
            }

            // 2. CENTER FULL-BLEED HARDWARE VIDEO STAGE
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                if (!playableUrl.isNullOrBlank()) {
                    NativeVideoSurface(
                        streamUrl = playableUrl!!,
                        isPlaying = isPlaying,
                        seekToMs = requestedSeekMs,
                        playbackSpeed = playbackSpeed,
                        volume = effectiveVolume,
                        subtitleUrl = selectedSubtitle?.url,
                        subtitleDelayMs = subConfig.delayOffsetMs,
                        onPositionChanged = { pos, dur ->
                            positionMs = pos
                            durationMs = dur
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Liquid Glass Buffering Pill
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .width(if (isCompact) 280.dp else 330.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xD9121622))
                            .border(1.dp, liquidGlassBorderBrush(), RoundedCornerShape(16.dp))
                            .padding(horizontal = 22.dp, vertical = 18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = NaphColors.CyanGlow,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Starting Stream...",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.5.sp),
                                color = Color.White
                            )
                        }
                        p2pTelemetry?.let { tel ->
                            Text(
                                text = "${formatBytesPerSec(tel.downloadSpeedBytesPerSec)} • ${tel.connectedPeers} peers (${tel.totalSeeds} seeds)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = NaphColors.CyanGlow
                            )
                            LinearProgressIndicator(
                                progress = { (tel.bufferProgressPercent / 100f).coerceIn(0.05f, 1f) },
                                color = NaphColors.CyanGlow,
                                trackColor = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }

            // 3. NUVIO LIQUID GLASS BOTTOM TRANSPORT DOCK (Safe-Area Aware for iPhone Home Indicator)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF07090F), Color(0xFF0B0E17))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = if (isCompact) 14.dp else 20.dp, vertical = 8.dp)
            ) {
                // Row 1: Ultra-Clean Timeline Scrubber + Elapsed / Remaining Timestamps
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatDurationMs(positionMs),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )

                    Slider(
                        value = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                        onValueChange = { frac ->
                            if (durationMs > 0L) {
                                val target = (frac * durationMs).toLong()
                                positionMs = target
                                requestedSeekMs = target
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                    )

                    val remainingMs = if (durationMs > positionMs) durationMs - positionMs else 0L
                    Text(
                        text = if (durationMs > 0L) "-${formatDurationMs(remainingMs)}" else "--:--",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = NaphColors.TextSecondary
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Row 2: Centered Nuvio Transport Controls + Left Volume + Right Skip / CC
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left: Volume Mute Toggle
                    ModernIconPill(
                        icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = if (isMuted) NaphColors.CrimsonPlay else Color.White,
                        size = 32.dp,
                        onClick = { isMuted = !isMuted }
                    )

                    // Center: Rewind 10s, White Circular Play/Pause, Forward 10s, Next Episode
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ModernIconPill(
                            icon = Icons.Default.FastRewind,
                            contentDescription = "-10s",
                            size = 34.dp,
                            onClick = {
                                val target = (positionMs - 10_000L).coerceAtLeast(0L)
                                positionMs = target
                                requestedSeekMs = target
                            }
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { isPlaying = !isPlaying }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        ModernIconPill(
                            icon = Icons.Default.FastForward,
                            contentDescription = "+10s",
                            size = 34.dp,
                            onClick = {
                                val target = if (durationMs > 0L) (positionMs + 10_000L).coerceAtMost(durationMs) else positionMs + 10_000L
                                positionMs = target
                                requestedSeekMs = target
                            }
                        )

                        if (hasNextEpisode) {
                            ModernIconPill(
                                icon = Icons.Default.SkipNext,
                                contentDescription = "Next Episode",
                                tint = NaphColors.CyanGlow,
                                size = 34.dp,
                                onClick = { appState.playNextEpisodeInQueue() }
                            )
                        }
                    }

                    // Right: Skip Intro pill (if active) or CC Badge
                    if (activeSkipSegment != null) {
                        ModernPrimaryButton(
                            text = activeSkipSegment.type.label,
                            icon = Icons.Default.SkipNext,
                            height = 30.dp,
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            onClick = {
                                positionMs = activeSkipSegment.endMs + 500L
                                requestedSeekMs = positionMs
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xCC1A2030))
                                .border(1.dp, liquidGlassBorderBrush(), RoundedCornerShape(10.dp))
                                .clickable { showSubtitleDrawer = !showSubtitleDrawer }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "CC: ${selectedSubtitle?.lang?.uppercase() ?: "AUTO"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 4. Live Subtitle Search, Delay Sync & Appearance Editor Drawer
        if (showSubtitleDrawer) {
            LiveSubtitleEditorDrawer(
                subtitles = subtitles,
                selectedTrack = selectedSubtitle,
                config = subConfig,
                onSelectTrack = { appState.selectSubtitleTrack(it) },
                onUpdateConfig = { appState.updateSubtitleConfig(it) },
                onClose = { showSubtitleDrawer = false },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // 5. External Player Handoff Modal
        if (showExternalPlayerModal) {
            ExternalPlayerHandoffDialog(
                streamUrl = playableUrl ?: currentStream.url.orEmpty(),
                title = media.name,
                onDismiss = { showExternalPlayerModal = false },
                onLaunched = { playerName, ok ->
                    showExternalPlayerModal = false
                    appState.showBanner(
                        if (ok) "Launched $playerName"
                        else "Stream URL copied / ready for $playerName"
                    )
                }
            )
        }

        // 6. Chromecast / AirPlay Discovery Modal
        if (showCastModal) {
            CastAndAirPlayDialog(
                title = media.name,
                onDismiss = { showCastModal = false },
                onSelectDevice = { deviceName ->
                    showCastModal = false
                    appState.showBanner("Casting '${media.name}' to $deviceName")
                }
            )
        }
    }
}

@Composable
private fun LiveSubtitleEditorDrawer(
    subtitles: List<com.naph.app.core.model.SubtitleTrack>,
    selectedTrack: com.naph.app.core.model.SubtitleTrack?,
    config: com.naph.app.core.model.SubtitleAppearanceConfig,
    onSelectTrack: (com.naph.app.core.model.SubtitleTrack?) -> Unit,
    onUpdateConfig: (com.naph.app.core.model.SubtitleAppearanceConfig) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight()
            .background(Color(0xF50E121D))
            .border(1.dp, liquidGlassBorderBrush(), RoundedCornerShape(0.dp))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Subtitles & Sync", style = MaterialTheme.typography.titleMedium, color = Color.White)
            ModernIconPill(
                icon = Icons.Default.Close,
                contentDescription = "Close",
                onClick = onClose,
                size = 30.dp
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

        Text("TRACKS (${subtitles.size})", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
        Spacer(Modifier.height(8.dp))

        ModernFilterPill(
            label = "Embedded / Auto",
            selected = selectedTrack == null,
            onClick = { onSelectTrack(null) }
        )
        Spacer(Modifier.height(6.dp))

        subtitles.forEach { track ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTrack?.id == track.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color(0xFF151926))
                    .clickable { onSelectTrack(track) }
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(track.label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp), color = Color.White)
                Text(track.format.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = NaphColors.CyanGlow)
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("DELAY SYNC (${config.delayOffsetMs} ms)", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModernGlassButton(text = "-100ms", height = 32.dp, onClick = { onUpdateConfig(config.copy(delayOffsetMs = config.delayOffsetMs - 100L)) })
            ModernGlassButton(text = "0ms", height = 32.dp, onClick = { onUpdateConfig(config.copy(delayOffsetMs = 0L)) })
            ModernGlassButton(text = "+100ms", height = 32.dp, onClick = { onUpdateConfig(config.copy(delayOffsetMs = config.delayOffsetMs + 100L)) })
        }

        Spacer(Modifier.height(16.dp))

        Text("FONT SIZE (${config.fontSizeSp.toInt()} sp)", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
        Slider(
            value = config.fontSizeSp,
            onValueChange = { onUpdateConfig(config.copy(fontSizeSp = it)) },
            valueRange = 14f..38f
        )

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ASS/SSA Styling", style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp), color = Color.White)
                Text("Render custom anime fonts via libass", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp), color = NaphColors.TextSecondary)
            }
            Switch(
                checked = config.preserveAssStyling,
                onCheckedChange = { onUpdateConfig(config.copy(preserveAssStyling = it)) }
            )
        }
    }
}

@Composable
private fun ExternalPlayerHandoffDialog(
    streamUrl: String,
    title: String,
    onDismiss: () -> Unit,
    onLaunched: (String, Boolean) -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable { onDismiss() }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .width(340.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xEB121622))
                .border(1.dp, liquidGlassBorderBrush(), RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Text("External Player", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                text = "Hand off stream to an installed player:",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = NaphColors.TextSecondary
            )

            listOf("mpv", "VLC Media Player", "Infuse (iOS)", "Outplayer (iOS)", "IINA (macOS)").forEach { player ->
                ModernGlassButton(
                    text = player,
                    onClick = {
                        val bin = when {
                            player.startsWith("mpv") -> "mpv"
                            player.startsWith("VLC") -> "vlc"
                            player.startsWith("Infuse") -> "Infuse"
                            player.startsWith("Outplayer") -> "Outplayer"
                            player.startsWith("IINA") -> "iina"
                            else -> "mpv"
                        }
                        val ok = launchExternalPlayer(bin, streamUrl, title)
                        onLaunched(player, ok)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CastAndAirPlayDialog(
    title: String,
    onDismiss: () -> Unit,
    onSelectDevice: (String) -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable { onDismiss() }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .width(340.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xEB121622))
                .border(1.dp, liquidGlassBorderBrush(NaphColors.CyanGlow), RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Text("Cast & AirPlay", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                text = "Select target display for '$title':",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = NaphColors.TextSecondary
            )

            listOf(
                "Living Room 4K TV (Chromecast)",
                "Apple TV 4K (AirPlay 2)",
                "DLNA / UPnP Smart TV"
            ).forEach { device ->
                ModernGlassButton(
                    text = device,
                    icon = Icons.Default.Cast,
                    iconTint = NaphColors.CyanGlow,
                    onClick = { onSelectDevice(device) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0) {
        "$hrs:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    } else {
        "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    }
}

private fun formatBytesPerSec(bytes: Long): String {
    if (bytes >= 1024L * 1024L) {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        val rounded = (mb * 10.0).roundToInt() / 10.0
        return "$rounded MB/s"
    }
    val kb = (bytes / 1024L).coerceAtLeast(0L)
    return "$kb KB/s"
}
