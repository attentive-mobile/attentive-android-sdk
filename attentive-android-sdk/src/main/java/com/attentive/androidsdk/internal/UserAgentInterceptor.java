package com.attentive.androidsdk.internal;

import android.content.Context;
import androidx.annotation.NonNull;
import com.attentive.androidsdk.internal.util.UserAgentBuilder;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class UserAgentInterceptor implements Interceptor {
    private final Context context;

    public UserAgentInterceptor(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Request requestWithUserAgentAdded = request.newBuilder().header(UserAgentBuilder.USER_AGENT_HEADER_NAME, getUserAgent()).build();
        return chain.proceed(requestWithUserAgentAdded);
    }

    private String getUserAgent() {
        return UserAgentBuilder.getUserAgent(context);
    }
}
