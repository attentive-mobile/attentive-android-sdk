package com.attentive.androidsdk.events

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Arrays

@RunWith(Parameterized::class)
class CustomEventTest(
    private val type: String,
    private val properties: Map<String, String>,
    private val throwableClass: Class<Throwable>?
) {
    @Test
    fun customEventBuilder() {
        System.out.printf("'%s', '%s', '%s'%n", type, properties, throwableClass)
        if (throwableClass == null) {
            // no throw
            CustomEvent.Builder(type, properties)
        } else {
            Assert.assertThrows(
                throwableClass
            ) {
                CustomEvent.Builder(
                    type,
                    properties
                )
            }
        }
    }

    companion object {
        @Parameterized.Parameters
        fun provideCustomEventBuildParams(): Collection<Array<Any>> {
            return Arrays.asList<Array<Any>>(
                arrayOf(
                    arrayOf(
                        null, java.util.Map.of<Any, Any>(),
                        IllegalArgumentException::class.java
                    ),
                    arrayOf(
                        "",
                        java.util.Map.of<Any, Any>(),
                        IllegalArgumentException::class.java
                    ),
                    arrayOf("f", null, IllegalArgumentException::class.java),
                    arrayOf(
                        "typeWithInvalidChar[", java.util.Map.of<Any, Any>(),
                        IllegalArgumentException::class.java
                    ),
                    arrayOf(
                        "f", java.util.Map.of("keyWithInvalidChar[", "value"),
                        IllegalArgumentException::class.java
                    ),
                    arrayOf("f", java.util.Map.of<Any, Any>(), null),
                    arrayOf("f", java.util.Map.of("key", "value"), null)
                )
            )
        }
    }
}