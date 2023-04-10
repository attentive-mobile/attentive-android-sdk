package com.attentive.androidsdk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.SharedPreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PersistentStorageTest {

    private Context context;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;
    private PersistentStorage persistentStorage;

    @Before
    public void setup() {
        sharedPreferences = mock(SharedPreferences.class);
        sharedPreferencesEditor = mock(SharedPreferences.Editor.class);
        doReturn(sharedPreferencesEditor).when(sharedPreferences).edit();

        doReturn(sharedPreferencesEditor).when(sharedPreferencesEditor).putString(any(), any());
        doReturn(sharedPreferencesEditor).when(sharedPreferencesEditor).clear();
        doReturn(sharedPreferencesEditor).when(sharedPreferencesEditor).remove(any());

        context = mock(Context.class);
        doReturn(sharedPreferences).when(context).getSharedPreferences(eq(PersistentStorage.SHARED_PREFERENCES_NAME), anyInt());

        persistentStorage = new PersistentStorage(context);
    }

    @After
    public void checkThatCommitIsNeverCalled() {
        // never call 'commit' because it's not performant since it blocks
        verify(sharedPreferencesEditor, never()).commit();
    }


    @Test
    public void save_validValues_callsEditorPutString() {
        // Arrange
        final String key = "someKey";
        final String value = "someValue";

        // Act
        persistentStorage.save(key, value);

        // Assert
        verify(sharedPreferencesEditor).putString(key, value);
        verify(sharedPreferencesEditor).apply();
    }

    @Test
    public void read_validKey_callsPreferencesGetString() {
        // Arrange
        final String key = "someKey";

        // Act
        persistentStorage.read(key);

        // Assert
        verify(sharedPreferences).getString(key, null);
    }

    @Test
    public void delete_validKey_callsPreferencesEditorRemove() {
        // Arrange
        final String key = "someKey";

        // Act
        persistentStorage.delete(key);

        // Assert
        verify(sharedPreferencesEditor).remove(key);
    }

    @Test
    public void deleteAll_callsPreferencesEditorClear() {
        // Arrange

        // Act
        persistentStorage.deleteAll();

        // Assert
        verify(sharedPreferencesEditor).clear();
    }
}