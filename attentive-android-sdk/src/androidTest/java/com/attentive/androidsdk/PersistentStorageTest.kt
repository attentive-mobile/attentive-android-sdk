package com.attentive.androidsdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistentStorageTest {
    private var persistentStorage: PersistentStorage? = null

    @Before
    fun setup() {
        persistentStorage =
            PersistentStorage(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @Test
    fun saveAndRead_addsOneKeyValuePair_keyValuePairIsReturned() {
        // Arrange
        val key = "someKey"
        val value = "someValue"

        // Act
        persistentStorage!!.save(key, value)

        // Assert
        Assert.assertEquals(value, persistentStorage!!.read(key))
    }

    @Test
    fun saveAndRead_overwriteExistingValue_readReturnsNewValue() {
        // Arrange
        val key = "someKey"
        val firstValue = "someValue"
        val secondValue = "someOtherValue"

        // Act
        persistentStorage!!.save(key, firstValue)
        persistentStorage!!.save(key, secondValue)

        // Assert
        Assert.assertEquals(secondValue, persistentStorage!!.read(key))
    }

    @Test
    fun read_readKeyThatDoesNotExist_returnsNull() {
        // Arrange
        val key = "someKey"

        // Act
        persistentStorage!!.delete(key)

        // Assert
        Assert.assertNull(persistentStorage!!.read(key))
    }

    @Test
    fun delete_deleteKeyThatDoesNotExist_noop() {
        // Arrange
        val key = "someKey"

        // Act
        persistentStorage!!.delete(key)

        // Assert
        Assert.assertNull(persistentStorage!!.read(key))
    }

    @Test
    fun delete_deleteExistingKey_readReturnsNull() {
        // Arrange
        val key = "someKey"
        val value = "someValue"
        persistentStorage!!.save(key, value)

        // Act
        persistentStorage!!.delete(key)

        // Assert
        Assert.assertEquals(null, persistentStorage!!.read(key))
    }
}
