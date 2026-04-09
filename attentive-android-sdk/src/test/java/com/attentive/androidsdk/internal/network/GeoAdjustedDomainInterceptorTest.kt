package com.attentive.androidsdk.internal.network

import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

class GeoAdjustedDomainInterceptorTest {

    private val domain = "testdomain"
    private val dtagUrl = String.format("https://cdn.attn.tv/%s/dtag.js", domain)

    @Test
    fun intercept_dtagReturns204_fallsBackToOriginalDomain() {
        // Arrange
        val httpClient = mockHttpClientWithResponse(buildDtagResponse(204, ""))
        val interceptor = GeoAdjustedDomainInterceptor(httpClient, domain)
        val chain = mockChainWithPostRequest("c=$domain&other=value")

        // Act - should not throw
        interceptor.intercept(chain)

        // Assert - body should still contain the original domain
        val requestCaptor = argumentCaptor<Request>()
        verify(chain).proceed(requestCaptor.capture())
        val body = requestCaptor.firstValue.bodyToString()
        Assert.assertTrue("Expected body to contain c=$domain", body.contains("c=$domain"))
    }

    @Test
    fun intercept_dtagReturns200WithValidTag_usesGeoAdjustedDomain() {
        // Arrange
        val tagBody = "window.__attentive_domain='testdomain-pt.attn.tv'"
        val httpClient = mockHttpClientWithResponse(buildDtagResponse(200, tagBody))
        val interceptor = GeoAdjustedDomainInterceptor(httpClient, domain)
        val chain = mockChainWithPostRequest("c=$domain&other=value")

        // Act
        interceptor.intercept(chain)

        // Assert - body should contain the geo-adjusted domain
        val requestCaptor = argumentCaptor<Request>()
        verify(chain).proceed(requestCaptor.capture())
        val body = requestCaptor.firstValue.bodyToString()
        Assert.assertTrue("Expected body to contain c=testdomain-pt", body.contains("c=testdomain-pt"))
    }

    @Test
    fun intercept_dtagRequestThrowsIOException_fallsBackToOriginalDomain() {
        // Arrange
        val call: Call = mock()
        whenever(call.execute()).thenThrow(IOException("network error"))
        val httpClient: OkHttpClient = mock()
        whenever(httpClient.newCall(any())).thenReturn(call)

        val interceptor = GeoAdjustedDomainInterceptor(httpClient, domain)
        val chain = mockChainWithPostRequest("c=$domain&other=value")

        // Act - should not throw
        interceptor.intercept(chain)

        // Assert - body should still contain the original domain
        val requestCaptor = argumentCaptor<Request>()
        verify(chain).proceed(requestCaptor.capture())
        val body = requestCaptor.firstValue.bodyToString()
        Assert.assertTrue("Expected body to contain c=$domain", body.contains("c=$domain"))
    }

    private fun mockHttpClientWithResponse(response: Response): OkHttpClient {
        val call: Call = mock()
        whenever(call.execute()).thenReturn(response)
        val httpClient: OkHttpClient = mock()
        whenever(httpClient.newCall(any())).thenReturn(call)
        return httpClient
    }

    private fun buildDtagResponse(code: Int, body: String): Response {
        return Response.Builder()
            .request(Request.Builder().url(dtagUrl).build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "No Content")
            .body(body.toResponseBody("text/javascript".toMediaTypeOrNull()))
            .build()
    }

    private fun mockChainWithPostRequest(body: String): Interceptor.Chain {
        val request = Request.Builder()
            .url("https://events.attentivemobile.com/e")
            .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull()))
            .build()

        val chainResponse = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody(null))
            .build()

        val chain: Interceptor.Chain = mock()
        doReturn(request).whenever(chain).request()
        doReturn(chainResponse).whenever(chain).proceed(any())
        return chain
    }

    private fun Request.bodyToString(): String {
        val buffer = okio.Buffer()
        body!!.writeTo(buffer)
        return buffer.readUtf8()
    }
}
