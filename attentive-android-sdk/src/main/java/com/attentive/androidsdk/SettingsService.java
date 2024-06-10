package com.attentive.androidsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SettingsService {
    private static final String TAG = SettingsService.class.getSimpleName();
    private static final String SKIP_FATIGUE = "skipFatigue";
    private static final String[] SETTINGS = { SKIP_FATIGUE };

    private final PersistentStorage persistentStorage;

    public SettingsService(@NonNull PersistentStorage persistentStorage) {
        this.persistentStorage = persistentStorage;
    }

    @Nullable
    public Boolean isSkipFatigueEnabled() {
        return persistentStorage.readBoolean(SKIP_FATIGUE);
    }

    public void setSkipFatigueEnabled(@NonNull Boolean enabled) {
        persistentStorage.save(SKIP_FATIGUE, enabled);
    }

    public void resetSettings() {
        for (String key : SETTINGS) {
            persistentStorage.delete(key);
        }
    }
}
