package com.attentive.androidsdk;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PersistentStorage {
    static final String SHARED_PREFERENCES_NAME = "com.attentive.androidsdk.PERSISTENT_STORAGE";

    private final Context context;
    private final SharedPreferences sharedPreferences;

    public PersistentStorage(Context context) {
        this.context = context;

        this.sharedPreferences = this.context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public void save(String key, String value) {
        // call "apply" instead of "commit". "apply" writes the changes to memory synchronously but to disk
        // asynchronously, yielding better performance. "commit" writes the changes to disk synchronously; this can be
        // a long operation, which can block the current thread.
        sharedPreferences.edit().putString(key, value).apply();
    }

    public void save(@NonNull String key, @Nullable Boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    @Nullable
    public String read(String key) {
        return sharedPreferences.getString(key, null);
    }

    @Nullable
    public Boolean readBoolean(@NonNull String key) {
        if (sharedPreferences.contains(key)) {
            return sharedPreferences.getBoolean(key, false);
        }
        return null;
    }

    public void delete(String key) {
        sharedPreferences.edit().remove(key).apply();
    }

    public void deleteAll() {
        sharedPreferences.edit().clear().apply();
    }
}
