package com.attentive.androidsdk;

import android.content.Context;
import android.net.Uri;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import okhttp3.HttpUrl;
import okhttp3.Request;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Arrange
        // Act
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Assert
        assertEquals("com.attentive.androidsdk.test", appContext.getPackageName());

        HttpUrl url = new HttpUrl.Builder().scheme("https").host("google.com").addQueryParameter("domain", "{\"key\": \"value\"}").addQueryParameter("other", "[{\"key\": \"value\"}]").build();

        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority("events.dev.attentivemobile.com")
                .path("e")
                .appendQueryParameter("domain", "{\"key\": \"value\"}")
                .appendQueryParameter("other", "[{\"key\": \"value\"}]");
        String s = builder.toString();
        assertTrue(s != null);
    }
}
