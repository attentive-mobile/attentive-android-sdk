package com.attentive.androidsdk.push

import android.content.Context
import com.attentive.androidsdk.FactoryMocks
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.anyOrNull

class TokenProviderTest {
    private lateinit var factoryMocks: FactoryMocks
    private lateinit var context: Context

    @Before
    fun setUp() {
        factoryMocks = FactoryMocks.mockFactoryObjects()
        context = mock(Context::class.java)
        TokenProvider.getInstance().token = null
    }

    @After
    fun tearDown() {
        TokenProvider.getInstance().token = null
        factoryMocks.close()
    }

    @Test
    fun loadPersistedToken_restoresTokenFromStorage() {
        // MSDK-586: a warm launch has to start with a token rather than racing Firebase for one
        // all over again, otherwise every launch defers its first inbox request.
        Mockito.doReturn(PERSISTED_TOKEN)
            .`when`(factoryMocks.persistentStorage).read(TokenProvider.PUSH_TOKEN_KEY)

        TokenProvider.getInstance().loadPersistedToken(context)

        assertEquals(PERSISTED_TOKEN, TokenProvider.getInstance().token)
    }

    @Test
    fun loadPersistedToken_keepsInMemoryToken() {
        // The in-memory value is the fast path and can be fresher than the stored copy, so
        // hydration must not overwrite it.
        Mockito.doReturn(PERSISTED_TOKEN)
            .`when`(factoryMocks.persistentStorage).read(anyOrNull())
        TokenProvider.getInstance().token = LIVE_TOKEN

        TokenProvider.getInstance().loadPersistedToken(context)

        assertEquals(LIVE_TOKEN, TokenProvider.getInstance().token)
    }

    companion object {
        private const val PERSISTED_TOKEN = "persistedTokenValue"
        private const val LIVE_TOKEN = "liveTokenValue"
    }
}
