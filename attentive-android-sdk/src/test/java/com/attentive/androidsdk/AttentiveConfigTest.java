package com.attentive.androidsdk;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AttentiveConfigTest {
    @Test
    public void constructor_validParams_gettersReturnConstructorParams() {
        // Arrange
        final String domain = "domain";
        final AttentiveConfig.Mode mode = AttentiveConfig.Mode.DEBUG;

        // Act
        AttentiveConfig config = new AttentiveConfig(domain, mode, null);

        // Assert
        assertEquals(domain, config.getDomain());
        assertEquals(mode, config.getMode());
    }
}