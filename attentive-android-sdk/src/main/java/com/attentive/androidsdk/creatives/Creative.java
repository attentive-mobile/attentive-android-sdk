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
import androidx.annotation.Nullable;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import com.attentive.androidsdk.AttentiveConfig;
import com.attentive.androidsdk.ClassFactory;
import com.attentive.androidsdk.internal.util.CreativeUrlFormatter;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class Creative {
    public static final String TAG = Creative.class.getSimpleName();
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
    // Making this atomic to make sure it doesn't run into any race conditions
    private static final AtomicBoolean isCreativeOpen = new AtomicBoolean(false);

    private final AttentiveConfig attentiveConfig;
    private final CreativeUrlFormatter creativeUrlFormatter;
    private final View parentView;
    private final Handler handler;
    private final WebViewClient webViewClient;
    private final WebViewCompat.WebMessageListener creativeListener;
    @Nullable
    private WebView webView;
    @Nullable
    private CreativeTriggerCallback triggerCallback;

    public Creative(AttentiveConfig attentiveConfig, View parentView) {
        this.attentiveConfig = attentiveConfig;
        this.parentView = parentView;

        this.handler = new Handler();
        this.webViewClient = createWebViewClient();
        this.creativeListener = createCreativeListener();

        this.webView = createWebView(parentView);

        changeWebViewVisibility(false);
        ((ViewGroup) parentView).addView(
                webView, new ViewGroup.LayoutParams(parentView.getLayoutParams()));

        this.creativeUrlFormatter = new CreativeUrlFormatter(ClassFactory.buildObjectMapper());
    }

    public void trigger() {
        trigger(null);
    }

    public void trigger(@Nullable CreativeTriggerCallback callback) {
        trigger(callback, null);
    }

    /**
     * Triggers to show the creative.
     * @param callback {@link CreativeTriggerCallback} to be called when the creative updates it's state.
     * @param creativeId The creative ID to use. If not provided it will render the creative determined by online configuration.
     */
    public void trigger(@Nullable CreativeTriggerCallback callback, @Nullable String creativeId) {
        triggerCallback = callback;

        if (webView == null) {
            Log.e(TAG, "WebView not properly created or `destroy` already called on this Creative. Cannot trigger Creative after destroyed.");
            if (triggerCallback != null) {
                triggerCallback.onCreativeNotOpened();
            }
            return;
        }

        Log.i(TAG,
                String.format("Attempting to trigger creative with attn domain %s, width %s, and height %s",
                        attentiveConfig.getDomain(),
                        webView.getWidth(), webView.getHeight()));

        String url = creativeUrlFormatter.buildCompanyCreativeUrl(attentiveConfig, creativeId);

        if (attentiveConfig.getMode().equals(AttentiveConfig.Mode.DEBUG)) {
            changeWebViewVisibility(true);
        }

        if (isCreativeOpen.get()) {
            Log.w(TAG, "Attempted to trigger creative, but creative is currently open. Taking no action");
            return;
        }
        webView.loadUrl(url);
    }

    public void destroy() {
        Log.i(TAG, "Destroying creative");
        isCreativeOpen.set(false);
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
        webSettings.setDomStorageEnabled(true);

        view.setWebViewClient(webViewClient);

        // Add listener for creative OPEN / CLOSE events
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(
                    view, "CREATIVE_LISTENER", CREATIVE_LISTENER_ALLOWED_ORIGINS, creativeListener);
        } else {
            Log.e(TAG, "Creative listener cannot be attached!");
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
                    Log.i(TAG, "Page finished loading");
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
                        // Usually, our Creative will be rendered inside of an Activity. However, this is not required,
                        // and some clients may choose not to do so (for example they can be rendered directly in the
                        // DecorView). Adding this flag allows Intents to work both from inside and outside an Activity.
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        view.getContext().startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, String.format("Error opening the URI '%s' from the WebView. Error message: '%s'", uri, e.getMessage()));
                    }

                    // Don't render the URI in the WebView since the above code tells Android to open the URI in a new app
                    return true;
                }

                return false;
            }
        };
    }

    private WebViewCompat.WebMessageListener createCreativeListener() {
        return (view, message, sourceOrigin, isMainFrame, replyProxy) -> {
            String messageData = message.getData();
            Log.i(TAG, String.format("Creative message data %s", messageData));
            if (messageData != null) {
                if (messageData.equalsIgnoreCase("CLOSE")) {
                    closeCreative();
                } else if (messageData.equalsIgnoreCase("OPEN")) {
                    openCreative();
                } else if (messageData.equalsIgnoreCase("TIMED OUT")) {
                    onCreativeTimedOut();
                }
            }
        };
    }

    private void onCreativeTimedOut() {
        Log.e(TAG, "Creative timed out. Not showing WebView.");
        if (triggerCallback != null) {
            triggerCallback.onCreativeNotOpened();
        }
        isCreativeOpen.set(false);
    }

    private void openCreative() {
        handler.post(() -> {
            if (isCreativeOpen.get()) {
                Log.w(TAG, "Attempted to trigger creative, but creative is currently open. Taking no action");
                return;
            }
            // Host apps have reported webView NPEs here. The current thinking is that destroy gets
            // called just before this callback is executed. If destroy was previously called then it's
            // okay to ignore these callbacks since the host app has told us the creative should no longer
            // be displayed.
            if (webView != null) {
                changeWebViewVisibility(true);
                webView.requestLayout();
                isCreativeOpen.set(true);
                if (triggerCallback != null) {
                    triggerCallback.onOpen();
                }
            } else {
                Log.w(TAG, "The creative loaded but the WebView is null. Ignoring.");
                if (triggerCallback != null) {
                    triggerCallback.onCreativeNotOpened();
                }
            }
        });
    }

    private void closeCreative() {
        handler.post(() -> {
            if (webView != null) {
                changeWebViewVisibility(false);
                if (triggerCallback != null) {
                    triggerCallback.onClose();
                }
            } else {
                Log.w(TAG, "The user closed the creative but the WebView is null. Ignoring.");
                if (triggerCallback != null) {
                    triggerCallback.onCreativeNotClosed();
                }
            }
            isCreativeOpen.set(false);
        });
    }

    private void changeWebViewVisibility(boolean visible) {
        if (webView != null) {
            if (visible) {
                webView.setVisibility(View.VISIBLE);
            } else {
                webView.setVisibility(View.INVISIBLE);
            }
        }
    }
}
