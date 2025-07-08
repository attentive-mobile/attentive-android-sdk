package com.attentive.androidsdk.creatives

import PassThroughWebView
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewTreeObserver
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.VisibleForTesting
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewCompat.WebMessageListener
import androidx.webkit.WebViewFeature
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.internal.util.CreativeUrlFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class Creative internal constructor(
    private var attentiveConfig: AttentiveConfig,
    private var parentView: View,
    private var activity: Activity? = null,
    @set:VisibleForTesting
    internal var webView: PassThroughWebView? = null,
    @SuppressLint("SupportAnnotationUsage") @VisibleForTesting
    private val handler: Handler = Handler(Looper.getMainLooper())
) {
    /**
     * Creates a new Creative instance. Used to display and control creatives.
     * @param attentiveConfig The AttentiveConfig instance to use.
     * @param parentView The view to add the WebView to.
     * @param activity The Activity to use for lifecycle callbacks.
     */
    constructor(
        attentiveConfig: AttentiveConfig,
        parentView: View,
        activity: Activity? = null
    ) : this(
        attentiveConfig,
        parentView,
        activity,
        null,
        Handler(Looper.getMainLooper())
    )

    @VisibleForTesting
    internal var creativeUrlFormatter: CreativeUrlFormatter
    private val webViewClient: WebViewClient
    private val creativeListener: WebMessageListener
    private var triggerCallback: CreativeTriggerCallback? = null

    private val triggerQueue = mutableListOf<() -> Unit>()
    @VisibleForTesting
    internal var isWebViewReady = false

    // Making this atomic to make sure it doesn't run into any race conditions
    internal val isCreativeOpen = AtomicBoolean(false)
    internal val isCreativeOpening = AtomicBoolean(false)
    internal val isCreativeDestroyed = AtomicBoolean(false)


    init {
        Timber.d(
            "Calling constructor of Creative with parameters: %s, %s, %s, %s, %s",
            attentiveConfig,
            parentView,
            activity,
            webView,
            handler
        )
        Timber.d("parentView class name = %s", parentView.javaClass.name)
        Timber.d("parentView type = %s", parentView::class.java)
        Timber.d("parentView width = %s", parentView.width)
        Timber.d("parentView height = %s", parentView.height)
        Timber.d("Android version: %s", Build.VERSION.SDK_INT)
        this.webViewClient = createWebViewClient()
        this.creativeListener = createCreativeListener()


        CoroutineScope(Dispatchers.Main).launch {
            if (webView == null) {
                Timber.d("Creating WebView on main thread")
                webView = createWebView(parentView)
                addWebViewToParent()
            }
            isWebViewReady = true
            triggerQueue.forEach { it() }
            triggerQueue.clear()
        }

        this.creativeUrlFormatter = CreativeUrlFormatter()

        activity?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Timber.d("Registering activity lifecycle callbacks")
                // Delegate to CreativeActivityCallbacks to handle lifecycle events
                it.registerActivityLifecycleCallbacks(CreativeActivityCallbacks(this))
            }
        }
    }

    private fun addWebViewToParent() {
        changeWebViewVisibility(false)
        val width = parentView.width
        val height = parentView.height
        val layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT)
        webView?.let {
            it.setBackgroundColor(Color.TRANSPARENT)
            Timber.d("Set webview background color to transparent")
        }

        (parentView as ViewGroup).addView(webView, layoutParams)
    }

    /**
     * Triggers to show the creative.
     * @param callback [CreativeTriggerCallback] to be called when the creative updates it's state.
     * @param creativeId The creative ID to use. If not provided it will render the creative determined by online configuration.
     */
    fun trigger(callback: CreativeTriggerCallback? = null, creativeId: String? = null) {
        Timber.d("trigger method called with parameters: %s, %s", callback, creativeId)

        triggerCallback = callback

        if (!isWebViewReady) {
            Timber.d("WebView not ready yet, queueing trigger")
            triggerQueue.add { trigger(callback, creativeId) }
            return
        }

        if (isCreativeDestroyed.get()) {
            Timber.e("Attempted to trigger a destroyed creative. Ignoring.")
            triggerCallback?.onCreativeNotOpened()
            return
        }

        if (webView == null) {
            Timber.e("WebView not properly created or `destroy` already called on this Creative. Cannot trigger Creative after destroyed.")
            triggerCallback?.onCreativeNotOpened()
            return
        }

        Timber.d(
            "Attempting to trigger creative with attn domain %s, webview width %s, and webview height %s",
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

        Timber.d("Start loading creative with url %s", url)
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
        Timber.d("Destroying creative")
        isCreativeOpen.set(false)
        isCreativeOpening.set(false)
        if (parentView != null && webView != null) {
            Timber.d("WebView removed from view hierarchy correctly")
            (parentView as ViewGroup).removeView(webView)
        }
        // TODO: better thread-safety when destroying. Lock?
        if (webView != null) {
            // set the webView member variable to null BEFORE we destroy it so other code on other threads that check if
            // webView isn't null doesn't try to use it after it is destroyed
            val webViewToDestroy = webView
            webView = null
            webViewToDestroy?.destroy()
            Timber.d("WebView destroyed correctly")
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
    internal fun createWebView(parentView: View): PassThroughWebView {
        val view = PassThroughWebView(parentView.context)
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
                Timber.d("onConsoleMessage + ${consoleMessage.message()}")
                return true
            }
        }

        // Add listener for creative OPEN / CLOSE events
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            Timber.d("Adding WebMessageListener")
            WebViewCompat.addWebMessageListener(
                view, "CREATIVE_LISTENER", CREATIVE_LISTENER_ALLOWED_ORIGINS, creativeListener
            )
        } else {
            Timber.e("Creative listener cannot be attached!")
        }

        if (attentiveConfig.mode == AttentiveConfig.Mode.PRODUCTION) {
            Timber.d("Setting background color to transparent")
            view.setBackgroundColor(Color.TRANSPARENT)
        }
        return view
    }

    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Timber.d("onPageStarted: %s %s", webView, url)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                Timber.e("onReceivedError: %s %s", webView, error)
                super.onReceivedError(view, request, error)
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                Timber.e("onReceivedHttpError: %s %s", webView, errorResponse)
                super.onReceivedHttpError(view, request, errorResponse)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                Timber.d("onPageFinished: %s %s", webView, url)
                if (view.progress == 100) {
                    Timber.d("Page finished loading")
                    view.loadUrl(CREATIVE_LISTENER_JS)
                    webView?.injectStateWatcher()
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, uri: String): Boolean {
                Timber.d("shouldOverrideUrlLoading: %s %s", webView, uri)
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
        Timber.d("createCreativeListener() called")
        return WebMessageListener { view: WebView?, message: WebMessageCompat, sourceOrigin: Uri?, isMainFrame: Boolean, replyProxy: JavaScriptReplyProxy? ->
            val messageData = message.data
            Timber.d("Creative message data %s", messageData)
            if (messageData != null) {
                Timber.d(
                    "messageData is not null message:%s message:%s %s %s",
                    messageData,
                    message,
                    sourceOrigin,
                    isMainFrame
                )
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
                    Timber.d("document-visibility: true and creative is open, closing creative")
                    closeCreative()
                }
            }
        }
    }

    private fun onCreativeTimedOut() {
        Timber.e("Creative timed out. Not showing WebView.")
        triggerCallback?.let {
            Timber.e("Trigger callback is not null")
            it.onCreativeNotOpened()
        }
        isCreativeOpen.set(false)
    }

    internal fun openCreative() {
        Timber.d("openCreative() called")
        CoroutineScope(Dispatchers.Main).launch {
            Timber.d("handler post")
            isCreativeOpening.set(false)
            if (isCreativeOpen.get()) {
                Timber.w("Attempted to trigger creative, but creative is currently open. Taking no action")
                return@launch
            }
            // Host apps have reported webView NPEs here. The current thinking is that destroy gets
            // called just before this callback is executed. If destroy was previously called then it's
            // okay to ignore these callbacks since the host app has told us the creative should no longer
            // be displayed.
            if (webView != null) {
                changeWebViewVisibility(true)
                Timber.d("WebView requestLayout()")
                webView!!.requestLayout()
                isCreativeOpening.set(false)
                isCreativeOpen.set(true)
                if (triggerCallback != null) {
                    triggerCallback!!.onOpen()
                }
                Timber.d("WebView correctly displayed")
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
        Timber.d("closeCreative() called")
        CoroutineScope(Dispatchers.Main).launch {
            Timber.d("handler post")
            isCreativeOpen.set(false)
            isCreativeOpening.set(false)
            if (webView != null) {
                changeWebViewVisibility(false)
                Timber.d("clearCache() called")
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
        Timber.d("changeWebViewVisibility() called with parameter %s", visible)
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


    }
}