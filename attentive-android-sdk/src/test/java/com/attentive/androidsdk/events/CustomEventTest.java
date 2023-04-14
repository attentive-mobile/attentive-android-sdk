package com.attentive.androidsdk.events;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CustomEventTest {
    private final String type;
    private final Map<String, String> properties;
    private final Class<Throwable> throwableClass;

    public CustomEventTest(String type, Map<String, String> properties, Class<Throwable> throwableClass) {
        this.type = type;
        this.properties = properties;
        this.throwableClass = throwableClass;
    }

    @Test
    public void customEventBuilder() {
        System.out.printf("'%s', '%s', '%s'%n", type, properties, throwableClass);
        if (throwableClass == null) {
            // no throw
            new CustomEvent.Builder(type, properties);
        } else {
            assertThrows(throwableClass, () -> new CustomEvent.Builder(type, properties));
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> provideCustomEventBuildParams() {
        return Arrays.asList(new Object[][] {
            { null, Map.of(), IllegalArgumentException.class },
            { "", Map.of(), IllegalArgumentException.class },
            { "f", null, IllegalArgumentException.class },
            { "typeWithInvalidChar[", Map.of(), IllegalArgumentException.class },
            { "f", Map.of("keyWithInvalidChar[", "some"), IllegalArgumentException.class },
            { "f", Map.of(), null },
        });
    }
}