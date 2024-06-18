package com.attentive.androidsdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import android.content.Context;
import com.attentive.androidsdk.internal.events.InfoEvent;
import com.attentive.androidsdk.internal.util.AppInfo;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class AttentiveConfigTest {
    private static final String DOMAIN = "DOMAINValue";
    private static final AttentiveConfig.Mode MODE = AttentiveConfig.Mode.DEBUG;
    private static final String VISITOR_ID = "visitorIdValue";
    private static final String NEW_VISITOR_ID = "newVisitorIdValue";

    private FactoryMocks factoryMocks;
    private static MockedStatic<AppInfo> mockedAppInfo;

    @Before
    public void setup() {
        factoryMocks = FactoryMocks.mockFactoryObjects();

        doReturn(VISITOR_ID).when(factoryMocks.getVisitorService()).getVisitorId();
        doReturn(NEW_VISITOR_ID).when(factoryMocks.getVisitorService()).createNewVisitorId();
        mockedAppInfo = Mockito.mockStatic(AppInfo.class);
        when(AppInfo.isDebuggable(any())).thenReturn(false);
    }

    @After
    public void cleanup() {
        factoryMocks.close();
        mockedAppInfo.close();
    }

    @Test
    public void constructor_validParams_gettersReturnConstructorParams() {
        // Arrange

        // Act
        AttentiveConfig config = new AttentiveConfig.Builder()
                .domain(DOMAIN)
                .mode(MODE)
                .context(mock(Context.class))
                .build();

        // Assert
        assertEquals(DOMAIN, config.getDomain());
        assertEquals(MODE, config.getMode());
        String visitorId = config.getUserIdentifiers().getVisitorId();
        assertTrue(visitorId != null && !visitorId.isEmpty());
        assertNotNull(config.getAttentiveApi());
        assertFalse(config.skipFatigueOnCreatives());

        verify(factoryMocks.getAttentiveApi()).sendEvent(argThat(arg -> arg instanceof InfoEvent), eq(config.getUserIdentifiers()), eq(DOMAIN));
    }

    @Test
    public void constructor_validParams_gettersReturnConstructorParams_skipFatigueAsTrue() {
        // Arrange

        // Act
        AttentiveConfig config = new AttentiveConfig.Builder()
                .domain(DOMAIN)
                .mode(MODE)
                .context(mock(Context.class))
                .skipFatigueOnCreatives(true)
                .build();

        // Assert
        assertEquals(DOMAIN, config.getDomain());
        assertEquals(MODE, config.getMode());
        String visitorId = config.getUserIdentifiers().getVisitorId();
        assertTrue(visitorId != null && !visitorId.isEmpty());
        assertNotNull(config.getAttentiveApi());
        assertTrue(config.skipFatigueOnCreatives());

        verify(factoryMocks.getAttentiveApi()).sendEvent(argThat(arg -> arg instanceof InfoEvent), eq(config.getUserIdentifiers()), eq(DOMAIN));
    }

    @Test
    public void clearUser_identifyWasPreviouslyCalledWithIdentifiers_identifiersAreCleared() {
        // Arrange
        AttentiveConfig config = new AttentiveConfig.Builder()
                .domain(DOMAIN)
                .mode(MODE)
                .context(mock(Context.class))
                .build();
        UserIdentifiers userIdentifiers = buildUserIdentifiers();
        config.identify(userIdentifiers);

        // Act
        config.clearUser();

        // Assert
        assertNull(config.getUserIdentifiers().getClientUserId());
        assertNull(config.getUserIdentifiers().getPhone());
        assertNull(config.getUserIdentifiers().getEmail());
        assertNull(config.getUserIdentifiers().getShopifyId());
        assertNull(config.getUserIdentifiers().getKlaviyoId());
        assertEquals(Map.of(), config.getUserIdentifiers().getCustomIdentifiers());
    }

    @Test
    public void clearUser_verifyNewVisitorIdCreated() {
        // Arrange
        AttentiveConfig config = new AttentiveConfig.Builder()
                .domain(DOMAIN)
                .mode(MODE)
                .context(mock(Context.class))
                .build();
        assertEquals(VISITOR_ID, config.getUserIdentifiers().getVisitorId());

        // Act
        config.clearUser();

        // Assert
        verify(factoryMocks.getVisitorService()).createNewVisitorId();
        assertEquals(NEW_VISITOR_ID, config.getUserIdentifiers().getVisitorId());
    }

    @Test
    public void identify_identifyWasPreviouslyCalledWithIdentifiers_identifiersAreUpdated() {
        // Arrange
        AttentiveConfig config = new AttentiveConfig.Builder()
                .domain(DOMAIN)
                .mode(MODE)
                .context(mock(Context.class))
                .build();
        config.identify(buildUserIdentifiers());

        // Act
        UserIdentifiers newUserIdentifiers = new UserIdentifiers.Builder()
                .withClientUserId("newClientId")
                .withPhone("+14158889999")
                .withEmail("newEmail@gmail.com")
                .withShopifyId("67890")
                .withKlaviyoId("09876")
                .withCustomIdentifiers(Map.of("key1", "newValue1", "extraKey", "extraValue"))
                .build();
        config.identify(newUserIdentifiers);

        // Assert
        assertEquals("newClientId", config.getUserIdentifiers().getClientUserId());
        assertEquals("+14158889999", config.getUserIdentifiers().getPhone());
        assertEquals("newEmail@gmail.com", config.getUserIdentifiers().getEmail());
        assertEquals("67890", config.getUserIdentifiers().getShopifyId());
        assertEquals("09876", config.getUserIdentifiers().getKlaviyoId());
        assertEquals(
                Map.of("key1", "newValue1", "key2", "value2", "extraKey", "extraValue"),
                config.getUserIdentifiers().getCustomIdentifiers());
    }

    @Test
    public void identify_withIdentifiers_sendsUserIdentifierCollectedEvent() {
        AttentiveConfig config = new AttentiveConfig.Builder()
                .domain(DOMAIN)
                .mode(MODE)
                .context(mock(Context.class))
                .build();
        UserIdentifiers userIdentifiers = buildUserIdentifiers();
        config.identify(userIdentifiers);

        verify(
                factoryMocks.getAttentiveApi(),
                times(1)
        ).sendUserIdentifiersCollectedEvent(
                eq(DOMAIN),
                eq(config.getUserIdentifiers()),
                any(AttentiveApiCallback.class));
    }

    @Test
    public void changeDomain() {
        // Arrange
        AttentiveConfig config = new AttentiveConfig.Builder()
                .domain(DOMAIN)
                .mode(MODE)
                .context(mock(Context.class))
                .build();
        UserIdentifiers userIdentifiers = buildUserIdentifiers();
        config.identify(userIdentifiers);

        // Act
        config.changeDomain("newDomain");

        // Assert
        assertEquals("newDomain", config.getDomain());
    }

    @Test
    public void changeDomain_emptyDomain() {
        // Arrange
        AttentiveConfig config = new AttentiveConfig.Builder()
                .domain(DOMAIN)
                .mode(MODE)
                .context(mock(Context.class))
                .build();
        UserIdentifiers userIdentifiers = buildUserIdentifiers();
        config.identify(userIdentifiers);

        // Act
        config.changeDomain("");

        // Assert
        assertEquals(DOMAIN, config.getDomain());

    }

    private UserIdentifiers buildUserIdentifiers() {
        return new UserIdentifiers.Builder()
                .withClientUserId("clientId")
                .withPhone("+14156667777")
                .withEmail("email@gmail.com")
                .withShopifyId("12345")
                .withKlaviyoId("54321")
                .withCustomIdentifiers(Map.of("key1", "value1", "key2", "value2"))
                .build();
    }
}