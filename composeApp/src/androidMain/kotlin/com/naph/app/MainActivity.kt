package com.naph.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.naph.app.data.storage.PlatformLocalStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlatformLocalStorage.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            NaphApp()
        }
    }
}
