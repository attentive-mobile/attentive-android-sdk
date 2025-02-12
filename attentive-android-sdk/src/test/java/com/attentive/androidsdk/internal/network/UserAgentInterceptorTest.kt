package com.attentive.androidsdk.internal.network

import android.content.Context
import com.attentive.androidsdk.internal.util.AppInfo
import com.attentive.androidsdk.internal.util.AppInfo.getApplicationName
import com.attentive.androidsdk.internal.util.AppInfo.getApplicationPackageName
import com.attentive.androidsdk.internal.util.AppInfo.getApplicationVersion
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.io.IOException
import java.util.Map

class UserAgentInterceptorTest {
    private var userAgentInterceptor: UserAgentInterceptor? = null

    @Before
    fun setup() {
        userAgentInterceptor = Mockito.spy(
            UserAgentInterceptor(
                Mockito.mock(
                    Context::class.java
                )
            )
        )
    }

    @Test
    @Throws(IOException::class)
    fun intercept_interceptCalled_customUserAgentIsAdded() {
        // Arrange
        val chain: Interceptor.Chain = Mockito.mock<Interceptor.Chain>(Interceptor.Chain::class.java)
        Mockito.doReturn(buildRequest()).`when`<Interceptor.Chain>(chain).request()
        val testAgent = "someUserAgentGoesHere"
        Mockito.doReturn(testAgent).`when`(userAgentInterceptor)?.userAgent

        // Act
        userAgentInterceptor!!.intercept(chain)

        // Assert
        val requestArgumentCaptor = ArgumentCaptor.forClass(
            Request::class.java
        )
        Mockito.verify<Interceptor.Chain>(chain).proceed(requestArgumentCaptor.capture())
        val requestWithUserAgent = requestArgumentCaptor.value
        Assert.assertEquals(testAgent, requestWithUserAgent.header("User-Agent"))
    }

    @get:Test
    val userAgent_returnsCorrectlyFormattedUserAgent: Unit
        get() {
            Mockito.mockStatic(AppInfo::class.java)
                .use { appInfoMockedStatic ->
                    appInfoMockedStatic.`when`<Any> {
                        getApplicationName(
                            ArgumentMatchers.any()
                        )
                    }.thenReturn(APP_NAME)
                    appInfoMockedStatic.`when`<Any> {
                        getApplicationVersion(
                            ArgumentMatchers.any()
                        )
                    }.thenReturn(APP_VERSION)
                    appInfoMockedStatic.`when`<Any> {
                        getApplicationPackageName(
                            ArgumentMatchers.any()
                        )
                    }.thenReturn(APP_PACKAGE_NAME)
                    appInfoMockedStatic.`when`<Any>{(AppInfo::androidLevel)}
                        .thenReturn(ANDROID_LEVEL)
                    appInfoMockedStatic.`when`<Any>{(AppInfo::androidVersion)}
                        .thenReturn(ANDROID_VERSION)
                    appInfoMockedStatic.`when`<Any>{(AppInfo::attentiveSDKVersion)}
                        .thenReturn(ATTENTIVE_SDK_VERSION)
                    appInfoMockedStatic.`when`<Any>{(AppInfo::attentiveSDKName)}
                        .thenReturn(ATTENTIVE_SDK_NAME)

                    val userAgent = userAgentInterceptor!!.userAgent
                    Assert.assertEquals(
                        "appName-Value/" + APP_VERSION + " (" + APP_PACKAGE_NAME + "; Android " + ANDROID_VERSION + "; Android API Level " + ANDROID_LEVEL + ") " + ATTENTIVE_SDK_NAME + "/" + ATTENTIVE_SDK_VERSION,
                        userAgent
                    )
                }
        }

    private fun buildRequest(): Request {
        return Request.Builder().url("https://attentive.com").get().build()
//        return Request(
//            HttpUrl.parse()!!,
//            "GET",
//            of.of(Map.of<String, String>()),
//            null,
//            Map.of<Class<*>, Any>()
//        )
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