package com.naph.app

import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.GradientPaint
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1380.dp, 860.dp),
        position = WindowPosition.Aligned(Alignment.Center)
    )
    val appIcon = remember { BitmapPainter(createNaphAppIconBitmap().toComposeImageBitmap()) }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        icon = appIcon,
        title = "NAPH"
    ) {
        window.minimumSize = Dimension(960, 640)
        NaphApp()
    }
}

/**
 * Renders the 256x256 native OS Window / Taskbar Icon for NAPH:
 * Deep obsidian squircle with a glowing electric cyan-to-violet gradient border,
 * cinema prism play triangle, and crisp white N-Wing emblem.
 */
private fun createNaphAppIconBitmap(): BufferedImage {
    val size = 256
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

    val pad = 12f
    val boxSize = size - pad * 2f
    val arc = 68f
    val squircle = RoundRectangle2D.Float(pad, pad, boxSize, boxSize, arc, arc)

    // 1. Deep Obsidian Base
    g.paint = GradientPaint(0f, 0f, Color(0x0F, 0x13, 0x22), size.toFloat(), size.toFloat(), Color(0x06, 0x08, 0x0E))
    g.fill(squircle)

    // 2. Ambient Radial Cyan-Indigo Glow
    g.paint = RadialGradientPaint(
        Point2D.Float(size * 0.52f, size * 0.48f),
        size * 0.48f,
        floatArrayOf(0f, 0.55f, 1f),
        arrayOf(Color(56, 189, 248, 85), Color(99, 102, 241, 35), Color(0, 0, 0, 0))
    )
    g.fill(squircle)

    // 3. Glowing Gradient Outer Rim
    g.paint = GradientPaint(pad, pad, Color(0x38, 0xBD, 0xF8), size - pad, size - pad, Color(0xA8, 0x55, 0xF7))
    g.stroke = BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    g.draw(squircle)

    // 4. Stylized Cyan-to-Violet Cinema Play Prism
    val prism = Path2D.Float().apply {
        moveTo(size * 0.31, size * 0.73)
        lineTo(size * 0.31, size * 0.27)
        lineTo(size * 0.74, size * 0.50)
        closePath()
    }
    g.paint = GradientPaint(
        size * 0.28f,
        size * 0.25f,
        Color(0x38, 0xBD, 0xF8),
        size * 0.75f,
        size * 0.75f,
        Color(0x8B, 0x5C, 0xF6)
    )
    g.fill(prism)

    // 5. Crisp White N-Wing Emblem Stroke
    val nWing = Path2D.Float().apply {
        moveTo(size * 0.29, size * 0.73)
        lineTo(size * 0.29, size * 0.27)
        lineTo(size * 0.56, size * 0.62)
        lineTo(size * 0.56, size * 0.27)
    }
    g.color = Color.WHITE
    g.stroke = BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    g.draw(nWing)

    g.dispose()
    return img
}
