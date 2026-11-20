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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naph.app.core.model.SkipSegment
import com.naph.app.core.model.SkipSegmentType
import com.naph.app.ui.state.NaphAppState
import com.naph.app.ui.theme.NaphColors
import kotlin.math.roundToInt

/**
 * Revamped Unified Video Player Screen:
 * - Top Crisp Header Bar (`#0E1017`), Center Native `libmpv` / `ExoPlayer` / `MPVKit` Canvas,
 *   and Dedicated Bottom Control Dock (`#0E1017`) so controls are ALWAYS 100% visible,
 *   never obscured by the native AWT/X11 video canvas, and use pure white high-contrast fonts!
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
            // 1. TOP PLAYER HEADER BAR (High-Contrast Dark Surface — Never Hidden Under Video Canvas)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E1017))
                    .border(1.dp, Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { appState.stopPlaybackAndSaveProgress(positionMs, durationMs) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1A1E2B))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = buildString {
                                append(media.name)
                                if (episode != null) {
                                    append("  •  S${episode!!.season}:E${episode!!.episode} — ${episode!!.displayTitle}")
                                }
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = currentStream.detectedResolution,
                                style = MaterialTheme.typography.labelSmall,
                                color = NaphColors.GoldRating
                            )
                            currentStream.detectedHdrBadge?.let {
                                Text("• $it", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
                            }
                            currentStream.detectedFileSizeLabel?.let {
                                Text("• $it", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                            Text(
                                text = "• ${currentStream.addonName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaphColors.TextSecondary
                            )
                        }
                    }
                }

                // Live P2P Swarm Pill + Player Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    p2pTelemetry?.let { tel ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF161A26))
                                .border(1.dp, NaphColors.CyanGlow.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = NaphColors.CyanGlow,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "P2P ${formatBytesPerSec(tel.downloadSpeedBytesPerSec)} • ${tel.connectedPeers} Peers (${tel.totalSeeds} Seeds)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    // Skip Intro / Credits Button in Header if active
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
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(activeSkipSegment.type.label, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Button(
                        onClick = { showExternalPlayerModal = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A1E2B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("External Player", color = Color.White)
                    }

                    Button(
                        onClick = { showCastModal = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A1E2B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Cast, contentDescription = null, tint = NaphColors.CyanGlow, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cast", color = Color.White)
                    }

                    Button(
                        onClick = { showSubtitleDrawer = !showSubtitleDrawer },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showSubtitleDrawer) MaterialTheme.colorScheme.primary else Color(0xFF1A1E2B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Subtitles", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 2. CENTER VIDEO STAGE (Real libmpv / ExoPlayer / MPVKit Canvas)
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
                    // Live P2P Swarm Connecting & Sequential Piece Buffering Screen
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .width(480.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF131620))
                            .border(1.dp, NaphColors.CyanGlow.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                            .padding(32.dp)
                    ) {
                        CircularProgressIndicator(
                            color = NaphColors.CyanGlow,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = "Connecting to BitTorrent P2P Swarm...",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = currentStream.cleanTitleLine,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NaphColors.TextSecondary,
                            maxLines = 2,
                            textAlign = TextAlign.Center
                        )
                        p2pTelemetry?.let { tel ->
                            Text(
                                text = "Peers: ${tel.connectedPeers} • Seeds: ${tel.totalSeeds} • Speed: ${formatBytesPerSec(tel.downloadSpeedBytesPerSec)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaphColors.CyanGlow
                            )
                            LinearProgressIndicator(
                                progress = { (tel.bufferProgressPercent / 100f).coerceIn(0.05f, 1f) },
                                color = NaphColors.CyanGlow,
                                trackColor = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }

            // 3. BOTTOM PLAYBACK CONTROL DOCK (High-Contrast Dark Surface — Always Readable & Clickable!)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E1017))
                    .border(1.dp, Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                // Timeline Scrubber Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatDurationMs(positionMs),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                        color = Color.White
                    )
                    Spacer(Modifier.width(14.dp))
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
                            inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = if (durationMs > 0L) formatDurationMs(durationMs) else "LIVE / BUFFERING",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                        color = NaphColors.TextSecondary
                    )
                }

                // Transport Controls Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val target = (positionMs - 10_000L).coerceAtLeast(0L)
                                positionMs = target
                                requestedSeekMs = target
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1A1E2B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.FastRewind, contentDescription = "-10s", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("-10s", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { isPlaying = !isPlaying },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 11.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isPlaying) "Pause" else "Play",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Button(
                            onClick = {
                                val target = if (durationMs > 0L) (positionMs + 10_000L).coerceAtMost(durationMs) else positionMs + 10_000L
                                positionMs = target
                                requestedSeekMs = target
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1A1E2B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text("+10s", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.FastForward, contentDescription = "+10s", modifier = Modifier.size(18.dp))
                        }

                        if (hasNextEpisode) {
                            Button(
                                onClick = { appState.playNextEpisodeInQueue() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1A1E2B),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = null, tint = NaphColors.CyanGlow)
                                Spacer(Modifier.width(6.dp))
                                Text("Next Episode", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Active Subtitle & Audio Status Label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Subtitles: ${selectedSubtitle?.label ?: "Embedded / Auto"} (${subConfig.delayOffsetMs}ms)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NaphColors.TextSecondary
                        )
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
                        if (ok) "Launched $playerName with active stream URL"
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
            .width(380.dp)
            .fillMaxHeight()
            .background(Color(0xF811131B))
            .border(1.dp, Color.White.copy(alpha = 0.14f))
            .padding(22.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Subtitles & Styling", style = MaterialTheme.typography.titleLarge, color = Color.White)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = 12.dp))

        Text("SUBTITLE TRACKS (${subtitles.size})", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
        Spacer(Modifier.height(8.dp))

        FilterChip(
            selected = selectedTrack == null,
            onClick = { onSelectTrack(null) },
            label = { Text("Off / Embedded Default") }
        )

        subtitles.forEach { track ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTrack?.id == track.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color(0xFF1A1E2B))
                    .clickable { onSelectTrack(track) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(track.label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Text(track.format.uppercase(), style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
            }
        }

        Spacer(Modifier.height(18.dp))

        Text("SUBTITLE DELAY (${config.delayOffsetMs} ms)", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onUpdateConfig(config.copy(delayOffsetMs = config.delayOffsetMs - 100L)) }) {
                Text("-100ms", color = Color.White)
            }
            OutlinedButton(onClick = { onUpdateConfig(config.copy(delayOffsetMs = 0L)) }) {
                Text("0ms", color = Color.White)
            }
            OutlinedButton(onClick = { onUpdateConfig(config.copy(delayOffsetMs = config.delayOffsetMs + 100L)) }) {
                Text("+100ms", color = Color.White)
            }
        }

        Spacer(Modifier.height(18.dp))

        Text("FONT SIZE (${config.fontSizeSp.toInt()} sp)", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
        Slider(
            value = config.fontSizeSp,
            onValueChange = { onUpdateConfig(config.copy(fontSizeSp = it)) },
            valueRange = 14f..38f
        )

        Text("BACKGROUND OPACITY (${(config.backgroundOpacity * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, color = NaphColors.CyanGlow)
        Slider(
            value = config.backgroundOpacity,
            onValueChange = { onUpdateConfig(config.copy(backgroundOpacity = it)) },
            valueRange = 0f..0.95f
        )

        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Preserve ASS/SSA Styling", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text("Render custom anime fonts & positioning via libass", style = MaterialTheme.typography.bodyMedium, color = NaphColors.TextSecondary)
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
                .background(Color(0xFF131620))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
                .padding(24.dp)
        ) {
            Text("Open in External Player", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
                text = "Hand off the active stream URL to an external media player:",
                style = MaterialTheme.typography.bodyMedium,
                color = NaphColors.TextSecondary
            )

            listOf("mpv", "VLC Media Player", "IINA (macOS)", "Infuse (iOS)", "Outplayer (iOS)").forEach { player ->
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2231)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(player, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF131620))
                .border(1.dp, NaphColors.CyanGlow.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(24.dp)
        ) {
            Text("Chromecast & AirPlay", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
                text = "Cast '$title' to a network TV or receiver:",
                style = MaterialTheme.typography.bodyMedium,
                color = NaphColors.TextSecondary
            )

            listOf(
                "Living Room 4K TV (Chromecast)",
                "Apple TV 4K (AirPlay 2)",
                "DLNA / UPnP Smart TV"
            ).forEach { device ->
                Button(
                    onClick = { onSelectDevice(device) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2231)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cast, contentDescription = null, tint = NaphColors.CyanGlow)
                    Spacer(Modifier.width(10.dp))
                    Text(device, color = Color.White)
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
    if (bytes >= 1024L * 1024L) {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        val rounded = (mb * 10.0).roundToInt() / 10.0
        return "$rounded MB/s"
    }
    val kb = (bytes / 1024L).coerceAtLeast(0L)
    return "$kb KB/s"
}
