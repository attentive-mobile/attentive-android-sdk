package com.attentive.androidsdk.push

import com.attentive.androidsdk.PersistentStorage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull

class TokenProviderTest {
    private lateinit var storage: PersistentStorage

    @Before
    fun setUp() {
        storage = mock(PersistentStorage::class.java)
        TokenProvider.getInstance().bindStorage(null)
        TokenProvider.getInstance().token = null
    }

    @After
    fun tearDown() {
        // TokenProvider is a process-wide singleton: detach so later test classes start clean.
        TokenProvider.getInstance().bindStorage(null)
        TokenProvider.getInstance().token = null
    }

    @Test
    fun bindStorage_restoresPersistedToken() {
        // MSDK-586: a warm launch has to start with a token rather than racing Firebase for one
        // all over again, otherwise every launch defers its first inbox request.
        Mockito.doReturn(PERSISTED_TOKEN).`when`(storage).read(TokenProvider.PUSH_TOKEN_KEY)

        TokenProvider.getInstance().bindStorage(storage)

        assertEquals(PERSISTED_TOKEN, TokenProvider.getInstance().token)
    }

    @Test
    fun bindStorage_keepsInMemoryToken() {
        // The in-memory value is the fast path and can be fresher than the stored copy, so
        // binding must not overwrite it.
        Mockito.doReturn(PERSISTED_TOKEN).`when`(storage).read(anyOrNull())
        TokenProvider.getInstance().token = LIVE_TOKEN

        TokenProvider.getInstance().bindStorage(storage)

        assertEquals(LIVE_TOKEN, TokenProvider.getInstance().token)
    }

    @Test
    fun token_isWrittenThroughToBoundStorage() {
        // The half the next launch depends on: a token fetched now must survive the process.
        TokenProvider.getInstance().bindStorage(storage)

        TokenProvider.getInstance().token = LIVE_TOKEN

        verify(storage).save(TokenProvider.PUSH_TOKEN_KEY, LIVE_TOKEN)
    }

    @Test
    fun token_isNotPersisted_whenNoStorageBound() {
        // Push-disabled apps bind no storage; a token must not reach disk for them.
        TokenProvider.getInstance().bindStorage(storage)
        TokenProvider.getInstance().bindStorage(null)

        TokenProvider.getInstance().token = LIVE_TOKEN

        verify(storage, never()).save(anyOrNull<String>(), any<String>())
        assertEquals(LIVE_TOKEN, TokenProvider.getInstance().token)
    }

    companion object {
        private const val PERSISTED_TOKEN = "persistedTokenValue"
        private const val LIVE_TOKEN = "liveTokenValue"
    }
}
