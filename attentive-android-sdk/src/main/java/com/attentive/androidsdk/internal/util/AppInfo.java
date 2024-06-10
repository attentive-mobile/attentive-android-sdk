package com.attentive.androidsdk.internal.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import com.attentive.androidsdk.BuildConfig;

public class AppInfo {
    /**
     * @return the marketing Android version e.g. 10
     */
    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * @return the Android API level e.g. 33
     */
    public static String getAndroidLevel() {
        return String.valueOf(Build.VERSION.SDK_INT);
    }

    /**
     * @return the host app's readable name e.g. 'CompanyApp'
     */
    @Nullable
    public static String getApplicationName(Context context) {
        int labelRes = context.getApplicationInfo().labelRes;
        if (labelRes == 0) {
            CharSequence nonLocalizedLabel = context.getApplicationInfo().nonLocalizedLabel;
            if (nonLocalizedLabel == null) {
                return null;
            } else {
                return nonLocalizedLabel.toString();
            }
        } else {
            return context.getString(labelRes);
        }
    }

    /**
     * @return the host app's package name e.g. 'com.mycompany.CompanyApp'
     */
    public static String getApplicationPackageName(Context context) {
        return context.getApplicationContext().getPackageName();
    }

    /**
     * @return the host app's version e.g. '1.5'
     */
    @Nullable
    public static String getApplicationVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * @return the Attentive SDK's version e.g. '0.3.2'
     */
    public static String getAttentiveSDKVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * @return the Attentive SDK's name e.g. 'attentive-android-sdk'
     */
    public static String getAttentiveSDKName() {
        return "attentive-android-sdk";
    }

    /**
     * @return true if the host app is debuggable
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static boolean isDebuggable(Context context) {
        return ((context.getApplicationInfo().flags
                & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
    }
}
