package com.attentive.androidsdk;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PersistentStorageTest {
    @Test
    public void saveAndRead_addsOneKeyValuePair_keyValuePairIsReturned() {
        // Arrange
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PersistentStorage persistentStorage = new PersistentStorage(appContext);
        final String key = "someKey";
        final String value = "someValue";

        // Act
        persistentStorage.save(key, value);

        // Assert
        assertEquals(value, persistentStorage.read(key));
    }

    @Test
    public void saveAndRead_overwriteExistingValue_readReturnsNewValue() {
        // Arrange
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PersistentStorage persistentStorage = new PersistentStorage(appContext);
        final String key = "someKey";
        final String firstValue = "someValue";
        final String secondValue = "someOtherValue";

        // Act
        persistentStorage.save(key, firstValue);
        persistentStorage.save(key, secondValue);

        // Assert
        assertEquals(secondValue, persistentStorage.read(key));
    }
}
