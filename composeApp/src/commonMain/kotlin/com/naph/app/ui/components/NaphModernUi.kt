package com.naph.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
import com.naph.app.ui.theme.NaphColors

// Pre-cached Brushes & Shapes (Zero per-frame allocations during 120Hz / 120fps scrolling!)
private val CachedLiquidGlassBorder = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.28f),
        Color.White.copy(alpha = 0.07f),
        Color.White.copy(alpha = 0.15f)
    )
)

private val CachedLiquidGlassFill = Brush.verticalGradient(
    colors = listOf(
        Color(0xD9171C2A),
        Color(0xCC0D1018)
    )
)

private val CachedGlassButtonFill = Brush.verticalGradient(
    colors = listOf(
        Color(0xE021283B),
        Color(0xD4131724)
    )
)

private val CachedFloatingBarFill = Brush.verticalGradient(
    colors = listOf(
        Color(0xF01C2234),
        Color(0xE60D111B)
    )
)

private val CachedActiveTabFill = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.24f),
        Color.White.copy(alpha = 0.10f)
    )
)

private val CachedIconPillFill = Brush.verticalGradient(
    colors = listOf(
        Color(0xD9242B3E),
        Color(0xC9121622)
    )
)

private val Shape10 = RoundedCornerShape(10.dp)
private val Shape12 = RoundedCornerShape(12.dp)
private val Shape16 = RoundedCornerShape(16.dp)
private val Shape18 = RoundedCornerShape(18.dp)
private val Shape22 = RoundedCornerShape(22.dp)
private val Shape28 = RoundedCornerShape(28.dp)

fun liquidGlassBorderBrush(accentColor: Color? = null): Brush =
    if (accentColor == null) {
        CachedLiquidGlassBorder
    } else {
        Brush.verticalGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.72f),
                accentColor.copy(alpha = 0.24f),
                accentColor.copy(alpha = 0.45f)
            )
        )
    }

fun liquidGlassFillBrush(): Brush = CachedLiquidGlassFill

/**
 * High-Contrast Custom Toggle Switch:
 * Clearly visible in BOTH OFF and ON states across all themes & AMOLED black mode!
 */
@Composable
fun NaphSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val trackColor by animateColorAsState(
        targetValue = if (checked) primary else Color(0xFF2D3548),
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "switch_track"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) primary else Color(0xFF64748B),
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "switch_border"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 3.dp,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "switch_thumb"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .width(48.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(1.2.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) Color.White else Color(0xFFE2E8F0))
        )
    }
}

/**
 * Hardware-friendly downsampled network image loader for locked 120Hz scrolling.
 */
@Composable
fun FastNetworkImage(
    url: String?,
    contentDescription: String?,
    widthPx: Int = 240,
    heightPx: Int = 360,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier
) {
    if (url.isNullOrBlank()) return
    val context = LocalPlatformContext.current
    val request = remember(url, widthPx, heightPx) {
        ImageRequest.Builder(context)
            .data(url)
            .size(widthPx, heightPx)
            .precision(Precision.INEXACT)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}

/**
 * Liquid Glass Card Container (Pre-cached brushes for zero-jank 120Hz scrolling)
 */
@Composable
fun LiquidGlassCard(
    cornerRadius: Dp = 18.dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = remember(cornerRadius) {
        when (cornerRadius) {
            16.dp -> Shape16
            18.dp -> Shape18
            else -> RoundedCornerShape(cornerRadius)
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CachedLiquidGlassFill)
            .border(1.dp, CachedLiquidGlassBorder, shape),
        content = content
    )
}

/**
 * Apple iOS Liquid Glass Floating Capsule Navigation Bar with Native Auto-Shrink on Scroll:
 * - Scrolling UP / at top: full floating glass capsule (62.dp height, icons + labels)
 * - Scrolling DOWN: smoothly shrinks into a compact minimal pill (44.dp height, 212.dp width, icons-only)
 */
@Composable
fun <T> LiquidGlassFloatingNavBar(
    items: List<T>,
    selectedItem: T,
    isMinimized: Boolean,
    labelOf: (T) -> String,
    iconOf: (T) -> ImageVector,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val barHeight by animateDpAsState(
        targetValue = if (isMinimized) 44.dp else 60.dp,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "nav_bar_height"
    )
    val maxBarWidth by animateDpAsState(
        targetValue = if (isMinimized) 216.dp else 460.dp,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "nav_bar_width"
    )
    val verticalPad by animateDpAsState(
        targetValue = if (isMinimized) 6.dp else 10.dp,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "nav_bar_pad"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = verticalPad)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .widthIn(max = maxBarWidth)
                .fillMaxWidth()
                .height(barHeight)
                .graphicsLayer { clip = true; shape = Shape28 }
                .background(CachedFloatingBarFill)
                .border(1.dp, CachedLiquidGlassBorder, Shape28)
                .padding(horizontal = 5.dp, vertical = 4.dp)
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(if (isMinimized) 36.dp else 50.dp)
                        .clip(Shape22)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(CachedActiveTabFill)
                                    .border(0.8.dp, Color.White.copy(alpha = 0.25f), Shape22)
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onSelect(item) }
                ) {
                    Icon(
                        imageVector = iconOf(item),
                        contentDescription = labelOf(item),
                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.62f),
                        modifier = Modifier.size(if (isMinimized) 18.dp else 20.dp)
                    )
                    if (!isMinimized) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = labelOf(item),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.62f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom Modern NAPH Cinema Prism App Icon (Vector Canvas)
 */
@Composable
fun NaphBrandIcon(
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val corner = CornerRadius(w * 0.28f, h * 0.28f)

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0F1322), Color(0xFF070910)),
                start = Offset.Zero,
                end = Offset(w, h)
            ),
            cornerRadius = corner
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x5538BDF8), Color(0x226366F1), Color.Transparent),
                center = Offset(w * 0.52f, h * 0.48f),
                radius = w * 0.55f
            )
        )

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFF6366F1), Color(0xFFA855F7)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            cornerRadius = corner,
            style = Stroke(width = w * 0.055f)
        )

        val prismPath = Path().apply {
            moveTo(w * 0.30f, h * 0.74f)
            lineTo(w * 0.30f, h * 0.26f)
            lineTo(w * 0.74f, h * 0.50f)
            close()
        }
        drawPath(
            path = prismPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFF6366F1), Color(0xFFE879F9)),
                start = Offset(w * 0.25f, h * 0.22f),
                end = Offset(w * 0.76f, h * 0.76f)
            )
        )

        val accentPath = Path().apply {
            moveTo(w * 0.28f, h * 0.74f)
            lineTo(w * 0.28f, h * 0.26f)
            lineTo(w * 0.56f, h * 0.62f)
            lineTo(w * 0.56f, h * 0.26f)
        }
        drawPath(
            path = accentPath,
            color = Color.White,
            style = Stroke(
                width = w * 0.075f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * High-Contrast Modern Primary Action Pill
 */
@Composable
fun ModernPrimaryButton(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    height: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(height)
            .clip(Shape12)
            .background(containerColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = contentColor,
            maxLines = 1
        )
    }
}

/**
 * High-Contrast Liquid Glass Pill Button
 */
@Composable
fun ModernGlassButton(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    iconTint: Color = Color.White,
    textColor: Color = Color.White,
    isActive: Boolean = false,
    height: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val borderBrush = remember(isActive, primary) {
        if (isActive) liquidGlassBorderBrush(primary) else CachedLiquidGlassBorder
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(height)
            .clip(Shape12)
            .then(
                if (isActive) Modifier.background(primary.copy(alpha = 0.28f))
                else Modifier.background(CachedGlassButtonFill)
            )
            .border(1.dp, borderBrush, Shape12)
            .clickable { onClick() }
            .padding(horizontal = 14.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) primary else iconTint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = textColor,
            maxLines = 1
        )
    }
}

/**
 * Liquid Glass Circular / Squircle Icon Pill
 */
@Composable
fun ModernIconPill(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color = Color.White,
    isActive: Boolean = false,
    size: Dp = 36.dp,
    isCircle: Boolean = true,
    modifier: Modifier = Modifier
) {
    val shape = if (isCircle) CircleShape else Shape10
    val primary = MaterialTheme.colorScheme.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(
                if (isActive) Modifier.background(primary)
                else Modifier.background(CachedIconPillFill)
            )
            .border(
                width = 1.dp,
                color = if (isActive) primary else Color.White.copy(alpha = 0.22f),
                shape = shape
            )
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

/**
 * High-Contrast Liquid Glass Filter Pill
 */
@Composable
fun ModernFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val bg = if (selected) primary else Color(0xD91A2030)
    val borderColor = if (selected) primary else Color.White.copy(alpha = 0.20f)
    val textColor = if (selected) Color.White else Color(0xFFE2E8F0)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(32.dp)
            .clip(Shape10)
            .background(bg)
            .border(1.dp, borderColor, Shape10)
            .clickable { onClick() }
            .padding(horizontal = 13.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = textColor,
            maxLines = 1
        )
    }
}
