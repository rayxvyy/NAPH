package com.naph.app.data.storage

/**
 * Simple local on-device key-value persistence (no cloud needed):
 * Saves local profiles and installed addon manifest URLs directly on the user's device.
 */
expect object PlatformLocalStorage {
    fun saveString(key: String, value: String)
    fun loadString(key: String): String?
}
