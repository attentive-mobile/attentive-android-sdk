package com.attentive.androidsdk;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import android.content.Context;
import org.junit.Test;

public class AttentiveConfigTest {
    @Test
    public void constructor_validParams_gettersReturnConstructorParams() {
        // Arrange
        final String domain = "domain";
        final AttentiveConfig.Mode mode = AttentiveConfig.Mode.DEBUG;

        // Act
        AttentiveConfig config = new AttentiveConfig(domain, mode, mock(Context.class));

        // Assert
        assertEquals(domain, config.getDomain());
        assertEquals(mode, config.getMode());
    }
}