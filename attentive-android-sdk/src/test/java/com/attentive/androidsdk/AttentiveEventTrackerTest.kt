package com.attentive.androidsdk

import android.app.Application
import android.os.Looper
import androidx.lifecycle.Lifecycle
import com.attentive.androidsdk.AttentiveApi
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.events.Event
import com.attentive.androidsdk.tracking.AppLaunchTracker
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import kotlin.jvm.java

class AttentiveEventTrackerTest {
    lateinit var config: AttentiveConfig
    private var attentiveApi: AttentiveApi? = null
    private var launchTracker: AppLaunchTracker? = null
    val mockLifecycle = Mockito.mock(Lifecycle::class.java)
    val instance = Mockito.spy(AttentiveEventTracker::class.java)


    @Before
    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    fun setup() {
        config = Mockito.mock(AttentiveConfig::class.java)
        attentiveApi = Mockito.mock(AttentiveApi::class.java)
        launchTracker = Mockito.mock(AppLaunchTracker::class.java)
        Mockito.doReturn(attentiveApi).`when`(config)?.attentiveApi
        Mockito.doReturn(DOMAIN).`when`(config)?.domain
        Mockito.doReturn(USER_IDENTIFIERS).`when`(config)?.userIdentifiers
        Mockito.doReturn(mockLifecycle).`when`(launchTracker)?.lifecycle
        Mockito.doReturn(launchTracker).whenever(instance).launchTracker
        whenever(config.applicationContext).thenReturn(Mockito.mock(Application::class.java))
    }
//
//    @Test
//    fun initialize_validConfig_success() {
//        instance.initialize(config)
//    }
//
//    @Test
//    fun initialize_calledTwice_doesNotThrow() {
//        instance.initialize(config)
//    }
//
//
//    @Test
//    fun recordEvent_validEvent_sendsToApi() {
//        // Arrange
//        instance.initialize(config)
//        val eventToSend = Mockito.mock(
//            Event::class.java
//        )
//
//        // Act
//        instance.recordEvent(eventToSend)
//
//        // Assert
//        Mockito.verify(attentiveApi)?.sendEvent(eventToSend, USER_IDENTIFIERS, DOMAIN)
//    }

    companion object {
        private const val DOMAIN = "someDomainValue"
        private val USER_IDENTIFIERS = UserIdentifiers.Builder().build()
    }
}