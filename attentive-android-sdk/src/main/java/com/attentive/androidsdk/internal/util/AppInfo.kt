package com.attentive.androidsdk.internal.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RestrictTo
import com.attentive.androidsdk.BuildConfig

object AppInfo {
    @JvmStatic
    val androidVersion: String
        /**
         * @return the marketing Android version e.g. 10
         */
        get() = Build.VERSION.RELEASE

    @JvmStatic
    val androidLevel: String
        /**
         * @return the Android API level e.g. 33
         */
        get() = Build.VERSION.SDK_INT.toString()

    /**
     * @return the host app's readable name e.g. 'CompanyApp'
     */
    @JvmStatic
    fun getApplicationName(context: Context): String? {
        val labelRes = context.applicationInfo.labelRes
        if (labelRes == 0) {
            val nonLocalizedLabel = context.applicationInfo.nonLocalizedLabel
            return nonLocalizedLabel?.toString()
        } else {
            return context.getString(labelRes)
        }
    }

    /**
     * @return the host app's package name e.g. 'com.mycompany.CompanyApp'
     */
    @JvmStatic
    fun getApplicationPackageName(context: Context): String {
        return context.applicationContext.packageName
    }

    /**
     * @return the host app's version e.g. '1.5'
     */
    @JvmStatic
    fun getApplicationVersion(context: Context): String? {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    @JvmStatic
    val attentiveSDKVersion: String
        /**
         * @return the Attentive SDK's version e.g. '0.3.2'
         */
        get() = BuildConfig.VERSION_NAME

    @JvmStatic
    val attentiveSDKName: String
        /**
         * @return the Attentive SDK's name e.g. 'attentive-android-sdk'
         */
        get() = "attentive-android-sdk"

    /**
     * @return true if the host app is debuggable
     */
    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun isDebuggable(context: Context): Boolean {
        return ((context.applicationInfo.flags
                and ApplicationInfo.FLAG_DEBUGGABLE) != 0)
    }
}
