package com.attentive.androidsdk.creatives

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
    private var activity: Activity,
    @set:VisibleForTesting
    internal var webView: WebView? = null,
    @SuppressLint("SupportAnnotationUsage") @VisibleForTesting
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {
    /**
     * Creates a new Creative instance. Used to display and control creatives.
     * @param attentiveConfig The AttentiveConfig instance to use.
     * @param parentView The view to add the WebView to.
     * @param activity The Activity to use for lifecycle callbacks and WebView context.
     */
    constructor(
        attentiveConfig: AttentiveConfig,
        parentView: View,
        activity: Activity,
    ) : this(
        attentiveConfig,
        parentView,
        activity,
        null,
        Handler(Looper.getMainLooper()),
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

    // Bounding rectangle for touch event filtering (in pixels)
    private var creativeBounds: android.graphics.Rect? = null

    init {
        Timber.i(
            "Calling constructor of Creative with parameters: %s, %s, %s, %s, %s",
            attentiveConfig,
            parentView,
            activity,
            webView,
            handler,
        )
        Timber.i("parentView width = %s height = %s", parentView.width, parentView.height)
        Timber.i("Android version: %s", Build.VERSION.SDK_INT)

        // Log WebView provider info for diagnostics
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val webViewPackage = WebViewCompat.getCurrentWebViewPackage(activity)
            Timber.i("WebView provider: ${webViewPackage?.packageName} v${webViewPackage?.versionName}")
        }

        this.webViewClient = createWebViewClient()
        this.creativeListener = createCreativeListener()

        CoroutineScope(Dispatchers.Main).launch {
            if (webView == null) {
                Timber.d("Creating WebView on main thread")
                webView = createWebView(parentView)
                if (webView != null) {
                    addWebViewToParent()
                } else {
                    Timber.e("WebView creation failed - creative will not be available")
                }
            }
            // Only mark ready and process queue if webView exists
            if (webView != null) {
                isWebViewReady = true
                triggerQueue.forEach { it() }
                triggerQueue.clear()
            }
        }

        this.creativeUrlFormatter = CreativeUrlFormatter()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Timber.d("Registering activity lifecycle callbacks")
            // Delegate to CreativeActivityCallbacks to handle lifecycle events
            activity.registerActivityLifecycleCallbacks(CreativeActivityCallbacks(this))
        }
    }

    private fun addWebViewToParent() {
        changeWebViewVisibility(false)
        // Make WebView fullscreen - touch events will be filtered by bounding rect
        val width = parentView.width
        val height = parentView.height
        val layoutParams = ViewGroup.LayoutParams(width, height)
        webView?.let { view ->
            view.setBackgroundColor(Color.TRANSPARENT)
            Timber.d("Set webview background color to transparent")

            // Set up touch listener to filter events based on creative bounds
            // Suppress ClickableViewAccessibility: We're only filtering touches by bounds, not implementing
            // click logic. Touches inside bounds return false to let WebView handle them normally (including
            // accessibility), touches outside are intentionally blocked with no action needed.
            @SuppressLint("ClickableViewAccessibility")
            view.setOnTouchListener { _, event ->
                val bounds = creativeBounds

                // If no bounds set or creative not open, pass all touches through
                if (bounds == null || !isCreativeOpen.get()) {
                    Timber.i("No bounds or creative not open - passing touch through")
                    return@setOnTouchListener false
                }

                val x = event.x.toInt()
                val y = event.y.toInt()

                // Check if touch is within the creative's bounding rectangle
                val isInBounds = bounds.contains(x, y)

                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                    Timber.i("Touch at ($x, $y) - inBounds=$isInBounds (bounds=$bounds)")
                }

                if (isInBounds) {
                    // Touch is inside creative - let WebView handle it
                    false // Return false to let WebView's normal touch handling work
                } else {
                    // Touch is outside creative - consume it so it doesn't reach WebView
                    true
                }
            }
        }

        (parentView as ViewGroup).addView(webView, layoutParams)
    }

    /**
     * Triggers to show the creative.
     * @param callback [CreativeTriggerCallback] to be called when the creative updates it's state.
     * @param creativeId The creative ID to use. If not provided it will render the creative determined by online configuration.
     */
    fun trigger(
        callback: CreativeTriggerCallback? = null,
        creativeId: String? = null,
    ) {
        Timber.i("trigger method called with parameters - callback: %s, creativeId: %s", callback, creativeId)

        triggerCallback = callback

        if (!isWebViewReady) {
            Timber.i("WebView not ready yet, queueing trigger")
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

        Timber.i(
            "Attempting to trigger creative with attn domain %s, webview width %s, and webview height %s",
            attentiveConfig.domain,
            webView!!.width,
            webView!!.height,
        )

        val url = creativeUrlFormatter.buildCompanyCreativeUrl(attentiveConfig, creativeId)

        if (attentiveConfig.mode == AttentiveConfig.Mode.DEBUG) {
            changeWebViewVisibility(true)
        }

        if (isCreativeOpening.get()) {
            Timber.w("Attempted to trigger creative, but creative is already opening. Taking no action")
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
        Timber.i("Destroying creative")
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
        Timber.i("onBackPressed method called")
        Timber.i("isCreativeOpen.get() = %s", isCreativeOpen.get())
        if (isCreativeOpen.get()) {
            closeCreative()
            return true
        } else {
            return false
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    /***
     * @return a new WebView instance or null if WebView creation fails see https://issuetracker.google.com/issues/457969917
     */
    internal fun createWebView(parentView: View): WebView? {
        return try {
            val view = WebView(activity)
            val webSettings = view.settings

            // Security settings, allow JavaScript to run
            webSettings.allowFileAccessFromFileURLs = false
            webSettings.allowUniversalAccessFromFileURLs = false
            webSettings.allowFileAccess = false
            webSettings.javaScriptEnabled = true
            webSettings.domStorageEnabled = true

            view.webViewClient = webViewClient
            view.webChromeClient =
                object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                        Timber.d("onConsoleMessage + ${consoleMessage.message()}")
                        return true
                    }
                }

            // Add listener for creative OPEN / CLOSE events
            if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                Timber.d("Adding WebMessageListener")
                WebViewCompat.addWebMessageListener(
                    view,
                    "CREATIVE_LISTENER",
                    CREATIVE_LISTENER_ALLOWED_ORIGINS,
                    creativeListener,
                )
            } else {
                Timber.e("Creative listener cannot be attached!")
            }

            view.setBackgroundColor(Color.TRANSPARENT)

            view
        } catch (e: Exception) {
            Timber.e(
                e,
                "Failed to create WebView - current webview version may be broken. " +
                    "See: https://issuetracker.google.com/issues/457969917",
            )
            null
        }
    }

    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(
                view: WebView?,
                url: String?,
                favicon: Bitmap?,
            ) {
                super.onPageStarted(view, url, favicon)
                Timber.i("onPageStarted: %s %s", webView, url)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                Timber.e("onReceivedError: %s %s", webView, error)
                super.onReceivedError(view, request, error)
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?,
            ) {
                Timber.e("onReceivedHttpError: %s %s", webView, errorResponse)
                super.onReceivedHttpError(view, request, errorResponse)
            }

            override fun onPageFinished(
                view: WebView,
                url: String,
            ) {
                super.onPageFinished(view, url)

                Timber.i("onPageFinished: %s %s", webView, url)
                if (view.progress == 100) {
                    Timber.i("Page finished loading")
                    view.loadUrl(CREATIVE_LISTENER_JS)
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                uri: String,
            ): Boolean {
                Timber.i("shouldOverrideUrlLoading: %s %s", webView, uri)
                val lowercaseUri = uri.lowercase(Locale.getDefault())
                if (lowercaseUri.startsWith("sms://") || lowercaseUri.startsWith("http://") ||
                    lowercaseUri.startsWith(
                        "https://",
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
                            e.message,
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
        Timber.i("createCreativeListener() called")
        return WebMessageListener {
                view: WebView?,
                message: WebMessageCompat,
                sourceOrigin: Uri?,
                isMainFrame: Boolean,
                replyProxy: JavaScriptReplyProxy?,
            ->
            val messageData = message.data
            Timber.i("Creative message data %s", messageData)
            if (messageData != null) {
                // Try to parse as JSON for structured messages
                try {
                    if (messageData.startsWith("{")) {
                        val jsonObject = org.json.JSONObject(messageData)
                        val action = jsonObject.optString("action", "")

                        // Helper function to parse px values
                        fun parsePx(value: String?): Float? {
                            if (value == null) return null
                            val trimmed = value.trim()
                            if (!trimmed.endsWith("px")) return null
                            return trimmed.removeSuffix("px").toFloatOrNull()
                        }

                        when (action.uppercase()) {
                            "OPEN" -> {
                                Timber.i("Opening creative: %s", messageData)

                                val style = jsonObject.optJSONObject("style")
                                if (style != null) {
                                    val width = parsePx(style.optString("width"))
                                    val height = parsePx(style.optString("height"))
                                    val left = parsePx(style.optString("left"))
                                    val bottom = parsePx(style.optString("bottom"))

                                    if (width != null && height != null && width > 0 && height > 0 &&
                                        left != null && bottom != null && left >= 0 && bottom >= 0
                                    ) {
                                        val displayMetrics = activity.resources.displayMetrics
                                        val density = displayMetrics.density
                                        val parentHeight = parentView.height.toFloat()

                                        val widthPx = (width * density).toInt()
                                        val heightPx = (height * density).toInt()
                                        val leftPx = (left * density).toInt()
                                        val bottomPx = (bottom * density).toInt()
                                        val topPx = (parentHeight - bottomPx - heightPx).toInt()

                                        Timber.i(
                                            "OPEN - opening with dimensions width=$widthPx, height=$heightPx, left=$leftPx, top=$topPx (parentHeight=$parentHeight)",
                                        )
                                        openCreative(heightPx, widthPx, leftPx, topPx)
                                    } else {
                                        Timber.i("OPEN - invalid dimensions, using defaults")
                                        val displayMetrics = activity.resources.displayMetrics
                                        openCreative(displayMetrics.heightPixels, displayMetrics.widthPixels, 0, 0)
                                    }
                                } else {
                                    Timber.i("OPEN - no style, using defaults")
                                    val displayMetrics = activity.resources.displayMetrics
                                    openCreative(displayMetrics.heightPixels, displayMetrics.widthPixels, 0, 0)
                                }
                            }
                            "RESIZE_FRAME" -> {
                                Timber.d("Resize frame: %s", messageData)
                                // Ignore RESIZE_FRAME, we're using OPEN messages instead
                            }
                            "IMPRESSION" -> Timber.d("Impression: %s", messageData)
                            "CLOSE" -> closeCreative()
                            "TIMED OUT" -> onCreativeTimedOut()
                            else -> Timber.d("Unknown action: %s", action)
                        }
                        return@WebMessageListener
                    }
                } catch (e: Exception) {
                    Timber.e("Error parsing JSON message: %s", e.message)
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

    internal fun openCreative(
        height: Int,
        width: Int,
        left: Int = 0,
        top: Int = 0,
    ) {
        Timber.i("openCreative() called with height=$height, width=$width, left=$left, top=$top")
        CoroutineScope(Dispatchers.Main).launch {
            isCreativeOpening.set(false)

            // Host apps have reported webView NPEs here. The current thinking is that destroy gets
            // called just before this callback is executed. If destroy was previously called then it's
            // okay to ignore these callbacks since the host app has told us the creative should no longer
            // be displayed.
            if (webView != null) {
                // Set the bounding rectangle for touch event filtering
                // WebView stays fullscreen, but touches outside this rect will be ignored
                creativeBounds =
                    android.graphics.Rect(
                        left,
                        top,
                        left + width,
                        top + height,
                    )
                Timber.i("Set creative bounds: left=$left, top=$top, width=$width, height=$height (bounds=$creativeBounds)")

                // Make WebView visible
                changeWebViewVisibility(true)

                // Only fire callback once on first open
                if (!isCreativeOpen.getAndSet(true)) {
                    triggerCallback?.onOpen()
                }
                Timber.i("Creative opened with touch bounds set")
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
        Timber.i("closeCreative() called")
        CoroutineScope(Dispatchers.Main).launch {
            isCreativeOpen.set(false)
            isCreativeOpening.set(false)
            creativeBounds = null // Clear bounding rectangle so touches pass through
            Timber.i("Cleared creative bounds")
            if (webView != null) {
                changeWebViewVisibility(false)
                Timber.i("webview clearCache() called")
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
        Timber.i("changeWebViewVisibility() called, make visible: %s", visible)
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
        private const val CREATIVE_LISTENER_JS =
            "javascript:(async function() {\n" +
                "    window.addEventListener('visibilitychange', \n" +
                "        function(event){\n" +
                "           CREATIVE_LISTENER.postMessage(`document-visibility: \${document.hidden}`);\n" +
                "        },\n" +
                "    false);\n" +
                "    window.addEventListener('message',\n" +
                "        function(event){\n" +
                "            if (event.data && event.data.__attentive) {\n" +
                "                console.log(event.data.__attentive);\n " +
                "                CREATIVE_LISTENER.postMessage(JSON.stringify(event.data.__attentive));\n" +
                "            }\n" +
                "        },\n" +
                "    false);\n" +
                "    var timeoutHandle = null;\n" +
                "    const interval = setInterval(function() {\n" +
                "        e =document.querySelector('iframe');\n" +
                "        if(e && e.id === 'attentive_creative') {\n" +
                "           clearInterval(interval);\n" +
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
