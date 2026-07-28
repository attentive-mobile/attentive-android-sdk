package com.attentive.androidsdk

import android.app.Application
import android.content.Context
import com.attentive.androidsdk.internal.util.AppInfo
import com.attentive.androidsdk.internal.util.AppInfo.isDebuggable
import com.attentive.androidsdk.internal.network.InboxMessageDto
import com.attentive.androidsdk.internal.network.InboxResponse
import com.attentive.androidsdk.internal.network.RetrofitInboxApiService
import com.attentive.androidsdk.internal.network.UnreadCountRequest
import com.attentive.androidsdk.internal.network.UnreadCountResponse
import com.attentive.androidsdk.internal.util.Constants
import com.attentive.androidsdk.push.TokenProvider
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val inboxApiField = AttentiveSdk::class.java.getDeclaredField("inboxApi")
        inboxApiField.isAccessible = true
        inboxApiField.set(AttentiveSdk, null)
        val inboxStateField = AttentiveSdk::class.java.getDeclaredField("_inboxState")
        inboxStateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = inboxStateField.get(AttentiveSdk)
            as kotlinx.coroutines.flow.MutableStateFlow<com.attentive.androidsdk.inbox.InboxState>
        flow.value = com.attentive.androidsdk.inbox.InboxState()
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
        val inboxApiField = AttentiveSdk::class.java.getDeclaredField("inboxApi")
        inboxApiField.isAccessible = true
        inboxApiField.set(AttentiveSdk, inboxApi)

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
        val inboxApiField = AttentiveSdk::class.java.getDeclaredField("inboxApi")
        inboxApiField.isAccessible = true
        inboxApiField.set(AttentiveSdk, inboxApi)

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
        val inboxApiField = AttentiveSdk::class.java.getDeclaredField("inboxApi")
        inboxApiField.isAccessible = true
        inboxApiField.set(AttentiveSdk, inboxApi)

        runBlocking { AttentiveSdk.refreshInboxUnreadCount() }

        val bodyCaptor = argumentCaptor<UnreadCountRequest>()
        runBlocking { verify(inboxApi).getUnreadCount(any(), bodyCaptor.capture()) }
        assertEquals("fcm:$PUSH_TOKEN", bodyCaptor.firstValue.pushToken)
    }

    companion object {
        private const val DOMAIN = "testDomain"
        private const val VISITOR_ID = "visitorIdValue"
        private const val NEW_VISITOR_ID = "newVisitorIdValue"
        private const val PUSH_TOKEN = "testPushToken"
    }
}
