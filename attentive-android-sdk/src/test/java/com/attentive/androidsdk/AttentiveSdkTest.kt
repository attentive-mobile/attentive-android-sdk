package com.attentive.androidsdk

import android.app.Application
import android.content.Context
import com.attentive.androidsdk.internal.util.Constants
import com.google.firebase.messaging.RemoteMessage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class AttentiveSdkTest {
    private lateinit var remoteMessage: RemoteMessage
    private lateinit var application: Application
    private lateinit var context: Context
    private lateinit var callback: AttentiveSdk.PushTokenCallback

    @Before
    fun setUp() {
        remoteMessage = mock(RemoteMessage::class.java)
        application = mock(Application::class.java)
        context = mock(Context::class.java)
        callback = mock(AttentiveSdk.PushTokenCallback::class.java)
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
    fun sendNotification_callsAttentivePush() {
        AttentiveSdk.sendNotification(remoteMessage)
        // No assertion, just ensure no exception and method is callable
    }

    @Test
    fun getPushTokenWithCallback_invokesCallback() {
        AttentiveSdk.getPushTokenWithCallback(application, true, callback)
        // No assertion, just ensure method is callable
    }
}
