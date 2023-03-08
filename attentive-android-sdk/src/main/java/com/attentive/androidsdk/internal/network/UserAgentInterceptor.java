package com.attentive.androidsdk.internal.network;

import android.content.Context;
import androidx.annotation.NonNull;
import com.attentive.androidsdk.internal.util.AppInfo;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class UserAgentInterceptor implements Interceptor {
    public static String USER_AGENT_HEADER_NAME = "User-Agent";

    private final Context context;

    public UserAgentInterceptor(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Request requestWithUserAgentAdded = request.newBuilder().header(USER_AGENT_HEADER_NAME, getUserAgent()).build();
        return chain.proceed(requestWithUserAgentAdded);
    }

    private String getUserAgent() {
        return String.format("attentive-android-sdk/%s (Android %s; Android API Level %s) %s/%s (%s)", AppInfo.getAttentiveSDKVersion(), AppInfo.getAndroidVersion(), AppInfo.getAndroidLevel(), AppInfo.getApplicationName(context), AppInfo.getApplicationVersion(context), AppInfo.getApplicationPackageName(context));
    }
}
