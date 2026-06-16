package com.attentive.androidsdk

import com.attentive.androidsdk.internal.network.DatadogTracePriorityInterceptor
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassFactoryTest {
    @Test
    fun ensureDatadogTracePriorityAddsInterceptorWhenMissing() {
        val hostClient = OkHttpClient.Builder().build()
        assertTrue(
            "precondition: host client must not have the interceptor",
            hostClient.interceptors.none { it is DatadogTracePriorityInterceptor },
        )

        val wrapped = ClassFactory.ensureDatadogTracePriority(hostClient)

        assertEquals(1, wrapped.interceptors.count { it is DatadogTracePriorityInterceptor })
    }

    @Test
    fun ensureDatadogTracePriorityIsIdempotent() {
        val sdkClient =
            OkHttpClient.Builder()
                .addInterceptor(DatadogTracePriorityInterceptor())
                .build()

        val wrapped = ClassFactory.ensureDatadogTracePriority(sdkClient)

        // Should be returned unchanged (no double-add).
        assertSame(sdkClient, wrapped)
        assertEquals(1, wrapped.interceptors.count { it is DatadogTracePriorityInterceptor })
    }
}
