package com.naph.app.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naph.app.core.model.SkipSegment
import com.naph.app.core.model.SkipSegmentType
import com.naph.app.ui.state.NaphAppState
import com.naph.app.ui.theme.NaphColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

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
    var positionMs by remember { mutableStateOf(40_000L) }
    var durationMs by remember { mutableStateOf(2_400_000L) }
    var requestedSeekMs by remember { mutableStateOf<Long?>(null) }

    var showSubtitleDrawer by remember { mutableStateOf(false) }
    var showExternalPlayerModal by remember { mutableStateOf(false) }
    var showCastModal by remember { mutableStateOf(false) }
    var autoSkippedIds by remember { mutableStateOf(setOf<SkipSegmentType>()) }

    // Advance playback clock & handle automatic IntroDB / AniSkip when autoSkipIntros is enabled
    LaunchedEffect(isPlaying, playableUrl) {
        while (true) {
            delay(500)
            if (isPlaying && playableUrl != null) {
                positionMs = (positionMs + 500L).coerceAtMost(durationMs)

                val matchingSegment = skipSegments.find { positionMs in it.startMs..it.endMs }
                if (matchingSegment != null &&
                    activeProfile.autoSkipIntros &&
                    matchingSegment.type !in autoSkippedIds &&
                    positionMs >= matchingSegment.startMs + 2500L
                ) {
                    autoSkippedIds = autoSkippedIds + matchingSegment.type
                    positionMs = matchingSegment.endMs + 500L
                    requestedSeekMs = positionMs
                    appState.showBanner("Auto-Skipped ${matchingSegment.type.label.removePrefix("Skip ")} (IntroDB)")
                }
            }
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Native Video Playback Surface (libmpv / ExoPlayer / MPVKit)
        if (playableUrl != null) {
            NativeVideoSurface(
                streamUrl = playableUrl!!,
                isPlaying = isPlaying,
                seekToMs = requestedSeekMs,
                subtitleDelayMs = subConfig.delayOffsetMs,
                onPositionChanged = { pos, dur ->
                    if (dur > 0) {
                        positionMs = pos
                        durationMs = dur
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Sequential P2P Swarm Header Pre-Buffer Screen
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(NaphColors.GlassSurface)
                        .border(1.dp, NaphColors.CyanGlow.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(32.dp)
                ) {
                    CircularProgressIndicator(color = NaphColors.CyanGlow)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "NAPH Sequential P2P Engine • Pre-Buffering Header Pieces...",
                        style = MaterialTheme.typography.titleLarge,
                        color = NaphColors.CyanGlow
                    )
                    Spacer(Modifier.height(8.dp))
                    p2pTelemetry?.let { tel ->
                        Text(
                            text = "Connected Peers: ${tel.connectedPeers} (${tel.totalSeeds} Seeds) • Speed: ${formatBytesPerSec(tel.downloadSpeedBytesPerSec)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NaphColors.TextPrimary
                        )
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { (tel.bufferProgressPercent / 100f).coerceIn(0f, 1f) },
                            color = NaphColors.CyanGlow,
                            trackColor = NaphColors.ElevatedBorder,
                            modifier = Modifier
                                .width(360.dp)
                                .height(6.dp)
                        )
                    }
                }
            }
        }

        // 2. Live Styled ASS/SSA & SRT Subtitle Renderer Overlay
        if (selectedSubtitle != null && playableUrl != null) {
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 115.dp)
                    .offset { IntOffset(0, -subConfig.verticalOffsetDp.roundToInt()) }
            ) {
                val sampleLine = when (selectedSubtitle?.lang) {
                    "jpn" -> "「NAPH シーケンシャル P2P & libass スタイル字幕エンジン」"
                    "spa" -> "« Motor P2P secuencial NAPH • Subtítulos estilizados en vivo »"
                    else -> "“We must not fear. Fear is the mind-killer.” [${selectedSubtitle?.label} • Delay: ${subConfig.delayOffsetMs}ms]"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = subConfig.backgroundOpacity))
                        .border(
                            width = (subConfig.outlineWidthPx / 2f).dp,
                            color = Color(subConfig.outlineColorHex).copy(alpha = 0.65f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = sampleLine,
                        fontSize = subConfig.fontSizeSp.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = if (subConfig.fontFamily == "Monospace") FontFamily.Monospace
                        else if (subConfig.fontFamily == "Serif") FontFamily.Serif
                        else FontFamily.SansSerif,
                        color = Color(subConfig.textColorHex),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 3. Top Header Overlay (Title + Real-Time P2P Swarm Telemetry + Cast & External Player)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xE6070709), Color.Transparent)
                    )
                )
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { appState.stopPlaybackAndSaveProgress(positionMs, durationMs) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NaphColors.GlassSurface)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Player", tint = Color.White)
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = buildString {
                            append(media.name)
                            if (episode != null) {
                                append(" • S${episode!!.season}:E${episode!!.episode} — ${episode!!.displayTitle}")
                            }
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${currentStream.detectedResolution} ${currentStream.detectedHdrBadge.orEmpty()} • ${currentStream.addonName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NaphColors.CyanGlow
                    )
                }
            }

            // Live P2P Swarm Stats Pill + Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                p2pTelemetry?.let { tel ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NaphColors.GlassSurface)
                            .border(1.dp, NaphColors.CyanGlow.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = NaphColors.CyanGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "P2P ${formatBytesPerSec(tel.downloadSpeedBytesPerSec)} • ${tel.connectedPeers} Peers • Temp Cache",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaphColors.TextPrimary
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showExternalPlayerModal = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = NaphColors.TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("External Player", color = NaphColors.TextPrimary)
                }

                OutlinedButton(
                    onClick = { showCastModal = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Cast, contentDescription = null, tint = NaphColors.CyanGlow, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cast / AirPlay", color = NaphColors.TextPrimary)
                }

                Button(
                    onClick = { showSubtitleDrawer = !showSubtitleDrawer },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showSubtitleDrawer) NaphColors.ElectricPrimary else NaphColors.CardSurface
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Subtitles & ASS Style")
                }
            }
        }

        // 4. Floating Smart Skip Button (IntroDB / AniSkip: Skip Intro, Skip Recap, Skip Credits)
        if (activeSkipSegment != null) {
            Button(
                onClick = {
                    positionMs = activeSkipSegment.endMs + 500L
                    requestedSeekMs = positionMs
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 128.dp)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${activeSkipSegment.type.label} (IntroDB)",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // 5. Netflix-Style "Up Next Episode" Binge Card (When near end of episode or on demand)
        if (hasNextEpisode && (positionMs >= durationMs - 180_000L || activeSkipSegment?.type == SkipSegmentType.OUTRO)) {
            Button(
                onClick = { appState.playNextEpisodeInQueue() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NaphColors.ElectricPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp, bottom = 128.dp)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Up Next: Play Next Episode", fontWeight = FontWeight.Bold)
            }
        }

        // 6. Bottom Playback Controls & Timeline Scrubber
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xF2070709))
                    )
                )
                .padding(horizontal = 28.dp, vertical = 18.dp)
        ) {
            // Scrubber Slider with Intro/Outro Segment Highlights
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatDurationMs(positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = NaphColors.TextPrimary
                )
                Spacer(Modifier.width(12.dp))
                Slider(
                    value = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f,
                    onValueChange = { frac ->
                        positionMs = (frac * durationMs).toLong()
                        requestedSeekMs = positionMs
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = NaphColors.CyanGlow,
                        activeTrackColor = NaphColors.ElectricPrimary,
                        inactiveTrackColor = NaphColors.ElevatedBorder
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = formatDurationMs(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = NaphColors.TextSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = {
                            positionMs = (positionMs - 10_000L).coerceAtLeast(0L)
                            requestedSeekMs = positionMs
                        }
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "-10s", tint = Color.White)
                    }

                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NaphColors.ElectricPrimary)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            positionMs = (positionMs + 10_000L).coerceAtMost(durationMs)
                            requestedSeekMs = positionMs
                        }
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "+10s", tint = Color.White)
                    }

                    if (hasNextEpisode) {
                        OutlinedButton(
                            onClick = { appState.playNextEpisodeInQueue() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("Next Episode", color = Color.White)
                        }
                    }
                }

                // Quick Jump to Intro / Outro Test Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    skipSegments.forEach { seg ->
                        FilterChip(
                            selected = positionMs in seg.startMs..seg.endMs,
                            onClick = {
                                positionMs = seg.startMs
                                requestedSeekMs = positionMs
                            },
                            label = { Text("Jump to ${seg.type.name} (${formatDurationMs(seg.startMs)})") }
                        )
                    }
                }
            }
        }

        // 7. Live Subtitle Search, Delay Sync & Appearance Editor Drawer
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

        // 8. External Player Handoff Modal (VLC, MPV, IINA, Infuse, Outplayer)
        if (showExternalPlayerModal) {
            ExternalPlayerHandoffDialog(
                streamUrl = playableUrl ?: currentStream.url.orEmpty(),
                title = media.name,
                onDismiss = { showExternalPlayerModal = false },
                onLaunched = { playerName, ok ->
                    showExternalPlayerModal = false
                    appState.showBanner(
                        if (ok) "Launched $playerName with active stream URL"
                        else "Copied stream URL for $playerName: ${playableUrl ?: currentStream.url}"
                    )
                }
            )
        }

        // 9. Chromecast / AirPlay Discovery Modal
        if (showCastModal) {
            CastAndAirPlayDialog(
                title = media.name,
                streamUrl = playableUrl ?: currentStream.url.orEmpty(),
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
            .width(390.dp)
            .fillMaxHeight()
            .background(Color(0xF50E0F14))
            .border(1.dp, NaphColors.ElevatedBorder)
            .padding(22.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Subtitles & libass Live Editor", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Track Selector
        Text("ACTIVE SUBTITLE TRACK", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
        Spacer(Modifier.height(8.dp))

        FilterChip(
            selected = selectedTrack == null,
            onClick = { onSelectTrack(null) },
            label = { Text("Off (Disable Subtitles)") }
        )

        subtitles.forEach { track ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTrack?.id == track.id) NaphColors.ElectricPrimary.copy(alpha = 0.22f) else NaphColors.CardSurface)
                    .clickable { onSelectTrack(track) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(track.label, style = MaterialTheme.typography.bodyMedium, color = NaphColors.TextPrimary)
                Text(track.format.uppercase(), style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
            }
        }

        Spacer(Modifier.height(18.dp))

        // Sync Delay Offset (+/- ms)
        Text("SYNC DELAY OFFSET (${config.delayOffsetMs} ms)", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onUpdateConfig(config.copy(delayOffsetMs = config.delayOffsetMs - 100L)) }) {
                Text("-100ms")
            }
            OutlinedButton(onClick = { onUpdateConfig(config.copy(delayOffsetMs = 0L)) }) {
                Text("Reset (0ms)")
            }
            OutlinedButton(onClick = { onUpdateConfig(config.copy(delayOffsetMs = config.delayOffsetMs + 100L)) }) {
                Text("+100ms")
            }
        }

        Spacer(Modifier.height(18.dp))

        // Font Size Slider
        Text("FONT SIZE (${config.fontSizeSp.toInt()} sp)", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
        Slider(
            value = config.fontSizeSp,
            onValueChange = { onUpdateConfig(config.copy(fontSizeSp = it)) },
            valueRange = 14f..38f
        )

        // Vertical Offset Slider
        Text("VERTICAL POSITION (${config.verticalOffsetDp.toInt()} dp)", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
        Slider(
            value = config.verticalOffsetDp,
            onValueChange = { onUpdateConfig(config.copy(verticalOffsetDp = it)) },
            valueRange = 0f..140f
        )

        // Background Box Opacity
        Text("BACKGROUND BOX OPACITY (${(config.backgroundOpacity * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
        Slider(
            value = config.backgroundOpacity,
            onValueChange = { onUpdateConfig(config.copy(backgroundOpacity = it)) },
            valueRange = 0f..0.95f
        )

        // Font Family
        Text("FONT FAMILY", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Sans-Serif", "Serif", "Monospace").forEach { font ->
                FilterChip(
                    selected = config.fontFamily == font,
                    onClick = { onUpdateConfig(config.copy(fontFamily = font)) },
                    label = { Text(font) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Text Color Presets
        Text("SUBTITLE COLOR", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                0xFFFFFFFF to "White",
                0xFFF5C518 to "Cinema Yellow",
                0xFF06B6D4 to "Cyan",
                0xFF10B981 to "Green"
            ).forEach { (hex, name) ->
                FilterChip(
                    selected = config.textColorHex == hex,
                    onClick = { onUpdateConfig(config.copy(textColorHex = hex)) },
                    label = { Text(name) }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // libass Styling Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Preserve ASS/SSA Typesetting", style = MaterialTheme.typography.titleMedium)
                Text("Render embedded anime fonts & positioning via libass", style = MaterialTheme.typography.bodyMedium)
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(NaphColors.DeepSurface)
                .border(1.dp, NaphColors.ElevatedBorder, RoundedCornerShape(18.dp))
                .padding(24.dp)
        ) {
            Text("Handoff Stream to External Player", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Passes the local HTTP P2P Byte-Range or Debrid stream URL directly to your installed player:",
                style = MaterialTheme.typography.bodyMedium
            )

            listOf("mpv (External Desktop/Mobile)", "VLC Media Player", "IINA (macOS)", "Infuse (iOS / Apple TV)", "Outplayer (iOS)").forEach { player ->
                Button(
                    onClick = {
                        val bin = when {
                            player.startsWith("mpv") -> "mpv"
                            player.startsWith("VLC") -> "vlc"
                            player.startsWith("IINA") -> "iina"
                            else -> "mpv"
                        }
                        val ok = launchExternalPlayer(bin, streamUrl, title)
                        onLaunched(player, ok)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaphColors.CardSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(player, color = NaphColors.TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun CastAndAirPlayDialog(
    title: String,
    streamUrl: String,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(NaphColors.DeepSurface)
                .border(1.dp, NaphColors.CyanGlow.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(24.dp)
        ) {
            Text("Chromecast & AirPlay Discovery", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Stream '$title' directly to a local network TV or receiver:",
                style = MaterialTheme.typography.bodyMedium
            )

            listOf(
                "Living Room 4K TV (Chromecast Ultra)",
                "Apple TV 4K (AirPlay 2 Receiver)",
                "Bedroom NVIDIA Shield TV (Google Cast)",
                "DLNA / UPnP Smart TV Renderer"
            ).forEach { device ->
                Button(
                    onClick = { onSelectDevice(device) },
                    colors = ButtonDefaults.buttonColors(containerColor = NaphColors.CardSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cast, contentDescription = null, tint = NaphColors.CyanGlow)
                    Spacer(Modifier.width(10.dp))
                    Text(device, color = NaphColors.TextPrimary)
                }
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
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    val rounded = (mb * 10.0).roundToInt() / 10.0
    return "$rounded MB/s"
}
