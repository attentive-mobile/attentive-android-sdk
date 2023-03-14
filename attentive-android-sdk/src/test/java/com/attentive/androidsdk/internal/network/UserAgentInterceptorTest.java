package com.attentive.androidsdk.internal.network;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import com.attentive.androidsdk.internal.util.AppInfo;
import java.io.IOException;
import java.util.Map;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class UserAgentInterceptorTest {
    private static final String APP_NAME = "appNameValue";
    private static final String APP_VERSION = "appVersionValue";
    private static final String APP_PACKAGE_NAME = "com.what.exampleapp";
    private static final String ANDROID_VERSION = "androidVersion";
    private static final String ANDROID_LEVEL = "androidLevel";
    private static final String ATTENTIVE_SDK_VERSION = "attentiveSdkValue";

    private UserAgentInterceptor userAgentInterceptor;

    @Before
    public void setup() {
        userAgentInterceptor = spy(new UserAgentInterceptor(mock(Context.class)));
    }

    @Test
    public void intercept_interceptCalled_customUserAgentIsAdded() throws IOException {
        // Arrange
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        doReturn(buildRequest()).when(chain).request();
        final String testAgent = "someUserAgentGoesHere";
        doReturn(testAgent).when(userAgentInterceptor).getUserAgent();

        // Act
        userAgentInterceptor.intercept(chain);

        // Assert
        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(chain).proceed(requestArgumentCaptor.capture());
        Request requestWithUserAgent = requestArgumentCaptor.getValue();
        assertEquals(testAgent, requestWithUserAgent.header("User-Agent"));
    }

    @Test
    public void getUserAgent_returnsCorrectlyFormattedUserAgent() {
        try (MockedStatic<AppInfo> appInfoMockedStatic = Mockito.mockStatic(AppInfo.class)) {
            appInfoMockedStatic.when(() -> AppInfo.getApplicationName(any())).thenReturn(APP_NAME);
            appInfoMockedStatic.when(() -> AppInfo.getApplicationVersion(any())).thenReturn(APP_VERSION);
            appInfoMockedStatic.when(() -> AppInfo.getApplicationPackageName(any())).thenReturn(APP_PACKAGE_NAME);
            appInfoMockedStatic.when(AppInfo::getAndroidLevel).thenReturn(ANDROID_LEVEL);
            appInfoMockedStatic.when(AppInfo::getAndroidVersion).thenReturn(ANDROID_VERSION);
            appInfoMockedStatic.when(AppInfo::getAttentiveSDKVersion).thenReturn(ATTENTIVE_SDK_VERSION);

            String userAgent = userAgentInterceptor.getUserAgent();
            assertEquals(APP_NAME + "/" + APP_VERSION + " (" + APP_PACKAGE_NAME + "; Android " + ANDROID_VERSION + "; Android API Level " + ANDROID_LEVEL + ") attentive-android-sdk/" + ATTENTIVE_SDK_VERSION, userAgent);
        }
    }

    private Request buildRequest() {
        return new Request(HttpUrl.parse("https://attentive.com"), "GET", Headers.of(Map.of()), null, Map.of());
    }
}