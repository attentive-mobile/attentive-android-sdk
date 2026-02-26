package com.attentive.androidsdk

import android.content.Context
import android.content.SharedPreferences
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

class PersistentStorageTest {
    lateinit var context: Context
    lateinit var sharedPreferences: SharedPreferences
    lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    lateinit var persistentStorage: PersistentStorage

    @Before
    fun setup() {
        sharedPreferences = Mockito.mock(SharedPreferences::class.java)
        sharedPreferencesEditor = Mockito.mock(SharedPreferences.Editor::class.java)
        Mockito.doReturn(sharedPreferencesEditor).`when`(sharedPreferences)?.edit()

        Mockito.doReturn(sharedPreferencesEditor).`when`(sharedPreferencesEditor)
            ?.putString(ArgumentMatchers.any(), ArgumentMatchers.any())
        Mockito.doReturn(sharedPreferencesEditor).`when`(sharedPreferencesEditor)?.clear()
        Mockito.doReturn(sharedPreferencesEditor).`when`(sharedPreferencesEditor)
            ?.remove(ArgumentMatchers.any())

        context = Mockito.mock(Context::class.java)
        Mockito.doReturn(sharedPreferences).`when`(context)?.getSharedPreferences(
            ArgumentMatchers.eq(PersistentStorage.SHARED_PREFERENCES_NAME),
            ArgumentMatchers.anyInt(),
        )

        persistentStorage = PersistentStorage(context)
    }

    @After
    fun checkThatCommitIsNeverCalled() {
        // never call 'commit' because it's not performant since it blocks
        Mockito.verify(sharedPreferencesEditor, Mockito.never()).commit()
    }

    @Test
    fun save_validValues_callsEditorPutString() {
        // Arrange
        val key = "someKey"
        val value = "someValue"

        // Act
        persistentStorage!!.save(key, value)

        // Assert
        Mockito.verify(sharedPreferencesEditor).putString(key, value)
        Mockito.verify(sharedPreferencesEditor).apply()
    }

    @Test
    fun read_validKey_callsPreferencesGetString() {
        // Arrange
        val key = "someKey"

        // Act
        persistentStorage!!.read(key)

        // Assert
        Mockito.verify(sharedPreferences).getString(key, null)
    }

    @Test
    fun delete_validKey_callsPreferencesEditorRemove() {
        // Arrange
        val key = "someKey"

        // Act
        persistentStorage!!.delete(key)

        // Assert
        Mockito.verify(sharedPreferencesEditor).remove(key)
    }

    @Test
    fun deleteAll_callsPreferencesEditorClear() {
        // Arrange

        // Act

        persistentStorage!!.deleteAll()

        // Assert
        Mockito.verify(sharedPreferencesEditor).clear()
    }
}
