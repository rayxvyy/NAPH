package com.naph.app.data.storage

import android.content.Context
import android.content.SharedPreferences

actual object PlatformLocalStorage {
    private var prefs: SharedPreferences? = null
    private val mem = mutableMapOf<String, String>()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("naph_local_storage", Context.MODE_PRIVATE)
    }

    actual fun saveString(key: String, value: String) {
        mem[key] = value
        prefs?.edit()?.putString(key, value)?.apply()
    }

    actual fun loadString(key: String): String? {
        return prefs?.getString(key, null) ?: mem[key]
    }
}
