package com.attentive.androidsdk.push

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TokenProviderTest {
    @Before
    fun setUp() {
        TokenProvider.getInstance().token = null
    }

    @After
    fun tearDown() {
        // TokenProvider is a singleton; reset so later test classes start clean.
        TokenProvider.getInstance().token = null
    }

    @Test
    fun token_isPublishedOnTokenFlow() {
        TokenProvider.getInstance().token = LIVE_TOKEN

        assertEquals(LIVE_TOKEN, TokenProvider.getInstance().tokenFlow.value)
    }

    @Test
    fun tokenFlow_isNull_whenTokenCleared() {
        TokenProvider.getInstance().token = LIVE_TOKEN

        TokenProvider.getInstance().token = null

        assertNull(TokenProvider.getInstance().tokenFlow.value)
    }

    companion object {
        private const val LIVE_TOKEN = "liveTokenValue"
    }
}
