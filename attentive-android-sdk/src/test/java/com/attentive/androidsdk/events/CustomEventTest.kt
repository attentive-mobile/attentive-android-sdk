package com.attentive.androidsdk.events

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CustomEventTest(
    val type: String,
    val properties: Map<String, String>,
    val throwableClass: Class<Throwable>?,
) {
    @Test
    fun customEventBuilder() {
        System.out.printf("'%s', '%s', '%s'%n", type, properties, throwableClass)
        if (throwableClass == null) {
            // no throw
            CustomEvent.Builder(type, properties)
        } else {
            Assert.assertThrows(
                throwableClass,
            ) {
                CustomEvent.Builder(
                    type,
                    properties,
                ).build()
            }
        }
    }

    companion object {
        @Parameterized.Parameters
        @JvmStatic
        fun provideCustomEventBuildParams(): Collection<Array<Any?>> {
            return listOf(
                arrayOf("", emptyMap<String, String>(), IllegalArgumentException::class.java),
                arrayOf("typeWithInvalidChar[", emptyMap<String, String>(), IllegalArgumentException::class.java),
                arrayOf("f", mapOf("keyWithInvalidChar[" to "value"), IllegalArgumentException::class.java),
                arrayOf("f", emptyMap<String, String>(), null),
                arrayOf("f", mapOf("key" to "value"), null),
            )
        }
    }
}
