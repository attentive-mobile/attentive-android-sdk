package com.attentive.androidsdk.creatives;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import com.attentive.androidsdk.AttentiveConfig;
import com.attentive.androidsdk.ClassFactory;
import com.attentive.androidsdk.UserIdentifiers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;

public class Creative {
    private static final Set<String> CREATIVE_LISTENER_ALLOWED_ORIGINS = Set.of("https://creatives.attn.tv");
    private static final String CREATIVE_LISTENER_JS = "javascript:(async function() {\n" +
            "    window.addEventListener('message',\n" +
            "        function(event){\n" +
            "            if (event.data && event.data.__attentive && event.data.__attentive.action === 'CLOSE') {\n" +
            "                CREATIVE_LISTENER.postMessage('CLOSE');\n" +
            "            }\n" +
            "        },\n" +
            "    false);\n" +
            "    var timeoutHandle = null;\n" +
            "    const interval = setInterval(function() {\n" +
            "        e =document.querySelector('iframe');\n" +
            "        if(e && e.id === 'attentive_creative') {\n" +
            "           clearInterval(interval);\n" +
            "           CREATIVE_LISTENER.postMessage('OPEN');\n" +
            "           if (timeoutHandle != null) {\n" +
            "               clearTimeout(timeoutHandle);\n" +
            "           }\n" +
            "        }\n" +
            "    }, 100);\n" +
            "    timeoutHandle = setTimeout(function() {\n" +
            "        clearInterval(interval);\n" +
            "        CREATIVE_LISTENER.postMessage('TIMED OUT');\n" +
            "    }, 5000);\n" +
            "\n" +
            "})()";

    private final AttentiveConfig attentiveConfig;
    private final View parentView;

    private final Handler handler;
    private final WebViewClient webViewClient;
    private final WebViewCompat.WebMessageListener creativeListener;
    private final ObjectMapper objectMapper;

    private WebView webView;


    public Creative(AttentiveConfig attentiveConfig, View parentView) {
        this.attentiveConfig = attentiveConfig;
        this.parentView = parentView;

        this.handler = new Handler();
        this.webViewClient = createWebViewClient();
        this.creativeListener = createCreativeListener();

        this.webView = createWebView(parentView);

        webView.setVisibility(View.INVISIBLE);
        ((ViewGroup) parentView).addView(
                webView, new ViewGroup.LayoutParams(parentView.getLayoutParams()));

        this.objectMapper = ClassFactory.buildObjectMapper();
    }

    public void trigger() {
        if (webView == null) {
            Log.e(this.getClass().getName(), "WebView not properly created or `destroy` already called on this Creative. Cannot trigger Creative after destroyed.");
            return;
        }

        String url = buildCompanyCreativeUrl();

        if (attentiveConfig.getMode().equals(AttentiveConfig.Mode.DEBUG)) {
            webView.setVisibility(View.VISIBLE);
        }

        webView.loadUrl(url);
    }

    private String buildCompanyCreativeUrl() {
        Uri.Builder uriBuilder =
            getCompanyCreativeUriBuilder(attentiveConfig.getDomain(), attentiveConfig.getMode());

        UserIdentifiers userIdentifiers = attentiveConfig.getUserIdentifiers();

        addUserIdentifiersAsParameters(uriBuilder, userIdentifiers);

        return uriBuilder.build().toString();
    }

    private void addUserIdentifiersAsParameters(Uri.Builder builder,
                                                UserIdentifiers userIdentifiers) {
        if (userIdentifiers.getVisitorId() != null) {
            builder.appendQueryParameter("vid", userIdentifiers.getVisitorId());
        } else {
            Log.e(this.getClass().getName(), "No VisitorId found. This should not happen.");
        }

        if (userIdentifiers.getClientUserId() != null) {
            builder.appendQueryParameter("cuid", userIdentifiers.getClientUserId());
        }
        if (userIdentifiers.getPhone() != null) {
            builder.appendQueryParameter("p", userIdentifiers.getPhone());
        }
        if (userIdentifiers.getEmail() != null) {
            builder.appendQueryParameter("e", userIdentifiers.getEmail());
        }
        if (userIdentifiers.getKlaviyoId() != null) {
            builder.appendQueryParameter("kid", userIdentifiers.getKlaviyoId());
        }
        if (userIdentifiers.getShopifyId() != null) {
            builder.appendQueryParameter("sid", userIdentifiers.getShopifyId());
        }
        if (!userIdentifiers.getCustomIdentifiers().isEmpty()) {
            builder.appendQueryParameter("cstm", getCustomIdentifiersJson(userIdentifiers));
        }
    }

    private String getCustomIdentifiersJson(UserIdentifiers userIdentifiers) {
        try {
            return objectMapper.writeValueAsString(userIdentifiers.getCustomIdentifiers());
        } catch (JsonProcessingException e) {
            Log.e(this.getClass().getName(), "Could not serialize the custom identifiers. Message: " + e.getMessage());
            return "{}";
        }
    }

    public void destroy() {
        if (parentView != null && webView != null) {
            ((ViewGroup) parentView).removeView(webView);
        }
        // TODO: better thread-safety when destroying. Lock?
        if (webView != null) {
            // set the webView member variable to null BEFORE we destroy it so other code on other threads that check if
            // webView isn't null doesn't try to use it after it is destroyed
            WebView webViewToDestroy = webView;
            webView = null;
            webViewToDestroy.destroy();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private WebView createWebView(View parentView) {
        WebView view = new WebView(parentView.getContext());
        WebSettings webSettings = view.getSettings();

        // Security settings, allow JavaScript to run
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setAllowUniversalAccessFromFileURLs(false);
        webSettings.setAllowFileAccess(false);
        webSettings.setJavaScriptEnabled(true);

        view.setWebViewClient(webViewClient);

        // Add listener for creative OPEN / CLOSE events
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(
                    view, "CREATIVE_LISTENER", CREATIVE_LISTENER_ALLOWED_ORIGINS, creativeListener);
        } else {
            Log.e(this.getClass().getName(), "Creative listener cannot be attached!");
        }

        if (attentiveConfig.getMode() == AttentiveConfig.Mode.PRODUCTION) {
            view.setBackgroundColor(Color.TRANSPARENT);
        }
        return view;
    }

    private WebViewClient createWebViewClient() {
        return new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (view.getProgress() == 100) {
                    Log.i(this.getClass().getName(), "Page finished loading");
                    view.loadUrl(CREATIVE_LISTENER_JS);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String uri) {
                final String lowercaseUri = uri.toLowerCase();
                if (lowercaseUri.startsWith("sms://") || lowercaseUri.startsWith("http://") || lowercaseUri.startsWith("https://")) {
                    try {
                        // This tells Android to open the URI in an app that is relevant for the URI.
                        // i.e. for "sms://" it will open the messaging app and for "http://" it will
                        // open the browser
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        view.getContext().startActivity(intent);
                    } catch (Exception e) {
                        Log.e(this.getClass().getName(), String.format("Error opening the URI '%s' from the webview. Error message: '%s'", uri, e.getMessage()));
                    }

                    // Don't render the URI in the webview since the above code tells Android to open the URI in a new app
                    return true;
                }

                return false;
            }
        };
    }

    private WebViewCompat.WebMessageListener createCreativeListener() {
        return (view, message, sourceOrigin, isMainFrame, replyProxy) -> {
            String messageData = message.getData();
            if (messageData != null) {
                if (messageData.equalsIgnoreCase("CLOSE")) {
                    handler.post(() -> {
                        if (webView != null) {
                            webView.setVisibility(View.INVISIBLE);
                        } else {
                            Log.w(this.getClass().getName(), "The user closed the creative but the webview is null. Ignoring.");
                        }
                    });
                } else if (messageData.equalsIgnoreCase("OPEN")) {
                    handler.post(() -> {
                        // Host apps have reported webView NPEs here. The current thinking is that destroy gets
                        // called just before this callback is executed. If destroy was previously called then it's
                        // okay to ignore these callbacks since the host app has told us the creative should no longer
                        // be displayed.
                        if (webView != null) {
                            webView.setVisibility(View.VISIBLE);
                        } else {
                            Log.w(this.getClass().getName(), "The creative loaded but the webview is null. Ignoring.");
                        }
                    });
                } else if (messageData.equalsIgnoreCase("TIMED OUT")) {
                    Log.e(this.getClass().getName(), "Creative timed out. Not showing WebView.");
                }
            }
        };
    }

    private Uri.Builder getCompanyCreativeUriBuilder(String domain, AttentiveConfig.Mode mode) {
        Uri.Builder creativeUriBuilder = new Uri.Builder()
                .scheme("https")
                .authority("creatives.attn.tv")
                .path("mobile-apps/index.html")
                .appendQueryParameter("domain", domain);


        if (mode == AttentiveConfig.Mode.DEBUG) {
            creativeUriBuilder.appendQueryParameter("debug", "matter-trip-grass-symbol");
        }

        return creativeUriBuilder;
    }
}
