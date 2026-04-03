package com.attentive.androidsdk

import android.app.Application
import android.content.Context
import com.attentive.androidsdk.internal.util.AppInfo
import com.attentive.androidsdk.internal.util.AppInfo.isDebuggable
import com.attentive.androidsdk.internal.util.Constants
import com.attentive.androidsdk.push.TokenProvider
import com.google.firebase.messaging.RemoteMessage
import org.junit.After
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

        verify(factoryMocks.attentiveApi).sendUserUpdate(
            eq(DOMAIN), isNull(), isNull(), eq(NEW_VISITOR_ID), any()
        )
    }

    @Test
    fun clearUser_resetsIdentifiers() {
        AttentiveSdk.clearUser()

        verify(factoryMocks.visitorService).createNewVisitorId()
    }

    @Test
    fun updateUser_withWhitespaceOnlyEmail_doesNotCallSendUserUpdate() {
        AttentiveSdk.updateUser(email = "   ")

        verify(factoryMocks.attentiveApi, never()).sendUserUpdate(any(), any(), any(), any(), any())
    }

    @Test
    fun updateUser_withBothNullParams_doesNotCallSendUserUpdate() {
        AttentiveSdk.updateUser(email = null, phoneNumber = null)

        verify(factoryMocks.attentiveApi, never()).sendUserUpdate(any(), any(), any(), any(), any())
    }

    companion object {
        private const val DOMAIN = "testDomain"
        private const val VISITOR_ID = "visitorIdValue"
        private const val NEW_VISITOR_ID = "newVisitorIdValue"
        private const val PUSH_TOKEN = "testPushToken"
    }
}
