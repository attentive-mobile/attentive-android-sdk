package com.attentive.androidsdk

import android.app.Application
import android.content.Context
import com.attentive.androidsdk.events.Event
import com.attentive.androidsdk.internal.events.InfoEvent
import com.attentive.androidsdk.internal.util.AppInfo
import com.attentive.androidsdk.internal.util.AppInfo.isDebuggable
import com.attentive.androidsdk.tracking.AppLaunchTracker
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.internal.verification.VerificationModeFactory
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq

class AttentiveConfigTest {
    private lateinit var factoryMocks: FactoryMocks
    private var appLaunchTracker = Mockito.mock(AppLaunchTracker::class.java)

    @Before
    fun setup() {
        factoryMocks = FactoryMocks.mockFactoryObjects()

        Mockito.doReturn(VISITOR_ID).`when`(factoryMocks.visitorService).visitorId
        Mockito.doReturn(NEW_VISITOR_ID).`when`(factoryMocks.visitorService)
            .createNewVisitorId()
        mockedAppInfo = Mockito.mockStatic(
            AppInfo::class.java
        )
        Mockito.`when`(isDebuggable(any())).thenReturn(false)
    }

    @After
    fun cleanup() {
        factoryMocks.close()
        mockedAppInfo!!.close()
    }

    @Test
    fun constructor_validParams_gettersReturnConstructorParams() {
        // Arrange

        // Act

        val config = AttentiveConfig.Builder()
            .domain(DOMAIN)
            .mode(MODE)
            .applicationContext(Mockito.mock(Application::class.java))
            .build()

        // Assert
        Assert.assertEquals(DOMAIN, config.domain)
        Assert.assertEquals(MODE, config.mode)
        val visitorId = config.userIdentifiers.visitorId
        Assert.assertTrue(!visitorId.isNullOrEmpty())
        Assert.assertNotNull(config.attentiveApi)
        Assert.assertFalse(config.skipFatigueOnCreatives())

        Mockito.verify(factoryMocks.attentiveApi).sendEvent(
            argThat{ arg: Event? -> arg is InfoEvent },
            eq(config.userIdentifiers),
            eq(
                DOMAIN
            ),
            anyOrNull()
        )
    }

    @Test
    fun constructor_validParams_gettersReturnConstructorParams_skipFatigueAsTrue() {
        // Arrange

        // Act

        val config = AttentiveConfig.Builder()
            .domain(DOMAIN)
            .mode(MODE)
            .applicationContext(Mockito.mock(Application::class.java))
            .skipFatigueOnCreatives(true)
            .build()

        // Assert
        Assert.assertEquals(DOMAIN, config.domain)
        Assert.assertEquals(MODE, config.mode)
        val visitorId = config.userIdentifiers.visitorId
        Assert.assertTrue(!visitorId.isNullOrEmpty())
        Assert.assertNotNull(config.attentiveApi)
        Assert.assertTrue(config.skipFatigueOnCreatives())

        Mockito.verify(factoryMocks.attentiveApi).sendEvent(
            argThat{ arg: Event? -> arg is InfoEvent },
            eq(config.userIdentifiers),
            eq(DOMAIN),
            anyOrNull()
        )
    }

    @Test
    fun clearUser_identifyWasPreviouslyCalledWithIdentifiers_identifiersAreCleared() {
        // Arrange
        val config = AttentiveConfig.Builder()
            .domain(DOMAIN)
            .mode(MODE)
            .applicationContext(Mockito.mock(Application::class.java))
                       .build()
        val userIdentifiers = buildUserIdentifiers()
        config.identify(userIdentifiers)

        // Act
        config.clearUser()

        // Assert
        Assert.assertNull(config.userIdentifiers.clientUserId)
        Assert.assertNull(config.userIdentifiers.phone)
        Assert.assertNull(config.userIdentifiers.email)
        Assert.assertNull(config.userIdentifiers.shopifyId)
        Assert.assertNull(config.userIdentifiers.klaviyoId)
        Assert.assertEquals(emptyMap<Any, Any>(), config.userIdentifiers.customIdentifiers)
    }

    @Test
    fun clearUser_verifyNewVisitorIdCreated() {
        // Arrange
        val config = AttentiveConfig.Builder()
            .domain(DOMAIN)
            .mode(MODE)
            .applicationContext(Mockito.mock(Application::class.java))
            .build()
        Assert.assertEquals(VISITOR_ID, config.userIdentifiers.visitorId)

        // Act
        config.clearUser()

        // Assert
        Mockito.verify(factoryMocks.visitorService).createNewVisitorId()
        Assert.assertEquals(NEW_VISITOR_ID, config.userIdentifiers.visitorId)
    }

    @Test
    fun identify_identifyWasPreviouslyCalledWithIdentifiers_identifiersAreUpdated() {
        // Arrange
        val config = AttentiveConfig.Builder()
            .domain(DOMAIN)
            .mode(MODE)
            .applicationContext(Mockito.mock(Application::class.java))
                       .build()
        config.identify(buildUserIdentifiers())

        // Act
        val newUserIdentifiers = UserIdentifiers.Builder()
            .withClientUserId("newClientId")
            .withPhone("+14158889999")
            .withEmail("newEmail@gmail.com")
            .withShopifyId("67890")
            .withKlaviyoId("09876")
            .withCustomIdentifiers(mapOf("key1" to "newValue1", "extraKey" to "extraValue"))
            .build()
        config.identify(newUserIdentifiers)

        // Assert
        Assert.assertEquals("newClientId", config.userIdentifiers.clientUserId)
        Assert.assertEquals("+14158889999", config.userIdentifiers.phone)
        Assert.assertEquals("newEmail@gmail.com", config.userIdentifiers.email)
        Assert.assertEquals("67890", config.userIdentifiers.shopifyId)
        Assert.assertEquals("09876", config.userIdentifiers.klaviyoId)
        Assert.assertEquals(
            mapOf("key1" to "newValue1", "key2" to "value2", "extraKey" to "extraValue"),
            config.userIdentifiers.customIdentifiers
        )
    }

    @Test
    fun identify_withIdentifiers_sendsUserIdentifierCollectedEvent() {
        val config = AttentiveConfig.Builder()
            .domain(DOMAIN)
            .mode(MODE)
            .applicationContext(Mockito.mock(Application::class.java))
                       .build()
        val userIdentifiers = buildUserIdentifiers()
        config.identify(userIdentifiers)

        Mockito.verify(
            factoryMocks.attentiveApi,
            VerificationModeFactory.times(1)
        ).sendUserIdentifiersCollectedEvent(
            eq(DOMAIN),
            eq(config.userIdentifiers),
            any()
        )
    }

    @Test
    fun changeDomain() {
        // Arrange
        val config = AttentiveConfig.Builder()
            .domain(DOMAIN)
            .mode(MODE)
            .applicationContext(Mockito.mock(Application::class.java))
                       .build()
        val userIdentifiers = buildUserIdentifiers()
        config.identify(userIdentifiers)

        // Act
        config.changeDomain("newDomain")

        // Assert
        Assert.assertEquals("newDomain", config.domain)
    }
    
    @Test
    fun domain_emptyString_throwsException() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            AttentiveConfig.Builder().domain("")
        }
    }

    @Test
    fun domain_containsAttnTv_throwsException() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            AttentiveConfig.Builder().domain("https://mycompany.attn.tv")
        }
    }

    @Test
    fun domain_containsColon_throwsException() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            AttentiveConfig.Builder().domain("https://mycompany")
        }
    }

    @Test
    fun domain_containsSlash_throwsException() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            AttentiveConfig.Builder().domain("mycompany/path")
        }
    }

    @Test
    fun domain_validDomain_doesNotThrow() {
        val builder = AttentiveConfig.Builder().domain("mycompany")
        // If we get here without exception, the test passes
        Assert.assertNotNull(builder)
    }

    private fun buildUserIdentifiers(): UserIdentifiers {
        return UserIdentifiers.Builder()
            .withClientUserId("clientId")
            .withPhone("+14156667777")
            .withEmail("email@gmail.com")
            .withShopifyId("12345")
            .withKlaviyoId("54321")
            .withCustomIdentifiers(mapOf("key1" to "value1", "key2" to "value2"))
            .build()
    }

    companion object {
        private const val DOMAIN = "DOMAINValue"
        private val MODE = AttentiveConfig.Mode.DEBUG
        private const val VISITOR_ID = "visitorIdValue"
        private const val NEW_VISITOR_ID = "newVisitorIdValue"

        private var mockedAppInfo: MockedStatic<AppInfo>? = null
    }
}