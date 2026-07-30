package com.attentive.androidsdk

import android.app.Application
import android.content.Context
import com.attentive.androidsdk.internal.util.AppInfo
import com.attentive.androidsdk.internal.util.AppInfo.isDebuggable
import com.attentive.androidsdk.inbox.InboxState
import com.attentive.androidsdk.inbox.Style
import com.attentive.androidsdk.internal.network.DeleteMessageRequest
import com.attentive.androidsdk.internal.network.DeleteMessageResponse
import com.attentive.androidsdk.internal.network.InboxMessageDto
import com.attentive.androidsdk.internal.network.InboxResponse
import com.attentive.androidsdk.internal.network.MarkMessagesReadEntry
import com.attentive.androidsdk.internal.network.MarkMessagesReadRequest
import com.attentive.androidsdk.internal.network.MarkMessagesReadResponse
import com.attentive.androidsdk.internal.network.RetrofitInboxApiService
import com.attentive.androidsdk.internal.network.TrackClickRequest
import com.attentive.androidsdk.internal.network.UnreadCountRequest
import com.attentive.androidsdk.internal.network.UnreadCountResponse
import kotlinx.coroutines.flow.MutableStateFlow
import retrofit2.Response
import com.attentive.androidsdk.internal.util.Constants
import com.attentive.androidsdk.push.TokenProvider
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.whenever

class AttentiveSdkTest {
    private lateinit var remoteMessage: RemoteMessage
    private lateinit var application: Application
    private lateinit var context: Context
    private lateinit var callback: AttentiveSdk.PushTokenCallback
    private lateinit var factoryMocks: FactoryMocks
    private var mockedAppInfo: MockedStatic<AppInfo>? = null
    @Before
    fun setUp() {
        remoteMessage = mock(RemoteMessage::class.java)
        application = mock(Application::class.java)
        context = mock(Context::class.java)
        callback = mock(AttentiveSdk.PushTokenCallback::class.java)

        factoryMocks = FactoryMocks.mockFactoryObjects()
        Mockito.doReturn(VISITOR_ID).`when`(factoryMocks.visitorService).visitorId
        Mockito.doReturn(NEW_VISITOR_ID).`when`(factoryMocks.visitorService).createNewVisitorId()

        mockedAppInfo = Mockito.mockStatic(AppInfo::class.java)
        Mockito.`when`(isDebuggable(any())).thenReturn(false)

        // Set push token on the real TokenProvider singleton
        TokenProvider.getInstance().token = PUSH_TOKEN

        val config = AttentiveConfig.Builder()
            .domain(DOMAIN)
            .mode(AttentiveConfig.Mode.DEBUG)
            .applicationContext(mock(Application::class.java))
            .build()

        // Set _config directly to avoid AttentiveEventTracker.initialize which requires main looper
        val field = AttentiveSdk::class.java.getDeclaredField("_config")
        field.isAccessible = true
        field.set(AttentiveSdk, config)
    }

    @After
    fun tearDown() {
        factoryMocks.close()
        mockedAppInfo?.close()
        TokenProvider.getInstance().token = null
        setInboxApi(null)
        setInboxState(InboxState())
        setNextPageToken(null)
    }

    private fun setInboxApi(api: RetrofitInboxApiService?) {
        val field = AttentiveSdk::class.java.getDeclaredField("inboxApi")
        field.isAccessible = true
        field.set(AttentiveSdk, api)
    }

    private fun setInboxState(state: InboxState) {
        val field = AttentiveSdk::class.java.getDeclaredField("_inboxState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(AttentiveSdk) as MutableStateFlow<InboxState>).value = state
    }

    @Test
    fun isAttentiveFirebaseMessage_returnsTrue_whenAttentiveKeyPresent() {
        whenever(remoteMessage.data).thenReturn(mapOf(Constants.Companion.KEY_NOTIFICATION_TITLE to "Test"))
        assertTrue(AttentiveSdk.isAttentiveFirebaseMessage(remoteMessage))
    }

    @Test
    fun isAttentiveFirebaseMessage_returnsFalse_whenNoAttentiveKey() {
        whenever(remoteMessage.data).thenReturn(mapOf("other_key" to "Test"))
        assertFalse(AttentiveSdk.isAttentiveFirebaseMessage(remoteMessage))
    }

    @Test
    fun clearUser_callsSendUserUpdateWithNullIdentifiers() {
        AttentiveSdk.clearUser()

        // sendUserUpdate is launched on Dispatchers.IO; give it time to execute
        Thread.sleep(100)

        runBlocking {
            verify(factoryMocks.attentiveApi).sendUserUpdate(
                eq(DOMAIN), isNull(), isNull(), eq(NEW_VISITOR_ID), any(), any()
            )
        }
    }

    @Test
    fun clearUser_resetsIdentifiers() {
        AttentiveSdk.clearUser()

        verify(factoryMocks.visitorService).createNewVisitorId()
    }

    @Test
    fun updateUser_withWhitespaceOnlyEmail_doesNotCallSendUserUpdate() {
        AttentiveSdk.updateUser(email = "   ")

        runBlocking {
            verify(factoryMocks.attentiveApi, never()).sendUserUpdate(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun updateUser_withBothNullParams_doesNotCallSendUserUpdate() {
        AttentiveSdk.updateUser(email = null, phoneNumber = null)

        runBlocking {
            verify(factoryMocks.attentiveApi, never()).sendUserUpdate(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun refreshInbox_populatesInboxStateFromServer() {
        val inboxApi = mock(RetrofitInboxApiService::class.java)
        runBlocking {
            whenever(inboxApi.getMessages(any(), any())).thenReturn(
                InboxResponse(
                    messages = listOf(
                        InboxMessageDto(inboxMessageId = "m1", title = "hi", isRead = false),
                        InboxMessageDto(inboxMessageId = "m2", title = "there", isRead = true),
                    ),
                    nextPageToken = "next",
                ),
            )
            whenever(inboxApi.getUnreadCount(any(), any())).thenReturn(UnreadCountResponse(1))
        }
        setInboxApi(inboxApi)

        runBlocking { AttentiveSdk.refreshInbox() }

        val state = AttentiveSdk.inboxState.value
        assertEquals(2, state.messages.size)
        assertEquals("m1", state.messages[0].id)
        assertEquals(2, state.currentOffset)
        assertTrue(state.hasMoreMessages)
    }

    @Test
    fun refreshInbox_skips_whenInboxApiNotConfigured() {
        // inboxApi is null in @Before; refreshInbox should be a no-op with no network calls.
        runBlocking { AttentiveSdk.refreshInbox() }
        // Nothing to verify beyond "no throw"; asserting no state mutation.
        assertEquals(0, AttentiveSdk.inboxState.value.messages.size)
    }

    @Test
    fun refreshInbox_swallowsApiException() {
        val inboxApi = mock(RetrofitInboxApiService::class.java)
        runBlocking {
            whenever(inboxApi.getMessages(any(), any())).thenThrow(RuntimeException("boom"))
            whenever(inboxApi.getUnreadCount(any(), any())).thenReturn(UnreadCountResponse(0))
        }
        setInboxApi(inboxApi)

        runBlocking { AttentiveSdk.refreshInbox() }
        // API errors are logged and swallowed; state stays untouched (no
        // silent mock fallback).
        assertEquals(0, AttentiveSdk.inboxState.value.messages.size)
    }

    @Test
    fun refreshInboxUnreadCount_prefixesPushTokenWithFcm() {
        val inboxApi = mock(RetrofitInboxApiService::class.java)
        runBlocking {
            whenever(inboxApi.getUnreadCount(any(), any())).thenReturn(UnreadCountResponse(0))
        }
        setInboxApi(inboxApi)

        runBlocking { AttentiveSdk.refreshInboxUnreadCount() }

        val bodyCaptor = argumentCaptor<UnreadCountRequest>()
        runBlocking { verify(inboxApi).getUnreadCount(any(), bodyCaptor.capture()) }
        assertEquals("fcm:$PUSH_TOKEN", bodyCaptor.firstValue.pushToken)
    }

    @Test
    fun refreshInbox_mapsInboxMessageDtoFields() {
        val inboxApi = mock(RetrofitInboxApiService::class.java)
        runBlocking {
            whenever(inboxApi.getMessages(any(), any())).thenReturn(
                InboxResponse(
                    messages = listOf(
                        InboxMessageDto(
                            inboxMessageId = "m1",
                            title = "Hello",
                            body = "World",
                            imageUrl = "https://example.com/i.png",
                            actionUrl = "https://example.com/action",
                            sentAt = null,
                            isRead = false,
                        ),
                        InboxMessageDto(
                            inboxMessageId = "m2",
                            title = "No image",
                            body = "Small style",
                            imageUrl = null,
                            actionUrl = "https://example.com/other",
                            isRead = true,
                        ),
                    ),
                    nextPageToken = null,
                ),
            )
            whenever(inboxApi.getUnreadCount(any(), any())).thenReturn(UnreadCountResponse(1))
        }
        setInboxApi(inboxApi)

        runBlocking { AttentiveSdk.refreshInbox() }

        val messages = AttentiveSdk.inboxState.value.messages
        assertEquals("m1", messages[0].id)
        assertEquals("Hello", messages[0].title)
        assertEquals("World", messages[0].body)
        assertEquals("https://example.com/i.png", messages[0].imageUrl)
        assertEquals("https://example.com/action", messages[0].actionUrl)
        assertFalse(messages[0].isRead)
        assertEquals(Style.Large, messages[0].style)
        // No image → Small style
        assertNull(messages[1].imageUrl)
        assertEquals(Style.Small, messages[1].style)
        assertEquals("https://example.com/other", messages[1].actionUrl)
    }

    @Test
    fun loadMoreInboxMessages_appendsPage_andUpdatesOffset() {
        val inboxApi = mock(RetrofitInboxApiService::class.java)
        runBlocking {
            whenever(inboxApi.getMessages(any(), any())).thenReturn(
                InboxResponse(
                    messages = listOf(
                        InboxMessageDto(inboxMessageId = "m3", title = "third", isRead = false),
                        InboxMessageDto(inboxMessageId = "m4", title = "fourth", isRead = false),
                    ),
                    nextPageToken = null,
                ),
            )
        }
        setInboxApi(inboxApi)
        setInboxState(
            InboxState(
                messages = listOf(
                    fakeMessage("m1", isRead = true),
                    fakeMessage("m2", isRead = false),
                ),
                unreadCount = 1,
                currentOffset = 2,
                hasMoreMessages = true,
            ),
        )

        runBlocking { AttentiveSdk.loadMoreInboxMessages() }

        val state = AttentiveSdk.inboxState.value
        assertEquals(4, state.messages.size)
        assertEquals(listOf("m1", "m2", "m3", "m4"), state.messages.map { it.id })
        assertEquals(4, state.currentOffset)
        assertFalse(state.hasMoreMessages)
        assertFalse(state.isLoadingMore)
        assertEquals(3, state.unreadCount)
    }

    @Test
    fun loadMoreInboxMessages_skips_whenNoMoreMessages() {
        val inboxApi = mock(RetrofitInboxApiService::class.java)
        setInboxApi(inboxApi)
        setInboxState(
            InboxState(
                messages = listOf(fakeMessage("m1", isRead = false)),
                unreadCount = 1,
                currentOffset = 1,
                hasMoreMessages = false,
            ),
        )

        runBlocking { AttentiveSdk.loadMoreInboxMessages() }

        runBlocking { verify(inboxApi, never()).getMessages(any(), any()) }
    }

    @Test
    fun loadMoreInboxMessages_skips_whenAlreadyLoading() {
        val inboxApi = mock(RetrofitInboxApiService::class.java)
        setInboxApi(inboxApi)
        setInboxState(
            InboxState(
                messages = emptyList(),
                hasMoreMessages = true,
                isLoadingMore = true,
            ),
        )

        runBlocking { AttentiveSdk.loadMoreInboxMessages() }

        runBlocking { verify(inboxApi, never()).getMessages(any(), any()) }
    }

    @Test
    fun markRead_updatesLocalStateOptimistically_andCallsServer() {
        val inboxApi = mock(RetrofitInboxApiService::class.java)
        runBlocking {
            whenever(inboxApi.markMessagesRead(any(), any())).thenReturn(
                MarkMessagesReadResponse(
                    messages = listOf(MarkMessagesReadEntry(messageId = "m1", isRead = true)),
                    unreadCount = 0,
                ),
            )
        }
        setInboxApi(inboxApi)
        setInboxState(
            InboxState(
                messages = listOf(
                    fakeMessage("m1", isRead = false),
                    fakeMessage("m2", isRead = false),
                ),
                unreadCount = 2,
            ),
        )

        AttentiveSdk.markRead("m1")

        // Optimistic: local state immediately reflects m1 as read.
        val local = AttentiveSdk.inboxState.value
        assertTrue(local.messages.first { it.id == "m1" }.isRead)
        assertFalse(local.messages.first { it.id == "m2" }.isRead)

        // Wait for fire-and-forget IO coroutine.
        Thread.sleep(150)

        val bodyCaptor = argumentCaptor<MarkMessagesReadRequest>()
        runBlocking { verify(inboxApi).markMessagesRead(any(), bodyCaptor.capture()) }
        assertEquals(listOf("m1"), bodyCaptor.firstValue.messageIds)
        assertEquals(0, AttentiveSdk.inboxState.value.unreadCount)
    }

    @Test
    fun markUnread_updatesLocalStateOptimistically_andCallsServer() {
        val inboxApi = mock(RetrofitInboxApiService::class.java)
        runBlocking {
            whenever(inboxApi.markMessagesUnread(any(), any())).thenReturn(
                MarkMessagesReadResponse(
                    messages = listOf(MarkMessagesReadEntry(messageId = "m1", isRead = false)),
                    unreadCount = 2,
                ),
            )
        }
        setInboxApi(inboxApi)
        setInboxState(
            InboxState(
                messages = listOf(fakeMessage("m1", isRead = true)),
                unreadCount = 0,
            ),
        )

        AttentiveSdk.markUnread("m1")

        // Optimistic local update is synchronous and flips the message flag.
        // Don't assert the interim unreadCount here — the fire-and-forget IO
        // coroutine can race with this thread on fast CI runners and land the
        // server-provided count before we can observe the optimistic one.
        assertFalse(AttentiveSdk.inboxState.value.messages.first().isRead)

        Thread.sleep(150)

        runBlocking { verify(inboxApi).markMessagesUnread(any(), any()) }
        // Server-provided unreadCount overrides whatever the optimistic pass
        // computed.
        assertEquals(2, AttentiveSdk.inboxState.value.unreadCount)
    }

    @Test
    fun deleteMessage_removesLocally_andCallsServer() {
        val inboxApi = mock(RetrofitInboxApiService::class.java)
        runBlocking {
            @Suppress("UNCHECKED_CAST")
            whenever(inboxApi.deleteMessage(any(), any())).thenReturn(
                Response.success(DeleteMessageResponse(messageId = "m1"))
                    as Response<DeleteMessageResponse>,
            )
        }
        setInboxApi(inboxApi)
        setInboxState(
            InboxState(
                messages = listOf(
                    fakeMessage("m1", isRead = false),
                    fakeMessage("m2", isRead = false),
                ),
                unreadCount = 2,
            ),
        )

        AttentiveSdk.deleteMessage("m1")

        val local = AttentiveSdk.inboxState.value
        assertEquals(listOf("m2"), local.messages.map { it.id })
        assertEquals(1, local.unreadCount)

        Thread.sleep(150)

        val urlCaptor = argumentCaptor<String>()
        runBlocking { verify(inboxApi).deleteMessage(urlCaptor.capture(), any()) }
        assertTrue(urlCaptor.firstValue.endsWith("/inbox/messages/m1"))
    }

    @Test
    fun trackInboxClick_sendsRequestWithMessageIdAndActionUrl() {
        val inboxApi = mock(RetrofitInboxApiService::class.java)
        runBlocking {
            @Suppress("UNCHECKED_CAST")
            whenever(inboxApi.trackClick(any(), any())).thenReturn(
                Response.success(Unit) as Response<Unit>,
            )
        }
        setInboxApi(inboxApi)

        AttentiveSdk.trackInboxClick("m1", "https://example.com/action")

        Thread.sleep(150)

        val bodyCaptor = argumentCaptor<TrackClickRequest>()
        runBlocking { verify(inboxApi).trackClick(any(), bodyCaptor.capture()) }
        assertEquals("m1", bodyCaptor.firstValue.messageId)
        assertEquals("https://example.com/action", bodyCaptor.firstValue.actionUrl)
    }

    @Test
    fun clearUser_resetsInboxStateToEmpty() {
        setInboxState(
            InboxState(
                messages = listOf(fakeMessage("m1", isRead = false), fakeMessage("m2", isRead = true)),
                unreadCount = 1,
                currentOffset = 2,
                hasMoreMessages = true,
            ),
        )
        // Seed a non-null nextPageToken so we can assert it's cleared too.
        setNextPageToken("token-abc")

        AttentiveSdk.clearUser()

        val state = AttentiveSdk.inboxState.value
        assertEquals(0, state.messages.size)
        assertEquals(0, state.unreadCount)
        assertEquals(0, state.currentOffset)
        // Reset returns a fresh InboxState() — hasMoreMessages defaults to true
        // (we don't know yet), which is fine; the empty messages list is what matters.
        assertNull(readNextPageToken())
    }

    @Test
    fun refreshInbox_discardsResponseWhenIdentityChangesMidFlight() {
        val inboxApi = mock(RetrofitInboxApiService::class.java)
        runBlocking {
            whenever(inboxApi.getMessages(any(), any())).thenAnswer {
                // Simulate an identity change occurring while the request is in flight.
                AttentiveSdk.resetInboxForIdentityChange()
                InboxResponse(
                    messages = listOf(
                        InboxMessageDto(inboxMessageId = "stale1", title = "stale", isRead = false),
                    ),
                    nextPageToken = "stale-token",
                )
            }
            whenever(inboxApi.getUnreadCount(any(), any())).thenReturn(UnreadCountResponse(0))
        }
        setInboxApi(inboxApi)

        runBlocking { AttentiveSdk.refreshInbox() }

        // The stale response should NOT have populated state — resetInboxForIdentityChange
        // ran mid-flight and bumped the generation, so refreshInbox discards the response.
        val state = AttentiveSdk.inboxState.value
        assertEquals(0, state.messages.size)
        assertNull(readNextPageToken())
    }

    private fun setNextPageToken(token: String?) {
        val field = AttentiveSdk::class.java.getDeclaredField("nextPageToken")
        field.isAccessible = true
        field.set(AttentiveSdk, token)
    }

    private fun readNextPageToken(): String? {
        val field = AttentiveSdk::class.java.getDeclaredField("nextPageToken")
        field.isAccessible = true
        return field.get(AttentiveSdk) as String?
    }

    private fun fakeMessage(id: String, isRead: Boolean) =
        com.attentive.androidsdk.inbox.Message(
            id = id,
            title = "title $id",
            body = "body $id",
            timestamp = 0L,
            isRead = isRead,
            imageUrl = null,
            actionUrl = null,
            style = Style.Small,
        )

    companion object {
        private const val DOMAIN = "testDomain"
        private const val VISITOR_ID = "visitorIdValue"
        private const val NEW_VISITOR_ID = "newVisitorIdValue"
        private const val PUSH_TOKEN = "testPushToken"
    }
}
