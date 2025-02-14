package com.attentive.androidsdk.internal.network

import android.content.Context
import com.attentive.androidsdk.internal.util.AppInfo
import com.attentive.androidsdk.internal.util.AppInfo.getApplicationName
import com.attentive.androidsdk.internal.util.AppInfo.getApplicationPackageName
import com.attentive.androidsdk.internal.util.AppInfo.getApplicationVersion
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

class UserAgentInterceptorTest {
    private var userAgentInterceptor: UserAgentInterceptor? = null

    @Before
    fun setup() {
        userAgentInterceptor = spy(
            UserAgentInterceptor(
                mock<Context>()
            )
        )
    }

    @Test
    @Throws(IOException::class)
    fun intercept_interceptCalled_customUserAgentIsAdded() {
        // Arrange
        val chain: Interceptor.Chain = mock()
        doReturn(buildRequest()).whenever(chain).request()
        val testAgent = "someUserAgentGoesHere"
        doReturn(testAgent).whenever(userAgentInterceptor)?.userAgent

        // Act
        userAgentInterceptor!!.intercept(chain)

        // Assert
        val requestArgumentCaptor = org.mockito.kotlin.argumentCaptor<Request>()
        verify(chain).proceed(requestArgumentCaptor.capture())
        val requestWithUserAgent = requestArgumentCaptor.firstValue
        Assert.assertEquals(testAgent, requestWithUserAgent.header("User-Agent"))
    }

    @Test
    fun userAgent_returnsCorrectlyFormattedUserAgent() {
        mockStatic(AppInfo::class.java).use { appInfoMockedStatic ->
            whenever(getApplicationName(any())).thenReturn(APP_NAME)
            whenever(getApplicationVersion(any())).thenReturn(APP_VERSION)
            whenever(getApplicationPackageName(any())).thenReturn(APP_PACKAGE_NAME)
            whenever(AppInfo.androidLevel).thenReturn(ANDROID_LEVEL)
            whenever(AppInfo.androidVersion).thenReturn(ANDROID_VERSION)
            whenever(AppInfo.attentiveSDKVersion).thenReturn(ATTENTIVE_SDK_VERSION)
            whenever(AppInfo.attentiveSDKName).thenReturn(ATTENTIVE_SDK_NAME)

            val userAgent = userAgentInterceptor!!.userAgent
            Assert.assertEquals(
                "appName-Value/" + APP_VERSION + " (" + APP_PACKAGE_NAME + "; Android " + ANDROID_VERSION + "; Android API Level " + ANDROID_LEVEL + ") " + ATTENTIVE_SDK_NAME + "/" + ATTENTIVE_SDK_VERSION,
                userAgent
            )
        }
    }

    private fun buildRequest(): Request {
        return Request.Builder().url("https://attentive.com").get().build()
    }

    companion object {
        private const val APP_NAME = "appName Value"
        private const val APP_VERSION = "appVersionValue"
        private const val APP_PACKAGE_NAME = "com.what.exampleapp"
        private const val ANDROID_VERSION = "androidVersion"
        private const val ANDROID_LEVEL = "androidLevel"
        private const val ATTENTIVE_SDK_VERSION = "attentiveSdkVersionValue"
        private const val ATTENTIVE_SDK_NAME = "attentiveSdkNameValue"
    }
}