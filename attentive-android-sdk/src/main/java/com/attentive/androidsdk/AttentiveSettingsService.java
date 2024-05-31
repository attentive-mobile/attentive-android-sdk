package com.attentive.androidsdk;

import static com.attentive.androidsdk.internal.util.AppInfo.isDebuggable;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

public class AttentiveSettingsService extends Service {

    public static final String EXTRA_SET_SKIP_FATIGUE = "setSkipFatigue";
    public static final String EXTRA_RESET_SETTINGS = "resetSettings";
    private static final String TAG = AttentiveSettingsService.class.getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isDebuggable(this)) {
            Log.e(TAG, "onCreate: not in debug mode");
            return Service.START_NOT_STICKY;
        }
        final Bundle extras = intent.getExtras();
        if (extras == null) {
            Log.d(TAG, "onCreate: extras is null");
            return Service.START_NOT_STICKY;
        }

        final SettingsService settingsService = ClassFactory.buildSettingsService(ClassFactory.buildPersistentStorage(getApplicationContext()));
        handleSkipFatigueExtra(extras, settingsService);
        handleResetSettingsExtra(extras, settingsService);
        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @VisibleForTesting
    public static void handleSkipFatigueExtra(Bundle extras, SettingsService settingsService) {
        if (extras.containsKey(EXTRA_SET_SKIP_FATIGUE)) {
            Log.d(TAG, "Setting skip fatigue...");
            boolean skipFatigue = extras.getBoolean(EXTRA_SET_SKIP_FATIGUE);
            settingsService.setSkipFatigueEnabled(extras.getBoolean(EXTRA_SET_SKIP_FATIGUE, false));
            Log.i(TAG, "skipFatigue set to: " + skipFatigue);
        }
    }

    @VisibleForTesting
    public static void handleResetSettingsExtra(Bundle extras, SettingsService settingsService) {
        if (extras.containsKey(EXTRA_RESET_SETTINGS) && extras.getBoolean(EXTRA_RESET_SETTINGS, false)) {
            Log.d(TAG, "Resetting settings...");
            settingsService.resetSettings();
            Log.i(TAG, "Settings reset");
        }
    }
}
