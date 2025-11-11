import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import timber.log.Timber

internal class PassThroughWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private var overlayFullscreen = false
    private val handler = Handler(Looper.getMainLooper())
    private val bubbleRect = Rect(0, 0, 0, 0)

    init {
        settings.javaScriptEnabled = true
        addJavascriptInterface(OverlayStateBridge(), "AndroidOverlayBridge")
        isFocusable = true
        isFocusableInTouchMode = true
    }


    internal fun injectStateWatcher() {
        val js = """
            (() => {
                function getSmallOverlayRect() {
                    var overlay = document.getElementById('attentive_overlay');
                    if (!overlay) return null;
                    var iframe = overlay.querySelector('iframe');
                    if (!iframe) return null;
                    var rect = iframe.getBoundingClientRect();
                    return { left: rect.left, top: rect.top, width: rect.width, height: rect.height };
                }
                function isFullScreenOverlay() {
                    var overlay = document.getElementById('attentive_overlay');
                    if (!overlay) return false;
                    var iframe = overlay.querySelector('iframe');
                    if (!iframe) return false;
                    var w = iframe.style.width, h = iframe.style.height;
                    if ((w === '100%' || w === '100vw') && (h === '100%' || h === '100vh')) return true;
                    var rect = iframe.getBoundingClientRect();
                    if (rect.width > window.innerWidth * 0.95 && rect.height > window.innerHeight * 0.95) return true;
                    return false;
                }
                function notify() {
                    var full = isFullScreenOverlay();
                    if (full) {
                        window.AndroidOverlayBridge.onOverlayStateChanged("fullscreen", 0, 0, 0, 0);
                    } else {
                        var r = getSmallOverlayRect();
                        if (r) {
                            window.AndroidOverlayBridge.onOverlayStateChanged("small", r.left, r.top, r.width, r.height);
                        }
                    }
                }
                notify();
                var called = false;
                var observer = new MutationObserver(function() {
                    if (called) return;
                    called = true;
                    setTimeout(() => { notify(); called = false; }, 300);
                });
                observer.observe(document.body, { attributes:true, subtree:true, childList:true, attributeFilter:['style'] });
            })();
        """.trimIndent()
        evaluateJavascript(js, null)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    /***
     * Expose for automated testing frameworks.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (overlayFullscreen) {
            Timber.d("Overlay is fullscreen, passing touch event to WebView")
            super.onTouchEvent(event)
        } else {
            val x = event.x.toInt()
            val y = event.y.toInt()
            Timber.d("Overlay is not fullscreen, checking if touch event is in small content rect x: $x, y: $y, rect: $bubbleRect")
            if (bubbleRect.contains(x, y)) {
                Timber.d("Touch event is in small content rect, passing to WebView")
                super.onTouchEvent(event)
            } else {
                Timber.d("Touch event is not in small content rect, returning false")
                false
            }
        }
    }

    inner class OverlayStateBridge {
        @JavascriptInterface
        fun onOverlayStateChanged(
            state: String,
            left: Float,
            top: Float,
            width: Float,
            height: Float
        ) {
            Timber.d("Overlay state changed: state=$state, left=$left, top=$top, width=$width, height=$height")
            overlayFullscreen = state == "fullscreen"
            if (!overlayFullscreen && width > 0 && height > 0) {
                // WebView and JS use different coordinate spaces
                handler.post {
                    // Convert JS (CSS pixels) to Android (device pixels)
                    bubbleRect.set(
                        (left * scale).toInt(),
                        (top * scale).toInt(),
                        ((left + width) * scale).toInt(),
                        ((top + height) * scale).toInt()
                    )
                }
            }
        }
    }
}