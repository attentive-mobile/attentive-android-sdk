package com.attentive.androidsdk.creatives;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
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
import timber.log.Timber;

public class Creative {
    private static final Set<String> CREATIVE_LISTENER_ALLOWED_ORIGINS = Set.of("https://creatives.attn.tv");
    private static final String CREATIVE_LISTENER_JS = "javascript:(async function() {\n" +
            "    window.addEventListener('visibilitychange', \n" +
            "        function(event){\n" +
            "           CREATIVE_LISTENER.postMessage(`document-visibility: ${document.hidden}`);\n" +
            "        },\n" +
            "    false);\n" +
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

    /**
     * Creates a new Creative instance. Used to display and control creatives.
     * @param attentiveConfig The AttentiveConfig instance to use.
     * @param parentView The view to add the WebView to.
     */
    public Creative(AttentiveConfig attentiveConfig, View parentView) {
        this(attentiveConfig, parentView, null);
    }

    /**
     * Creates a new Creative instance. Used to display and control creatives.
     * @param attentiveConfig The AttentiveConfig instance to use.
     * @param parentView The view to add the WebView to.
     * @param activity The Activity to use for lifecycle callbacks.
     */
    public Creative(AttentiveConfig attentiveConfig, View parentView, @Nullable Activity activity) {
        Timber.d("Calling constructor of Creative with parameters: %s, %s, %s", attentiveConfig, parentView, activity);
        Timber.i("Android version: %s", Build.VERSION.SDK_INT);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && activity != null) {
            // Delegate to CreativeActivityCallbacks to handle lifecycle events
            activity.registerActivityLifecycleCallbacks(new CreativeActivityCallbacks(this));
        }
    }

    /**
     * Triggers to show the creative.
     */
    public void trigger() {
        trigger(null);
    }

    /**
     * Triggers to show the creative.
     * @param callback {@link CreativeTriggerCallback} to be called when the creative updates it's state.
     */
    public void trigger(@Nullable CreativeTriggerCallback callback) {
        trigger(callback, null);
    }

    /**
     * Triggers to show the creative.
     * @param callback {@link CreativeTriggerCallback} to be called when the creative updates it's state.
     * @param creativeId The creative ID to use. If not provided it will render the creative determined by online configuration.
     */
    public void trigger(@Nullable CreativeTriggerCallback callback, @Nullable String creativeId) {
        Timber.d("trigger method called with parameters: %s, %s", callback, creativeId);
        Timber.i("WebView is null: %s", webView == null);
        triggerCallback = callback;

        if (webView == null) {
            Timber.e("WebView not properly created or `destroy` already called on this Creative. Cannot trigger Creative after destroyed.");
            if (triggerCallback != null) {
                triggerCallback.onCreativeNotOpened();
            }
            return;
        }

        Timber.i("Attempting to trigger creative with attn domain %s, width %s, and height %s",
                        attentiveConfig.getDomain(),
                        webView.getWidth(), webView.getHeight());

        String url = creativeUrlFormatter.buildCompanyCreativeUrl(attentiveConfig, creativeId);

        if (attentiveConfig.getMode().equals(AttentiveConfig.Mode.DEBUG)) {
            changeWebViewVisibility(true);
        }

        if (isCreativeOpen.get()) {
            Timber.w("Attempted to trigger creative, but creative is currently open. Taking no action");
            return;
        }
        Timber.i("Start loading creative with url %s", url);
        webView.loadUrl(url);
    }

    /**
     * Destroys the creative. If you are supporting android versions below Build.VERSION_CODES.Q you
     * should call this method from the Activity#onDestroy lifecycle method.
     * If you are only supporting android versions above Build.VERSION_CODES.Q there is no need to
     * call this method anywhere since it will be handled internally on the SDK.
     * The method is still exposed in case some use case requires you to completely close the
     * creative.
     */
    public void destroy() {
        Timber.d("destroy method called");
        isCreativeOpen.set(false);
        if (parentView != null && webView != null) {
            Timber.i("WebView removed from view hierarchy correctly");
            ((ViewGroup) parentView).removeView(webView);
        }
        // TODO: better thread-safety when destroying. Lock?
        if (webView != null) {
            // set the webView member variable to null BEFORE we destroy it so other code on other threads that check if
            // webView isn't null doesn't try to use it after it is destroyed
            WebView webViewToDestroy = webView;
            webView = null;
            webViewToDestroy.destroy();
            Timber.i("WebView destroyed correctly");
        }
    }

    /**
     * Called when the user presses the back button. If the creative is open, it will close it and
     * return true, otherwise it will return false.
     * @return true if the creative was closed, false otherwise.
     */
    public boolean onBackPressed() {
        Timber.d("onBackPressed method called");
        Timber.d("isCreativeOpen.get() = %s", isCreativeOpen.get());
        if (isCreativeOpen.get()) {
            closeCreative();
            return true;
        } else {
            return false;
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
            Timber.e("Creative listener cannot be attached!");
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
                    Timber.i("Page finished loading");
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
                        Timber.e("Error opening the URI '%s' from the WebView. Error message: '%s'", uri, e.getMessage());
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
            Timber.i("Creative message data %s", messageData);
            if (messageData != null) {
                if (messageData.equalsIgnoreCase("CLOSE")) {
                    closeCreative();
                } else if (messageData.equalsIgnoreCase("OPEN")) {
                    openCreative();
                } else if (messageData.equalsIgnoreCase("TIMED OUT")) {
                    onCreativeTimedOut();
                } else if (messageData.equalsIgnoreCase("document-visibility: true") && isCreativeOpen.get()) {
                    closeCreative();
                }
            }
        };
    }

    private void onCreativeTimedOut() {
        Timber.e("Creative timed out. Not showing WebView.");
        if (triggerCallback != null) {
            triggerCallback.onCreativeNotOpened();
        }
        isCreativeOpen.set(false);
    }

    private void openCreative() {
        handler.post(() -> {
            if (isCreativeOpen.get()) {
                Timber.w("Attempted to trigger creative, but creative is currently open. Taking no action");
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
                Timber.i("WebView correctly displayed");
            } else {
                Timber.w("The creative loaded but the WebView is null. Ignoring.");
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
                // The following line is needed to avoid showing the previously creative instance
                // on the web view if a single instance is being used to display two different
                // creatives
                webView.clearCache(true);
                isCreativeOpen.set(false);
                if (triggerCallback != null) {
                    triggerCallback.onClose();
                }
            } else {
                Timber.w("The user closed the creative but the WebView is null. Ignoring.");
                if (triggerCallback != null) {
                    triggerCallback.onCreativeNotClosed();
                }
            }
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
