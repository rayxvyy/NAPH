package com.naph.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.naph.app.data.storage.PlatformLocalStorage
import com.naph.app.data.torrent.PlatformP2PStreamer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlatformLocalStorage.init(applicationContext)
        PlatformP2PStreamer.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            NaphApp()
        }
    }
}
