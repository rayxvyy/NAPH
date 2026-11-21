package com.naph.app.data.storage

import java.io.File

actual object PlatformLocalStorage {
    private val storageDir: File by lazy {
        val home = System.getProperty("user.home").orEmpty()
        File(home, ".naph_data").apply { mkdirs() }
    }

    actual fun saveString(key: String, value: String) {
        runCatching {
            File(storageDir, "$key.json").writeText(value)
        }
    }

    actual fun loadString(key: String): String? = runCatching {
        val f = File(storageDir, "$key.json")
        if (f.exists()) f.readText() else null
    }.getOrNull()
}
