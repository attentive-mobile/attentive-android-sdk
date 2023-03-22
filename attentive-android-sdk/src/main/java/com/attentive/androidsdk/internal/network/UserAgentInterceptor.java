package com.attentive.androidsdk.internal.network;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.attentive.androidsdk.internal.util.AppInfo;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class UserAgentInterceptor implements Interceptor {
    static String USER_AGENT_HEADER_NAME = "User-Agent";

    private final Context context;

    public UserAgentInterceptor(Context context) {
        this.context = context;
    }

    // This adds the user agent header to every request that the OkHttpClient makes
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Request requestWithUserAgentAdded = request.newBuilder().header(USER_AGENT_HEADER_NAME, getUserAgent()).build();
        return chain.proceed(requestWithUserAgentAdded);
    }

    @VisibleForTesting
    String getUserAgent() {
        final String appNameWithDashes =
            AppInfo.getApplicationName(context) == null ? null : AppInfo.getApplicationName(context).replace(" ", "-");
        return String.format("%s/%s (%s; Android %s; Android API Level %s) %s/%s",
            appNameWithDashes,
            AppInfo.getApplicationVersion(context),
            AppInfo.getApplicationPackageName(context),
            AppInfo.getAndroidVersion(),
            AppInfo.getAndroidLevel(),
            AppInfo.getAttentiveSDKName(),
            AppInfo.getAttentiveSDKVersion());
    }
}
