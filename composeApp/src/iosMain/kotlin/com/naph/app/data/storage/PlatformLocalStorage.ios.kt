package com.naph.app.data.storage

import platform.Foundation.NSUserDefaults

actual object PlatformLocalStorage {
    actual fun saveString(key: String, value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = "naph_local_$key")
    }

    actual fun loadString(key: String): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey("naph_local_$key")
    }
}
