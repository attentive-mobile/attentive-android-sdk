package com.attentive.androidsdk.creatives

import android.view.View
import android.webkit.WebView
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever

class CreativeStateTest {

    @Mock
    private lateinit var parentView: View

    @Mock
    private lateinit var webView: WebView

    @Mock
    private lateinit var creative: Creative

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(parentView.context).thenReturn(mock(android.content.Context::class.java))
        whenever(creative.createWebView(any())).thenReturn(webView)
        creative = mock(Creative::class.java)
        whenever(creative.destroy()).thenCallRealMethod()
        whenever(creative.trigger()).thenCallRealMethod()
        whenever(creative.trigger(anyOrNull(), anyOrNull())).thenCallRealMethod()
    }

    @Test
    fun testCreativeIsInitiallyClosed() {
        assertFalse(Creative.isCreativeOpen())
        assertFalse(Creative.isCreativeOpen())
        assertFalse(Creative.isCreativeOpening())
        assertFalse(Creative.isCreativeDestroyed())
    }

    @Test
    fun testCreativeOpensCorrectly() {
        creative.trigger()
        assertFalse(Creative.isCreativeOpen())
        assertFalse(Creative.isCreativeDestroyed())
    }

    @Test
    fun testCreativeClosesCorrectly() {
        creative.trigger()
        creative.closeCreative()
        assertFalse(Creative.isCreativeOpen())
        assertFalse(Creative.isCreativeOpening())
        assertFalse(Creative.isCreativeDestroyed())
    }

    @Test
    fun testCreativeDestroysCorrectly() {
        creative.destroy()
        assertFalse(Creative.isCreativeOpen())
        assertFalse(Creative.isCreativeOpening())
        assertTrue(Creative.isCreativeDestroyed())
    }

}