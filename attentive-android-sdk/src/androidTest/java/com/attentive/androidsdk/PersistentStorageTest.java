package com.attentive.androidsdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PersistentStorageTest {
    private PersistentStorage persistentStorage;

    @Before
    public void setup() {
        persistentStorage = new PersistentStorage(InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test
    public void saveAndRead_addsOneKeyValuePair_keyValuePairIsReturned() {
        // Arrange
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
        final String key = "someKey";
        final String firstValue = "someValue";
        final String secondValue = "someOtherValue";

        // Act
        persistentStorage.save(key, firstValue);
        persistentStorage.save(key, secondValue);

        // Assert
        assertEquals(secondValue, persistentStorage.read(key));
    }

    @Test
    public void read_readKeyThatDoesNotExist_returnsNull() {
        // Arrange
        final String key = "someKey";

        // Act
        persistentStorage.delete(key);

        // Assert
        assertNull(persistentStorage.read(key));
    }

    @Test
    public void delete_deleteKeyThatDoesNotExist_noop() {
        // Arrange
        final String key = "someKey";

        // Act
        persistentStorage.delete(key);

        // Assert
        assertNull(persistentStorage.read(key));
    }

    @Test
    public void delete_deleteExistingKey_readReturnsNull() {
        // Arrange
        final String key = "someKey";
        final String value = "someValue";
        persistentStorage.save(key, value);

        // Act
        persistentStorage.delete(key);

        // Assert
        assertEquals(null, persistentStorage.read(key));
    }
}
