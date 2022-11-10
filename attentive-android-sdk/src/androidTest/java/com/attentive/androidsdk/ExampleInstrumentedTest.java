package com.attentive.androidsdk;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Arrange
        // Act
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Assert
        assertEquals("com.attentive.androidsdk.test", appContext.getPackageName());
    }
}
