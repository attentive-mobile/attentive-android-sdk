package com.attentive.androidsdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class AttentiveConfigTest {
    private static final String DOMAIN = "DOMAINValue";
    private static final AttentiveConfig.Mode MODE = AttentiveConfig.Mode.DEBUG;
    private static final String VISITOR_ID = "visitorIdValue";
    private static final String NEW_VISITOR_ID = "newVisitorIdValue";

    private FactoryMocks factoryMocks;

    @Before
    public void setup() {
        factoryMocks = FactoryMocks.mockFactoryObjects();

        doReturn(VISITOR_ID).when(factoryMocks.getVisitorService()).getVisitorId();
        doReturn(NEW_VISITOR_ID).when(factoryMocks.getVisitorService()).createNewVisitorId();
    }

    @After
    public void cleanup() {
        factoryMocks.close();
    }

    @Test
    public void constructor_validParams_gettersReturnConstructorParams() {
        // Arrange

        // Act
        AttentiveConfig config = new AttentiveConfig(DOMAIN, MODE, mock(Context.class));

        // Assert
        assertEquals(DOMAIN, config.getDomain());
        assertEquals(MODE, config.getMode());
        assertFalse(config.getUserIdentifiers().getVisitorId().isEmpty());
        assertTrue(config.getAttentiveApi() instanceof AttentiveApi);
    }

    @Test
    public void clearUser_identifyWasPreviouslyCalledWithIdentifiers_identifiersAreCleared() {
        // Arrange
        AttentiveConfig config = new AttentiveConfig(DOMAIN, MODE, mock(Context.class));
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
        AttentiveConfig config = new AttentiveConfig(DOMAIN, MODE, mock(Context.class));
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
        AttentiveConfig config = new AttentiveConfig(DOMAIN, MODE, mock(Context.class));
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