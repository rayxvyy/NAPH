package com.naph.app

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1380.dp, 860.dp),
        position = WindowPosition.Aligned(androidx.compose.ui.Alignment.Center)
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "NAPH — Cross-Platform Media Hub & Sequential P2P Streaming Engine"
    ) {
        window.minimumSize = Dimension(960, 640)
        NaphApp()
    }
}
