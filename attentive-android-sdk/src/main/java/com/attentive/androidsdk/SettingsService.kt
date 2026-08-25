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

        /** Retired setting. Kept in [SETTINGS] so stale stored values still get cleared. */
        private const val LEGACY_SKIP_FATIGUE = "skipFatigue"
        private val SETTINGS = arrayOf(LEGACY_SKIP_FATIGUE, LOG_LEVEL)
    }
}
