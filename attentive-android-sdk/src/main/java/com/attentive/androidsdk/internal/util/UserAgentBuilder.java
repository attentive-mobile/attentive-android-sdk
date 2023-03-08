package com.attentive.androidsdk.internal.util;

import android.content.Context;

public class UserAgentBuilder {
    public static String USER_AGENT_HEADER_NAME = "User-Agent";

    public static String getUserAgent(Context context) {
        return String.format("attentive-android-sdk/%s (Android %s; Android API Level %s) %s/%s (%s)", AppInfo.getAttentiveSDKVersion(), AppInfo.getAndroidVersion(), AppInfo.getAndroidLevel(), AppInfo.getApplicationName(context), AppInfo.getApplicationVersion(context), AppInfo.getApplicationPackageName(context));
    }
}
