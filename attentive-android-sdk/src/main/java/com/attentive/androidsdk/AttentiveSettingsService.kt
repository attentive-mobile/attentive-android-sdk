package com.attentive.androidsdk

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.VisibleForTesting
import com.attentive.androidsdk.internal.util.AppInfo
import timber.log.Timber
import timber.log.Timber.DebugTree

class AttentiveSettingsService : Service() {
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (!AppInfo.isDebuggable(this)) {
            Timber.e("onCreate: not in debug mode")
            return START_STICKY
        }
        val extras = intent.extras
        if (extras == null) {
            Timber.d("onCreate: extras is null")
            return START_STICKY
        }

        val settingsService = ClassFactory.buildSettingsService(
            ClassFactory.buildPersistentStorage(
                applicationContext
            )
        )
        handleSkipFatigueExtra(extras, settingsService)
        handleResetSettingsExtra(extras, settingsService)
        handleSetLogLevelExtra(extras, settingsService)
        stopSelf()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        Timber.plant(DebugTree())
        return null
    }

    companion object {
        const val EXTRA_SET_SKIP_FATIGUE: String = "setSkipFatigue"
        const val EXTRA_RESET_SETTINGS: String = "resetSettings"
        const val EXTRA_SET_LOG_LEVEL: String = "setLogLevel"

        @JvmStatic
        @VisibleForTesting
        fun handleSkipFatigueExtra(extras: Bundle, settingsService: SettingsService) {
            if (extras.containsKey(EXTRA_SET_SKIP_FATIGUE)) {
                Timber.d("Setting skip fatigue...")
                val skipFatigue = extras.getBoolean(EXTRA_SET_SKIP_FATIGUE, false)
                settingsService.isSkipFatigueEnabled = skipFatigue
                Timber.i("skipFatigue set to: %s", skipFatigue)
            }
        }

        @JvmStatic
        @VisibleForTesting
        fun handleResetSettingsExtra(extras: Bundle, settingsService: SettingsService) {
            if (extras.containsKey(EXTRA_RESET_SETTINGS) && extras.getBoolean(
                    EXTRA_RESET_SETTINGS,
                    false
                )
            ) {
                Timber.d("Resetting settings...")
                settingsService.resetSettings()
                Timber.i("Settings reset")
            }
        }

        @VisibleForTesting
        fun handleSetLogLevelExtra(extras: Bundle, settingsService: SettingsService) {
            if (extras.containsKey(EXTRA_SET_LOG_LEVEL)) {
                Timber.d("Setting log level...")
                val logLevel = extras.getInt(EXTRA_SET_LOG_LEVEL, -1)
                val attentiveLogLevel =
                    AttentiveLogLevel.fromId(logLevel)
                if (attentiveLogLevel == null) {
                    Timber.w(
                        "Log level should be one of: %s",
                        *AttentiveLogLevel.entries.toTypedArray() as Array<Any?>
                    )
                    Timber.i("Log level not set")
                    return
                }
                settingsService.logLevel = attentiveLogLevel
                Timber.i("log level set to: %s", attentiveLogLevel)
            }
        }
    }
}
