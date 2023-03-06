package com.attentive.androidsdk.internal.util;

import android.util.Log;

public class VersionValidator {
    private static final int MINIMUM_ANDROID_VERSION = android.os.Build.VERSION_CODES.O;

    public static boolean isBuildVersionSupported() {
        if (android.os.Build.VERSION.SDK_INT >= MINIMUM_ANDROID_VERSION) {
            return true;
        }
        Log.w(
                VersionValidator.class.toString(),
                String.format(
                        "The build version: %d is not supported by the attentive-android-sdk - " +
                                "calls will not error out but they will no-op. The minimum supported" +
                                " version is: %d.",
                        android.os.Build.VERSION.SDK_INT,
                        MINIMUM_ANDROID_VERSION)
        );
        return false;
    }
}
