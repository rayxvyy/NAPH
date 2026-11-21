package com.naph.app.data.storage

actual object PlatformLocalStorage {
    private val mem = mutableMapOf<String, String>()

    actual fun saveString(key: String, value: String) {
        mem[key] = value
    }

    actual fun loadString(key: String): String? = mem[key]
}
