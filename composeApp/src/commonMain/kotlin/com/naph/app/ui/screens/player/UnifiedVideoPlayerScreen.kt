package com.naph.app.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
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
import com.naph.app.ui.state.NaphAppState
import com.naph.app.ui.theme.NaphColors
import kotlin.math.roundToInt

/**
 * Sleek Modern Minimalist Cinema Video Player (Apple TV+ / IINA / Netflix style):
 * - Ultra-slim 48dp top header bar with circular icon pills (no giant chunky text buttons!)
 * - Full-bleed native hardware `libmpv` video canvas
 * - Single-row 54dp bottom transport bar with circular Play/Pause, icon-only ±10s, integrated slim timeline scrubber,
 *   and compact subtitle / P2P speed pill.
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

    var showSubtitleDrawer by remember { mutableStateOf(false) }
    var showExternalPlayerModal by remember { mutableStateOf(false) }
    var showCastModal by remember { mutableStateOf(false) }
    var autoSkippedIds by remember { mutableStateOf(setOf<SkipSegmentType>()) }

    // Automatic IntroDB / AniSkip Check when real position advances
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. ULTRA-SLIM MODERN TOP BAR (48.dp height, zero chunky buttons)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF08090D))
                    .padding(horizontal = 16.dp)
            ) {
                // Left: Circular Back Icon + Crisp Title & Compact Tech Tags
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ModernIconPill(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = { appState.stopPlaybackAndSaveProgress(positionMs, durationMs) },
                        size = 32.dp
                    )

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = buildString {
                            append(media.name)
                            if (episode != null) {
                                append("  •  S${episode!!.season}:E${episode!!.episode} ${episode!!.displayTitle}")
                            }
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.width(10.dp))

                    // Subtle Quality Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = buildString {
                                append(currentStream.detectedResolution)
                                currentStream.detectedHdrBadge?.let { append(" • $it") }
                                currentStream.detectedFileSizeLabel?.let { append(" • $it") }
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = NaphColors.TextSecondary
                        )
                    }
                }

                // Right: Compact P2P Speed Pill + Icon-Only Action Pills (No bulky text buttons!)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    p2pTelemetry?.let { tel ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF121520))
                                .border(1.dp, NaphColors.CyanGlow.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = NaphColors.CyanGlow,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "${formatBytesPerSec(tel.downloadSpeedBytesPerSec)} • ${tel.connectedPeers}p",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = Color.White
                            )
                        }
                    }

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
                    }

                    ModernIconPill(
                        icon = Icons.Default.Subtitles,
                        contentDescription = "Subtitles",
                        isActive = showSubtitleDrawer,
                        onClick = { showSubtitleDrawer = !showSubtitleDrawer },
                        size = 32.dp
                    )

                    ModernIconPill(
                        icon = Icons.Default.Cast,
                        contentDescription = "Cast / AirPlay",
                        tint = NaphColors.CyanGlow,
                        onClick = { showCastModal = true },
                        size = 32.dp
                    )

                    ModernIconPill(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open in External Player",
                        onClick = { showExternalPlayerModal = true },
                        size = 32.dp
                    )
                }
            }

            // 2. CENTER VIDEO STAGE (Full-bleed Native libmpv / ExoPlayer / MPVKit Canvas)
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
                        subtitleDelayMs = subConfig.delayOffsetMs,
                        onPositionChanged = { pos, dur ->
                            positionMs = pos
                            durationMs = dur
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Minimal Sleek P2P Buffering Pill (Lightweight & Non-Chunky)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .width(320.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0E1119))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
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
                                text = "Buffering P2P Stream...",
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
                                trackColor = Color.White.copy(alpha = 0.10f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }

            // 3. ULTRA-SLIM SINGLE-ROW BOTTOM TRANSPORT BAR (52.dp height — Sleek Circular Icons + Timeline)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(Color(0xFF08090D))
                    .padding(horizontal = 16.dp)
            ) {
                // Left: Compact Circular Transport Controls (Rewind 10s, Play/Pause, Forward 10s, Next Ep)
                ModernIconPill(
                    icon = Icons.Default.FastRewind,
                    contentDescription = "-10s",
                    size = 32.dp,
                    onClick = {
                        val target = (positionMs - 10_000L).coerceAtLeast(0L)
                        positionMs = target
                        requestedSeekMs = target
                    }
                )

                // Sleek White Circular Play / Pause Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { isPlaying = !isPlaying }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                ModernIconPill(
                    icon = Icons.Default.FastForward,
                    contentDescription = "+10s",
                    size = 32.dp,
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
                        size = 32.dp,
                        onClick = { appState.playNextEpisodeInQueue() }
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Current Time
                Text(
                    text = formatDurationMs(positionMs),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White
                )

                // Center: Slim Integrated Scrubber
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
                    modifier = Modifier.weight(1f)
                )

                // Total Duration
                Text(
                    text = if (durationMs > 0L) formatDurationMs(durationMs) else "--:--",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = NaphColors.TextSecondary
                )

                // Right: Compact Subtitle Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable { showSubtitleDrawer = !showSubtitleDrawer }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "CC: ${selectedSubtitle?.lang?.uppercase() ?: "AUTO"}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = NaphColors.TextSecondary
                    )
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
                        else "Stream URL: ${playableUrl ?: currentStream.url}"
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
            .width(340.dp)
            .fillMaxHeight()
            .background(Color(0xF80D0F16))
            .border(1.dp, Color.White.copy(alpha = 0.10f))
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
                size = 28.dp
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedTrack?.id == track.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color(0xFF141722))
                    .clickable { onSelectTrack(track) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(track.label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp), color = Color.White)
                Text(track.format.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = NaphColors.CyanGlow)
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("DELAY SYNC (${config.delayOffsetMs} ms)", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModernGlassButton(text = "-100ms", height = 30.dp, onClick = { onUpdateConfig(config.copy(delayOffsetMs = config.delayOffsetMs - 100L)) })
            ModernGlassButton(text = "0ms", height = 30.dp, onClick = { onUpdateConfig(config.copy(delayOffsetMs = 0L)) })
            ModernGlassButton(text = "+100ms", height = 30.dp, onClick = { onUpdateConfig(config.copy(delayOffsetMs = config.delayOffsetMs + 100L)) })
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
                .width(360.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF10131C))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                .padding(20.dp)
        ) {
            Text("External Player", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                text = "Hand off stream to an installed player:",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = NaphColors.TextSecondary
            )

            listOf("mpv", "VLC Media Player", "IINA (macOS)", "Infuse (iOS)", "Outplayer (iOS)").forEach { player ->
                ModernGlassButton(
                    text = player,
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
                .width(360.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF10131C))
                .border(1.dp, NaphColors.CyanGlow.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
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
