package com.naph.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * Hardware-friendly downsampled network image loader:
 * - Decodes posters at compact thumbnail pixel dimensions (e.g. 260x390 instead of 2000x3000)
 * - Caches decoded bitmaps in memory & disk so scrolling LazyColumn / LazyRow is 60-120 FPS instant.
 */
@Composable
fun FastNetworkImage(
    url: String?,
    contentDescription: String?,
    widthPx: Int = 260,
    heightPx: Int = 390,
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
 * Custom Modern NAPH Cinema Prism App Icon (Vector Canvas)
 * Replaces the old plain "N" square with a sleek dark obsidian squircle + glowing cyan-to-violet cinema play prism.
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

        // 1. Deep Obsidian Base Squircle
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0F1322), Color(0xFF070910)),
                start = Offset.Zero,
                end = Offset(w, h)
            ),
            cornerRadius = corner
        )

        // 2. Ambient Cyan/Indigo Radial Glow inside Squircle
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x5538BDF8), Color(0x226366F1), Color.Transparent),
                center = Offset(w * 0.52f, h * 0.48f),
                radius = w * 0.55f
            )
        )

        // 3. Glowing Gradient Outer Rim
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFF6366F1), Color(0xFFA855F7)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            cornerRadius = corner,
            style = Stroke(width = w * 0.055f)
        )

        // 4. Stylized N + Play Prism Emblem
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

        // 5. Crisp White N-Wing Accent Stroke
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
 * Sleek Modern Primary Action Pill (Apple TV / Linear style — compact, crisp, zero chunkiness)
 */
@Composable
fun ModernPrimaryButton(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    height: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(horizontal = 15.dp)
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
 * Sleek Translucent Dark Glass Pill Button (Replaces old chunky OutlinedButton)
 */
@Composable
fun ModernGlassButton(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    iconTint: Color = Color.White,
    textColor: Color = Color.White,
    isActive: Boolean = false,
    height: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    } else {
        Color(0xFF141722)
    }
    val borderColor = if (isActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    } else {
        Color.White.copy(alpha = 0.10f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else iconTint,
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
 * Compact Circular / Squircle Icon Button for Media Player & Toolbars
 */
@Composable
fun ModernIconPill(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color = Color.White,
    isActive: Boolean = false,
    size: Dp = 34.dp,
    isCircle: Boolean = true,
    modifier: Modifier = Modifier
) {
    val shape = if (isCircle) CircleShape else RoundedCornerShape(9.dp)
    val bg = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bg)
            .border(
                width = 1.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                shape = shape
            )
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.50f)
        )
    }
}

/**
 * Low-profile Modern Filter Pill (Replaces bulky Material3 FilterChip)
 */
@Composable
fun ModernFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF131620)
    val border = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.09f)
    val textColor = if (selected) Color.White else NaphColors.TextSecondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = textColor
        )
    }
}
