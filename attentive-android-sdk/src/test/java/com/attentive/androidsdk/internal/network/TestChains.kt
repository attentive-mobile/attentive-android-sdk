package com.attentive.androidsdk.internal.network

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Timeout
import java.util.concurrent.TimeUnit

/**
 * Skeleton [Interceptor.Chain] for unit tests. Subclasses override [proceed] (and optionally
 * [request]/[call]) and inherit no-op stubs for the timeout and connection accessors that
 * OkHttp interceptor tests don't exercise.
 */
internal abstract class TestChain : Interceptor.Chain {
    val testCall: TestCall = TestCall(Request.Builder().url("https://example.test/").build())

    override fun request(): Request = testCall.request()

    override fun connection() = null

    override fun call(): Call = testCall

    override fun connectTimeoutMillis() = 0

    override fun readTimeoutMillis() = 0

    override fun writeTimeoutMillis() = 0

    override fun withConnectTimeout(
        timeout: Int,
        unit: TimeUnit
    ) = throw UnsupportedOperationException()

    override fun withReadTimeout(
        timeout: Int,
        unit: TimeUnit
    ) = throw UnsupportedOperationException()

    override fun withWriteTimeout(
        timeout: Int,
        unit: TimeUnit
    ) = throw UnsupportedOperationException()
}

/** Minimal [Call] stub for tests; supports cancellation, no-op for everything else. */
internal class TestCall(private val req: Request) : Call {
    var canceled: Boolean = false

    override fun cancel() {
        canceled = true
    }

    override fun isCanceled(): Boolean = canceled

    override fun clone(): Call = this

    override fun execute(): Response = throw UnsupportedOperationException()

    override fun enqueue(responseCallback: Callback) = throw UnsupportedOperationException()

    override fun isExecuted(): Boolean = false

    override fun request(): Request = req

    override fun timeout(): Timeout = Timeout.NONE
}
