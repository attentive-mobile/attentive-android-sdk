package com.attentive.androidsdk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class AttentiveConfigTest {
    private static final String DOMAIN = "DOMAINValue";
    private static final AttentiveConfig.Mode MODE = AttentiveConfig.Mode.DEBUG;
    private static final String VISITOR_ID = "visitorIdValue";

    private FactoryMocks factoryMocks;

    @Before
    public void setup() {
        factoryMocks = FactoryMocks.mockFactoryObjects();

        doReturn(VISITOR_ID).when(factoryMocks.getVisitorService()).getVisitorId();
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
    }

    @Test
    public void clearUser_identifyWasPreviouslyCalledWithIdentifiers_identifiersAreCleared() {
        // Arrange
        AttentiveConfig config = new AttentiveConfig(DOMAIN, MODE, mock(Context.class));
        UserIdentifiers userIdentifiers = new UserIdentifiers.Builder().withClientUserId("someClientId").build();
        config.identify(userIdentifiers);

        final String newVisitorId = "newValue";
        doReturn(newVisitorId).when(factoryMocks.getVisitorService()).createNewVisitorId();

        // Act
        config.clearUser();

        // Assert
        assertNull(config.getUserIdentifiers().getClientUserId());
    }

    @Test
    public void clearUser_verifyNewVisitorIdCreated() {
        // Arrange
        final String newVisitorId = "newValue";
        doReturn(newVisitorId).when(factoryMocks.getVisitorService()).createNewVisitorId();

        AttentiveConfig config = new AttentiveConfig(DOMAIN, MODE, mock(Context.class));
        assertEquals(VISITOR_ID, config.getUserIdentifiers().getVisitorId());

        // Act
        config.clearUser();

        // Assert
        verify(factoryMocks.getVisitorService()).createNewVisitorId();
        assertEquals(newVisitorId, config.getUserIdentifiers().getVisitorId());
    }
}