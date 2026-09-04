package com.attentive.androidsdk

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
class SettingsService(private val persistentStorage: PersistentStorage) {
    var logLevel: AttentiveLogLevel?
        get() = AttentiveLogLevel.fromId(persistentStorage.readInt(LOG_LEVEL))
        set(value) {
            value?.let { persistentStorage.save(LOG_LEVEL, it.id) }
        }

    fun resetSettings() {
        SETTINGS.forEach(persistentStorage::delete)
    }

    companion object {
        private const val LOG_LEVEL = "logLevel"
        private val SETTINGS = arrayOf(LOG_LEVEL)
    }
}
