package com.attentive.androidsdk.internal.network

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Test

class DatadogTracePriorityInterceptorTest {
    private val interceptor = DatadogTracePriorityInterceptor()

    @Test
    fun addsHeaderWhenAbsent() {
        val chain = CapturingChain(Request.Builder().url("https://example.test/").build())

        interceptor.intercept(chain)

        assertEquals("1", chain.proceededRequest!!.header("x-datadog-sampling-priority"))
    }

    @Test
    fun overridesHeaderWhenAlreadySet() {
        val chain =
            CapturingChain(
                Request.Builder()
                    .url("https://example.test/")
                    .header("x-datadog-sampling-priority", "0")
                    .build(),
            )

        interceptor.intercept(chain)

        assertEquals("1", chain.proceededRequest!!.header("x-datadog-sampling-priority"))
    }

    private class CapturingChain(private val req: Request) : TestChain() {
        var proceededRequest: Request? = null

        override fun request(): Request = req

        override fun proceed(request: Request): Response {
            proceededRequest = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }
    }
}
