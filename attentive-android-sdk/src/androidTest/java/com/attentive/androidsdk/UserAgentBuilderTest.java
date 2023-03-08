package com.attentive.androidsdk;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.doReturn;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.android.dx.mockito.inline.extended.StaticMockitoSession;
import com.attentive.androidsdk.internal.util.AppInfo;
import com.attentive.androidsdk.internal.util.UserAgentBuilder;
import java.io.Console;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
public class UserAgentBuilderTest {
    private static final String FAKE_APPLICATION_NAME = "someName";
    private Context context;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void getUserAgent_returnsFormattedString() {
        String actualUserAgent = UserAgentBuilder.getUserAgent(context);
        // The app under test does not have an app name nor app version, hence the nulls
        assertEquals("attentive-android-sdk/0.3.3 (Android 12) null/null (com.attentive.androidsdk.test)", actualUserAgent);
    }
}