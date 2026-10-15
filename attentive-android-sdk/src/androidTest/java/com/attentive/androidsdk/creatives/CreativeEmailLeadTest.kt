package com.attentive.androidsdk.creatives

import android.app.Application
import android.os.SystemClock
import android.webkit.WebView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Sends `EMAIL_LEAD` messages through a real [Creative] WebView: page `postMessage` -> SDK-injected
 * forwarder -> `CREATIVE_LISTENER` -> [Creative]'s listener -> [AttentiveSdk.creativeEmailLeads].
 */
@RunWith(AndroidJUnit4::class)
class CreativeEmailLeadTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(CreativeKeyboardTestActivity::class.java)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var activity: CreativeKeyboardTestActivity
    private lateinit var creative: Creative
    private lateinit var webView: WebView
    private val leads: MutableList<CreativeEmailLead> = Collections.synchronizedList(mutableListOf())
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    @Before
    fun setUp() {
        activityRule.scenario.onActivity { activity = it }
        onMain {
            val config =
                AttentiveConfig.Builder()
                    .applicationContext(activity.applicationContext as Application)
                    .mode(AttentiveConfig.Mode.DEBUG)
                    .domain("games")
                    .build()
            creative = Creative(config, activity.creativeParent, activity)
        }
        assertTrue("Creative WebView was never created", waitFor(10_000) { onMain { creative.isWebViewReady } })
        webView = onMain { requireNotNull(creative.webView) }

        val subscribed = CountDownLatch(1)
        scope.launch {
            AttentiveSdk.creativeEmailLeads
                .onSubscription { subscribed.countDown() }
                .collect { leads += it }
        }
        assertTrue(subscribed.await(2, TimeUnit.SECONDS))

        onMain { webView.loadDataWithBaseURL(CREATIVES_URL, PAGE, "text/html", "utf-8", null) }
        assertTrue("page did not load", waitFor(10_000) { eval("document.readyState") == "\"complete\"" })
        awaitForwarderInjected()
    }

    @After
    fun tearDown() {
        scope.cancel()
        onMain { creative.destroy() }
    }

    @Test
    fun creativeIframe_emailLead_isEmittedWithoutForwardedOrigin() {
        postFromCreativeIframe(
            "{action:'EMAIL_LEAD', email:'user@example.com', creativeId:'1162985', visitorId:'v-123', metadata:{source:'app'}}",
        )

        val lead = awaitLead { it.email == "user@example.com" }
        val raw = JSONObject(lead.rawPayload)
        assertEquals("1162985", raw.getString("creativeId"))
        assertEquals("v-123", raw.getString("visitorId"))
        assertFalse(raw.has(Creative.MESSAGE_ORIGIN_KEY))
    }

    @Test
    fun emailLeadWithoutEmail_isEmittedWithNullEmail() {
        postFromTop("{action:'EMAIL_LEAD', email: undefined, visitorId:'prefill'}")

        assertNull(awaitLead { it.rawPayload.contains("\"visitorId\":\"prefill\"") }.email)
    }

    @Test
    fun leadWithNewEmail_isEmitted() {
        postFromTop("{action:'LEAD', email:'twoinone@example.com', phone:'+15555550100', userEmail:'known@example.com'}")

        val lead = awaitLead { it.email == "twoinone@example.com" }
        assertEquals("LEAD", JSONObject(lead.rawPayload).getString("action"))
    }

    @Test
    fun leadRepeatingAnEmittedOrKnownEmail_isNotEmitted() {
        postFromTop("{action:'EMAIL_LEAD', email:'first@example.com'}")
        postFromTop("{action:'LEAD', email:'First@Example.com', phone:'+15555550100'}")
        postFromTop("{action:'LEAD', email:'known@example.com', userEmail:'known@example.com'}")
        postFromTop("{action:'LEAD', phone:'+15555550100'}")
        postFromTop("{action:'EMAIL_LEAD', email:'sentinel@example.com'}")

        awaitLead { it.email == "sentinel@example.com" }
        assertEquals(listOf("first@example.com", "sentinel@example.com"), leads.map { it.email })
    }

    @Test
    fun otherActions_areNotEmitted() {
        postFromTop("{action:'IMPRESSION', email:'impression@example.com'}")
        postFromTop("{action:'EMAIL_LEAD', email:'sentinel@example.com'}")

        awaitLead { it.email == "sentinel@example.com" }
        assertEquals(listOf("sentinel@example.com"), leads.map { it.email })
    }

    @Test
    fun untrustedChildFrame_emailLead_isNotEmitted() {
        onMain {
            webView.evaluateJavascript(
                "var f = document.createElement('iframe');" +
                    "f.setAttribute('sandbox', 'allow-scripts');" +
                    "f.srcdoc = \"<script>window.top.postMessage({__attentive:{action:'EMAIL_LEAD', email:'forged@example.com', " +
                    "__origin:'https://creatives.attn.tv'}}, '*');<\\/script>\";" +
                    "document.body.appendChild(f);",
                null,
            )
        }
        // Wait out the forged post, then confirm a trusted lead still arrives after it.
        SystemClock.sleep(1_500)
        postFromTop("{action:'EMAIL_LEAD', email:'sentinel@example.com'}")

        awaitLead { it.email == "sentinel@example.com" }
        assertEquals(listOf("sentinel@example.com"), leads.map { it.email })
    }

    /** The SDK injects its forwarder in onPageFinished; probe until a lead round-trips. */
    private fun awaitForwarderInjected() {
        val injected =
            waitFor(8_000) {
                postFromTop("{action:'EMAIL_LEAD', email:'probe@example.com'}")
                SystemClock.sleep(200)
                leads.any { it.email == "probe@example.com" }
            }
        assertTrue("SDK forwarder JS was never injected", injected)
        leads.clear()
    }

    private fun postFromTop(objectLiteral: String) {
        onMain { webView.evaluateJavascript("window.postMessage({__attentive: $objectLiteral}, '*')", null) }
    }

    private fun postFromCreativeIframe(objectLiteral: String) {
        onMain {
            webView.evaluateJavascript(
                "document.getElementById('attentive_creative').contentWindow" +
                    ".eval(\"window.parent.postMessage({__attentive: $objectLiteral}, '*')\")",
                null,
            )
        }
    }

    private fun awaitLead(match: (CreativeEmailLead) -> Boolean): CreativeEmailLead {
        assertTrue(
            "expected lead not emitted; got ${leads.map { it.email }}",
            waitFor(5_000) { synchronized(leads) { leads.any(match) } },
        )
        return synchronized(leads) { leads.first(match) }
    }

    private fun eval(js: String): String? {
        val latch = CountDownLatch(1)
        var value: String? = null
        onMain {
            webView.evaluateJavascript(js) {
                value = it
                latch.countDown()
            }
        }
        latch.await(3, TimeUnit.SECONDS)
        return value
    }

    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
    }

    private fun waitFor(
        timeoutMs: Long,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return true
            SystemClock.sleep(100)
        }
        return condition()
    }

    private companion object {
        const val CREATIVES_URL = "https://creatives.attn.tv/mobile-apps/index.html"

        // iframe#attentive_creative stops the SDK's 5s "TIMED OUT" reset. Its srcdoc document
        // shares the page's https://creatives.attn.tv origin, like the real creative frame.
        const val PAGE =
            "<html><body><iframe id=\"attentive_creative\" srcdoc=\"<html><body>creative</body></html>\"></iframe></body></html>"
    }
}
