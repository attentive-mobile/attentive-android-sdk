package com.attentive.androidsdk.creatives

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.VisibleForTesting
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewCompat.WebMessageListener
import androidx.webkit.WebViewFeature
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.ClassFactory
import com.attentive.androidsdk.internal.util.CreativeUrlFormatter
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class Creative constructor(
    attentiveConfig: AttentiveConfig,
    parentView: View,
    activity: Activity? = null,
) {
    private val attentiveConfig: AttentiveConfig
    private val creativeUrlFormatter: CreativeUrlFormatter
    private val parentView: View?
    private val handler: Handler
    private val webViewClient: WebViewClient
    private val creativeListener: WebMessageListener
    internal var webView: WebView?
    private var triggerCallback: CreativeTriggerCallback? = null

    // Secondary constructor for testing
    @VisibleForTesting
    internal constructor(
        attentiveConfig: AttentiveConfig,
        parentView: View,
        activity: Activity? = null,
        webView: WebView
    ) : this(attentiveConfig, parentView, activity) {
        this.webView = webView
    }

    /**
     * Creates a new Creative instance. Used to display and control creatives.
     * @param attentiveConfig The AttentiveConfig instance to use.
     * @param parentView The view to add the WebView to.
     * @param activity The Activity to use for lifecycle callbacks.
     */
    /**
     * Creates a new Creative instance. Used to display and control creatives.
     * @param attentiveConfig The AttentiveConfig instance to use.
     * @param parentView The view to add the WebView to.
     */
    init {
        Timber.d(
            "Calling constructor of Creative with parameters: %s, %s, %s",
            attentiveConfig,
            parentView,
            activity
        )
        Timber.i("Android version: %s", Build.VERSION.SDK_INT)
        this.attentiveConfig = attentiveConfig
        this.parentView = parentView

        this.handler = Handler()
        this.webViewClient = createWebViewClient()
        this.creativeListener = createCreativeListener()


        this.webView = createWebView(parentView)

        changeWebViewVisibility(false)
        (parentView as ViewGroup).addView(
            webView, ViewGroup.LayoutParams(parentView.getLayoutParams())
        )

        this.creativeUrlFormatter = CreativeUrlFormatter()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && activity != null) {
            // Delegate to CreativeActivityCallbacks to handle lifecycle events
            activity.registerActivityLifecycleCallbacks(CreativeActivityCallbacks(this))
        }
    }

    /**
     * Triggers to show the creative.
     * @param callback [CreativeTriggerCallback] to be called when the creative updates it's state.
     * @param creativeId The creative ID to use. If not provided it will render the creative determined by online configuration.
     */
    /**
     * Triggers to show the creative.
     * @param callback [CreativeTriggerCallback] to be called when the creative updates it's state.
     */
    /**
     * Triggers to show the creative.
     */
    fun trigger(callback: CreativeTriggerCallback? = null, creativeId: String? = null) {
        Timber.d("trigger method called with parameters: %s, %s", callback, creativeId)
        Timber.i("WebView is null: %s", webView == null)

        if (isCreativeDestroyed.get()) {
            Timber.e("Attempted to trigger a destroyed creative. Ignoring.")
            return
        }

        triggerCallback = callback

        if (webView == null) {
            Timber.e("WebView not properly created or `destroy` already called on this Creative. Cannot trigger Creative after destroyed.")
            if (triggerCallback != null) {
                triggerCallback!!.onCreativeNotOpened()
            }
            return
        }

        Timber.i(
            "Attempting to trigger creative with attn domain %s, width %s, and height %s",
            attentiveConfig.domain,
            webView!!.width, webView!!.height
        )

        val url = creativeUrlFormatter.buildCompanyCreativeUrl(attentiveConfig, creativeId)

        if (attentiveConfig.mode == AttentiveConfig.Mode.DEBUG) {
            changeWebViewVisibility(true)
        }

        if (isCreativeOpening.get()) {
            Timber.w("Attempted to trigger creative, but creative is already opening. Taking no action")
            return
        }

        if (isCreativeOpen.get()) {
            Timber.w("Attempted to trigger creative, but creative is currently open. Taking no action")
            return
        }

        Timber.i("Start loading creative with url %s", url)
        isCreativeOpening.set(true)
        webView!!.loadUrl(url)
    }

    /**
     * Destroys the creative. If you are supporting android versions below Build.VERSION_CODES.Q you
     * should call this method from the Activity#onDestroy lifecycle method.
     * If you are only supporting android versions above Build.VERSION_CODES.Q there is no need to
     * call this method anywhere since it will be handled internally on the SDK.
     * The method is still exposed in case some use case requires you to completely close the
     * creative.
     */
    fun destroy() {
        Timber.d("destroy method called")
        isCreativeOpen.set(false)
        isCreativeOpening.set(false)
        if (parentView != null && webView != null) {
            Timber.i("WebView removed from view hierarchy correctly")
            (parentView as ViewGroup).removeView(webView)
        }
        // TODO: better thread-safety when destroying. Lock?
        if (webView != null) {
            // set the webView member variable to null BEFORE we destroy it so other code on other threads that check if
            // webView isn't null doesn't try to use it after it is destroyed
            val webViewToDestroy = webView
            webView = null
            webViewToDestroy?.destroy()
            Timber.i("WebView destroyed correctly")
        }
        isCreativeDestroyed.set(true)
    }

    /**
     * Called when the user presses the back button. If the creative is open, it will close it and
     * return true, otherwise it will return false.
     * @return true if the creative was closed, false otherwise.
     */
    fun onBackPressed(): Boolean {
        Timber.d("onBackPressed method called")
        Timber.d("isCreativeOpen.get() = %s", isCreativeOpen.get())
        if (isCreativeOpen.get()) {
            closeCreative()
            return true
        } else {
            return false
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    internal fun createWebView(parentView: View): WebView {
        val view = WebView(parentView.context)
        val webSettings = view.settings

        // Security settings, allow JavaScript to run
        webSettings.allowFileAccessFromFileURLs = false
        webSettings.allowUniversalAccessFromFileURLs = false
        webSettings.allowFileAccess = false
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        view.webViewClient = webViewClient
        view.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Timber.d(consoleMessage.message())
                return true
            }
        }

        // Add listener for creative OPEN / CLOSE events
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(
                view, "CREATIVE_LISTENER", CREATIVE_LISTENER_ALLOWED_ORIGINS, creativeListener
            )
        } else {
            Timber.e("Creative listener cannot be attached!")
        }

        if (attentiveConfig.mode == AttentiveConfig.Mode.PRODUCTION) {
            view.setBackgroundColor(Color.TRANSPARENT)
        }
        return view
    }

    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                if (view.progress == 100) {
                    Timber.i("Page finished loading")
                    view.loadUrl(CREATIVE_LISTENER_JS)
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, uri: String): Boolean {
                val lowercaseUri = uri.lowercase(Locale.getDefault())
                if (lowercaseUri.startsWith("sms://") || lowercaseUri.startsWith("http://") || lowercaseUri.startsWith(
                        "https://"
                    )
                ) {
                    try {
                        // This tells Android to open the URI in an app that is relevant for the URI.
                        // i.e. for "sms://" it will open the messaging app and for "http://" it will
                        // open the browser
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                        // Usually, our Creative will be rendered inside of an Activity. However, this is not required,
                        // and some clients may choose not to do so (for example they can be rendered directly in the
                        // DecorView). Adding this flag allows Intents to work both from inside and outside an Activity.
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        view.context.startActivity(intent)
                    } catch (e: Exception) {
                        Timber.e(
                            "Error opening the URI '%s' from the WebView. Error message: '%s'",
                            uri,
                            e.message
                        )
                    }

                    // Don't render the URI in the WebView since the above code tells Android to open the URI in a new app
                    return true
                }

                return false
            }
        }
    }

    private fun createCreativeListener(): WebMessageListener {
        return WebMessageListener { view: WebView?, message: WebMessageCompat, sourceOrigin: Uri?, isMainFrame: Boolean, replyProxy: JavaScriptReplyProxy? ->
            val messageData = message.data
            Timber.i("Creative message data %s", messageData)
            if (messageData != null) {
                if (messageData.equals("CLOSE", ignoreCase = true)) {
                    closeCreative()
                } else if (messageData.equals("OPEN", ignoreCase = true)) {
                    openCreative()
                } else if (messageData.equals("TIMED OUT", ignoreCase = true)) {
                    onCreativeTimedOut()
                } else if (messageData.equals(
                        "document-visibility: true",
                        ignoreCase = true
                    ) && isCreativeOpen.get()
                ) {
                    closeCreative()
                }
            }
        }
    }

    private fun onCreativeTimedOut() {
        Timber.e("Creative timed out. Not showing WebView.")
        if (triggerCallback != null) {
            triggerCallback!!.onCreativeNotOpened()
        }
        isCreativeOpen.set(false)
    }

    internal fun openCreative() {
        handler.post {
            isCreativeOpening.set(false)
            if (isCreativeOpen.get()) {
                Timber.w("Attempted to trigger creative, but creative is currently open. Taking no action")
                return@post
            }
            // Host apps have reported webView NPEs here. The current thinking is that destroy gets
            // called just before this callback is executed. If destroy was previously called then it's
            // okay to ignore these callbacks since the host app has told us the creative should no longer
            // be displayed.
            if (webView != null) {
                changeWebViewVisibility(true)
                webView!!.requestLayout()
                isCreativeOpening.set(false)
                isCreativeOpen.set(true)
                if (triggerCallback != null) {
                    triggerCallback!!.onOpen()
                }
                Timber.i("WebView correctly displayed")
            } else {
                Timber.w("The creative loaded but the WebView is null. Ignoring.")
                isCreativeOpening.set(false)
                if (triggerCallback != null) {
                    triggerCallback!!.onCreativeNotOpened()
                }
            }
        }
    }

    internal fun closeCreative() {
        handler.post {
            isCreativeOpen.set(false)  // Ensure state consistency
            if (webView != null) {
                changeWebViewVisibility(false)
                webView!!.clearCache(true)
                if (triggerCallback != null) {
                    triggerCallback!!.onClose()
                }
            } else {
                Timber.w("The user closed the creative but the WebView is null. Ignoring.")
                if (triggerCallback != null) {
                    triggerCallback!!.onCreativeNotClosed()
                }
            }
        }
    }


        private fun changeWebViewVisibility(visible: Boolean) {
        if (webView != null) {
            if (visible) {
                webView!!.visibility = View.VISIBLE
            } else {
                webView!!.visibility = View.INVISIBLE
            }
        }
    }

    companion object {
        private val CREATIVE_LISTENER_ALLOWED_ORIGINS = setOf("https://creatives.attn.tv")
        private const val CREATIVE_LISTENER_JS = "javascript:(async function() {\n" +
                "    window.addEventListener('visibilitychange', \n" +
                "        function(event){\n" +
                "           CREATIVE_LISTENER.postMessage(`document-visibility: \${document.hidden}`);\n" +
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
                "})()"

        // Making this atomic to make sure it doesn't run into any race conditions
        private val isCreativeOpen = AtomicBoolean(false)
        private val isCreativeOpening = AtomicBoolean(false)
        private val isCreativeDestroyed = AtomicBoolean(false)

        internal fun isCreativeOpen(): Boolean{
            return isCreativeOpen.get()
        }

        internal fun isCreativeDestroyed(): Boolean{
            return isCreativeDestroyed.get()
        }

        internal fun isCreativeOpening(): Boolean{
            return isCreativeOpening.get()
        }
    }
}
