package com.attentive.androidsdk

import android.os.Looper
import com.attentive.androidsdk.AttentiveApi
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.AttentiveEventTracker.Companion.instance
import com.attentive.androidsdk.events.Event
import com.attentive.androidsdk.tracking.AppLaunchTracker
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever

class AttentiveEventTrackerTest {
    lateinit var config: AttentiveConfig
    private var attentiveApi: AttentiveApi? = null

    @Before
    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    fun setup() {
        config = Mockito.mock(AttentiveConfig::class.java)
        attentiveApi = Mockito.mock(AttentiveApi::class.java)
        Mockito.doReturn(attentiveApi).`when`(config)?.attentiveApi
        Mockito.doReturn(DOMAIN).`when`(config)?.domain
        Mockito.doReturn(USER_IDENTIFIERS).`when`(config)?.userIdentifiers

        resetSingleton()
    }

    @Throws(
        SecurityException::class,
        NoSuchFieldException::class,
        IllegalArgumentException::class,
        IllegalAccessException::class
    )
    fun resetSingleton() {
        val instance = AttentiveEventTracker::class.java.getDeclaredField("INSTANCE")
        instance.isAccessible = true
        instance[null] = null
    }

    @Test
    fun initialize_validConfig_success() {
        instance.initialize(config)
    }

    @Test
    fun initialize_calledTwice_doesNotThrow() {
        instance.initialize(config)
    }


    @Test
    fun recordEvent_validEvent_sendsToApi() {
        // Arrange
        instance.initialize(config)
        val eventToSend = Mockito.mock(
            Event::class.java
        )

        // Act
        instance.recordEvent(eventToSend)

        // Assert
        Mockito.verify(attentiveApi)?.sendEvent(eventToSend, USER_IDENTIFIERS, DOMAIN)
    }

    companion object {
        private const val DOMAIN = "someDomainValue"
        private val USER_IDENTIFIERS = UserIdentifiers.Builder().build()
    }
}