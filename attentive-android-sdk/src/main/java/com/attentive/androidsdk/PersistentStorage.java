package com.attentive.androidsdk;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import org.jetbrains.annotations.NotNull;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PersistentStorage {
    static final String SHARED_PREFERENCES_NAME = "com.attentive.androidsdk.PERSISTENT_STORAGE";

    private final SharedPreferences sharedPreferences;

    public PersistentStorage(Context context) {

        this.sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public void save(String key, String value) {
        // call "apply" instead of "commit". "apply" writes the changes to memory synchronously but to disk
        // asynchronously, yielding better performance. "commit" writes the changes to disk synchronously; this can be
        // a long operation, which can block the current thread.
        sharedPreferences.edit().putString(key, value).apply();
    }

    /**
     * Method to save a boolean value in local storage
     * @param key The key of the value
     * @param value The boolean value
     */
    public void save(@NonNull String key, @NotNull Boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    /**
     * Method to save an int value in local storage
     * @param key The key of the value
     * @param value The int value
     */
    public void save(@NonNull String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    /**
     * Method to read a string value from local storage
     * @param key The key of the value
     * @return The string value, null if the key does not exist
     */
    @Nullable
    public String read(String key) {
        return sharedPreferences.getString(key, null);
    }

    /**
     * Method to read a boolean value from local storage
     * @param key The key of the value
     * @return The boolean value, false if the key does not exist
     */
    @NotNull
    public Boolean readBoolean(@NonNull String key) {
        if (sharedPreferences.contains(key)) {
            return sharedPreferences.getBoolean(key, false);
        }
        return false;
    }

    /**
     * Method to read an int value from local storage
     * @param key The key of the value
     * @return The int value, -1 if the key does not exist
     */
    @NotNull
    public Integer readInt(@NonNull String key) {
        if (sharedPreferences.contains(key)) {
            return sharedPreferences.getInt(key, -1);
        }
        return -1;
    }

    public void delete(String key) {
        sharedPreferences.edit().remove(key).apply();
    }

    public void deleteAll() {
        sharedPreferences.edit().clear().apply();
    }
}
