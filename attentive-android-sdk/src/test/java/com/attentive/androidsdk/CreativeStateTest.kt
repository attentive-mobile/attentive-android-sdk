package com.attentive.androidsdk.creatives

import android.app.Activity
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.webkit.WebSettings
import android.webkit.WebView
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.TimberRule
import com.attentive.androidsdk.internal.util.CreativeUrlFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner.Silent::class)
class CreativeStateTest {

    private lateinit var parentView: View
    private lateinit var webView: WebView
    private lateinit var creative: Creative
    private val testDispatcher = StandardTestDispatcher()
    private val handler: Handler = mock()

    companion object {
        @get:ClassRule
        @JvmStatic
        var timberRule = TimberRule()
    }

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher) // Set the test dispatcher

        val webSettings = mock<WebSettings>{}

        webView = mock<WebView> {
            on { settings } doReturn webSettings
        }

        parentView = mock<ViewGroup>{}
        whenever(parentView.viewTreeObserver).thenReturn(mock())


        whenever(handler.post(any())).thenAnswer { invocation ->
            val msg = invocation.getArgument<Runnable>(0)
            msg.run()
            null
        }

        val realCreative = createRealCreative(parentView, webView)
        creative = spy(realCreative)


        doReturn(webView).whenever(creative).createWebView(any())
    }

    fun createRealCreative(parentView: View, webViewParent: ViewParent): Creative{
        val realCreative = Creative(mock<AttentiveConfig>{}, parentView, mock<Activity>{}, webView, handler)
        realCreative.webView = webView
        realCreative.creativeUrlFormatter = mock<CreativeUrlFormatter>{
            doReturn("https://example.com").whenever(it).buildCompanyCreativeUrl(any(), anyOrNull())
        }
        return realCreative
    }

    @After
    fun tearDown() {
        creative.isCreativeOpening.set(false)
        creative.isCreativeDestroyed.set(false)
        creative.isCreativeOpen.set(false)
        Dispatchers.resetMain() // Reset the main dispatcher
    }

    @Test
    fun testCreativeIsInitiallyClosed() {
        assertFalse(creative.isCreativeOpen.get())
        assertFalse(creative.isCreativeOpening.get())
        assertFalse(creative.isCreativeDestroyed.get())
    }

    @Test
    fun testCreativeStartsOpeningCorrectly() {
        creative.isWebViewReady = true
        creative.trigger()
        assertTrue(creative.isCreativeOpening.get())
        assertFalse(creative.isCreativeOpen.get())
        assertFalse(creative.isCreativeDestroyed.get())
    }

    @Test
    fun testCreativeOpensCorrectly() {
        creative.isWebViewReady = true
        creative.openCreative()
        testDispatcher.scheduler.runCurrent() // Advance the dispatcher to execute pending coroutines
        assertTrue(creative.isCreativeOpen.get())
        assertFalse(creative.isCreativeOpening.get())
        assertFalse(creative.isCreativeDestroyed.get())
    }

    @Test
    fun testCreativeClosesCorrectly() {
        creative.trigger()
        creative.closeCreative()
        assertFalse(creative.isCreativeOpen.get())
        assertFalse(creative.isCreativeOpening.get())
        assertFalse(creative.isCreativeDestroyed.get())
    }

    @Test
    fun testCreativeDestroysCorrectly() {
        creative.destroy()
        assertFalse(creative.isCreativeOpen.get())
        assertFalse(creative.isCreativeOpening.get())
        assertTrue(creative.isCreativeDestroyed.get())
    }

    @Test
    fun testNewCreativeCanBeOpenedAfterPreviousCreativeDestroyed(){
        creative.openCreative()
        creative.destroy()
        creative = spy(createRealCreative(parentView, webView ))
        creative.openCreative()
        testDispatcher.scheduler.runCurrent() // Advance the dispatcher to execute pending coroutines
        assertTrue(creative.isCreativeOpen.get())
    }
}