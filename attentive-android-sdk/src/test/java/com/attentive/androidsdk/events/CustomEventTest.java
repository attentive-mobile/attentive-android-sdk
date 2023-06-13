package com.attentive.androidsdk.events;

import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CustomEventTest {
    private final String type;
    private final Map<String, String> properties;

    public CustomEventTest(String type, Map<String, String> properties) {
        this.type = type;
        this.properties = properties;
    }

    @Test
    public void customEventBuilder() {
        System.out.printf("'%s', '%s'%n", type, properties);
        new CustomEvent.Builder(type, properties);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> provideCustomEventBuildParams() {
        return Arrays.asList(new Object[][] {
            { null, Map.of() },
            { "", Map.of() },
            { "f", null },
            { "typeWithInvalidChar[", Map.of() },
            { "f", Map.of("keyWithInvalidChar[", "value") },
            { "f", Map.of() },
            { "f", Map.of("key", "value") }
        });
    }
}
