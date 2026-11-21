package com.naph.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
 * - Decodes posters at compact thumbnail pixel dimensions (e.g. 240x360 instead of 2000x3000)
 * - Caches decoded bitmaps in memory & disk so scrolling LazyColumn / LazyRow is 60-120 FPS instant.
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
 * Apple Liquid Glass Specular Rim Brush (top-edge glass refraction sheen)
 */
fun liquidGlassBorderBrush(accentColor: Color? = null): Brush = Brush.verticalGradient(
    colors = if (accentColor != null) {
        listOf(
            accentColor.copy(alpha = 0.65f),
            accentColor.copy(alpha = 0.22f),
            accentColor.copy(alpha = 0.40f)
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.26f),
            Color.White.copy(alpha = 0.06f),
            Color.White.copy(alpha = 0.14f)
        )
    }
)

fun liquidGlassFillBrush(): Brush = Brush.verticalGradient(
    colors = listOf(
        Color(0xD6171C2A),
        Color(0xCC0D1018)
    )
)

/**
 * Liquid Glass Card Container (Translucent Frosted Surface + Specular Refraction Rim)
 */
@Composable
fun LiquidGlassCard(
    cornerRadius: Dp = 18.dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(liquidGlassFillBrush())
            .border(1.dp, liquidGlassBorderBrush(), shape),
        content = content
    )
}

/**
 * Apple iOS Liquid Glass Floating Capsule Navigation Bar
 * Floats cleanly above the iPhone home indicator with specular top refraction & luminous active pill.
 */
@Composable
fun <T> LiquidGlassFloatingNavBar(
    items: List<T>,
    selectedItem: T,
    labelOf: (T) -> String,
    iconOf: (T) -> ImageVector,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val capsuleShape = RoundedCornerShape(28.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(capsuleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEB1A2030),
                            Color(0xE00C0F18)
                        )
                    )
                )
                .border(1.dp, liquidGlassBorderBrush(), capsuleShape)
                .padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                val pillShape = RoundedCornerShape(22.dp)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(pillShape)
                        .background(
                            if (isSelected) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.22f),
                                        Color.White.copy(alpha = 0.09f)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .border(
                            width = if (isSelected) 0.8.dp else 0.dp,
                            color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                            shape = pillShape
                        )
                        .clickable { onSelect(item) }
                ) {
                    Icon(
                        imageVector = iconOf(item),
                        contentDescription = labelOf(item),
                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.58f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = labelOf(item),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.58f),
                        maxLines = 1
                    )
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
 * Sleek Modern Primary Action Pill
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
    val shape = RoundedCornerShape(12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(height)
            .clip(shape)
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
 * Apple Liquid Glass Pill Button (Translucent Frosted Glass + Specular Refraction Rim)
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
    val shape = RoundedCornerShape(12.dp)
    val bgBrush = if (isActive) {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xD91D2333),
                Color(0xCC111520)
            )
        )
    }
    val borderBrush = liquidGlassBorderBrush(if (isActive) MaterialTheme.colorScheme.primary else null)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, borderBrush, shape)
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
 * Liquid Glass Circular / Squircle Icon Pill for Media Player & Toolbars
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
    val shape = if (isCircle) CircleShape else RoundedCornerShape(10.dp)
    val bgBrush = if (isActive) {
        Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xCC22283A), Color(0xB8111520))
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bgBrush)
            .border(
                width = 1.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.18f),
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
 * Low-profile Liquid Glass Filter Pill
 */
@Composable
fun ModernFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color(0xCC161B28)
    val borderBrush = liquidGlassBorderBrush(if (selected) MaterialTheme.colorScheme.primary else null)
    val textColor = if (selected) Color.White else NaphColors.TextSecondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(32.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, borderBrush, shape)
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
