package com.attentive.androidsdk

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
class PersistentStorage(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun save(key: String?, value: String?) {
        // call "apply" instead of "commit". "apply" writes the changes to memory synchronously but to disk
        // asynchronously, yielding better performance. "commit" writes the changes to disk synchronously; this can be
        // a long operation, which can block the current thread.
        sharedPreferences.edit().putString(key, value).apply()
    }

    /**
     * Method to save a boolean value in local storage
     * @param key The key of the value
     * @param value The boolean value
     */
    fun save(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    /**
     * Method to save an int value in local storage
     * @param key The key of the value
     * @param value The int value
     */
    fun save(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    /**
     * Method to read a string value from local storage
     * @param key The key of the value
     * @return The string value, null if the key does not exist
     */
    fun read(key: String?): String? {
        return sharedPreferences.getString(key, null)
    }

    /**
     * Method to read a boolean value from local storage
     * @param key The key of the value
     * @return The boolean value, false if the key does not exist
     */
    fun readBoolean(key: String): Boolean {
            return sharedPreferences.getBoolean(key, false)
    }

    /**
     * Method to read an int value from local storage
     * @param key The key of the value
     * @return The int value, -1 if the key does not exist
     */
    fun readInt(key: String): Int {
            return sharedPreferences.getInt(key, -1)
    }

    fun delete(key: String?) {
        sharedPreferences.edit().remove(key).apply()
    }

    fun deleteAll() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        const val SHARED_PREFERENCES_NAME: String = "com.attentive.androidsdk.PERSISTENT_STORAGE"
    }
}
