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
import com.attentive.androidsdk.UserIdentifiers;
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
            builder.appendQueryParameter("visitor_id", userIdentifiers.getVisitorId());
        } else {
            Log.e(this.getClass().getName(), "No VisitorId found. This should not happen.");
        }

        if (userIdentifiers.getClientUserId() != null) {
            builder.appendQueryParameter("client_user_id", userIdentifiers.getClientUserId());
        }
        if (userIdentifiers.getPhone() != null) {
            builder.appendQueryParameter("phone", userIdentifiers.getPhone());
        }
        if (userIdentifiers.getEmail() != null) {
            builder.appendQueryParameter("email", userIdentifiers.getEmail());
        }
        if (userIdentifiers.getKlaviyoId() != null) {
            builder.appendQueryParameter("klaviyo_id", userIdentifiers.getKlaviyoId());
        }
        if (userIdentifiers.getShopifyId() != null) {
            builder.appendQueryParameter("shopify_id", userIdentifiers.getShopifyId());
        }
    }

    public void destroy() {
        if (parentView != null && webView != null) {
            ((ViewGroup) parentView).removeView(webView);
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
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
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("sms://")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        view.getContext().startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        return true;
                    }
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
                    handler.post(() -> webView.setVisibility(View.INVISIBLE));
                } else if (messageData.equalsIgnoreCase("OPEN")) {
                    handler.post(() -> webView.setVisibility(View.VISIBLE));
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
