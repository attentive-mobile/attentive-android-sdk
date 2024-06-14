package com.attentive.androidsdk;

import static com.attentive.androidsdk.internal.util.AppInfo.isDebuggable;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.attentive.androidsdk.internal.util.LightTree;
import timber.log.Timber;

public class AttentiveSettingsService extends Service {

    public static final String EXTRA_SET_SKIP_FATIGUE = "setSkipFatigue";
    public static final String EXTRA_RESET_SETTINGS = "resetSettings";
    public static final String EXTRA_SET_LOG_LEVEL = "setLogLevel";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isDebuggable(this)) {
            Timber.e("onCreate: not in debug mode");
            return Service.START_STICKY;
        }
        final Bundle extras = intent.getExtras();
        if (extras == null) {
            Timber.d("onCreate: extras is null");
            return Service.START_STICKY;
        }

        final SettingsService settingsService = ClassFactory.buildSettingsService(ClassFactory.buildPersistentStorage(getApplicationContext()));
        handleSkipFatigueExtra(extras, settingsService);
        handleResetSettingsExtra(extras, settingsService);
        handleSetLogLevelExtra(extras, settingsService);
        stopSelf();
        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (isDebuggable(this)) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new LightTree());
        }
        return null;
    }

    @VisibleForTesting
    public static void handleSkipFatigueExtra(Bundle extras, SettingsService settingsService) {
        if (extras.containsKey(EXTRA_SET_SKIP_FATIGUE)) {
            Timber.d("Setting skip fatigue...");
            boolean skipFatigue = extras.getBoolean(EXTRA_SET_SKIP_FATIGUE);
            settingsService.setSkipFatigueEnabled(extras.getBoolean(EXTRA_SET_SKIP_FATIGUE, false));
            Timber.i("skipFatigue set to: %s", skipFatigue);
        }
    }

    @VisibleForTesting
    public static void handleResetSettingsExtra(Bundle extras, SettingsService settingsService) {
        if (extras.containsKey(EXTRA_RESET_SETTINGS) && extras.getBoolean(EXTRA_RESET_SETTINGS, false)) {
            Timber.d("Resetting settings...");
            settingsService.resetSettings();
            Timber.i("Settings reset");
        }
    }

    @VisibleForTesting
    public static void handleSetLogLevelExtra(Bundle extras, SettingsService settingsService) {
        if (extras.containsKey(EXTRA_SET_LOG_LEVEL)) {
            Timber.d("Setting log level...");
            int logLevel = extras.getInt(EXTRA_SET_LOG_LEVEL, -1);
            AttentiveLogLevel attentiveLogLevel = AttentiveLogLevel.fromId(logLevel);
            if (attentiveLogLevel == null) {
                Timber.w("Log level should be one of: %s", (Object[]) AttentiveLogLevel.values());
                Timber.i("Log level not set");
                return;
            }
            settingsService.setLogLevel(attentiveLogLevel);
            Timber.i("log level set to: %s", attentiveLogLevel);
        }
    }
}
