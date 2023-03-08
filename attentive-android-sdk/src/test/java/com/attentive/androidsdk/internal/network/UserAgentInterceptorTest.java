package com.attentive.androidsdk.internal.network;

import static org.junit.Assert.*;
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

public class UserAgentInterceptorTest {
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

    private Request buildRequest() {
        return new Request(HttpUrl.parse("https://attentive.com"), "GET", Headers.of(Map.of()), null, Map.of());
    }
}