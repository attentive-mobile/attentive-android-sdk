package com.attentive.androidsdk;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.attentive.androidsdk.events.Event;
import java.lang.reflect.Field;
import org.junit.Before;
import org.junit.Test;

public class AttentiveEventTrackerTest {
    private static final String DOMAIN = "someDomainValue";
    private static final UserIdentifiers USER_IDENTIFIERS = new UserIdentifiers.Builder().build();

    private AttentiveConfig config;
    private AttentiveApi attentiveApi;

    @Before
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        config = mock(AttentiveConfig.class);
        attentiveApi = mock(AttentiveApi.class);
        doReturn(attentiveApi).when(config).getAttentiveApi();
        doReturn(DOMAIN).when(config).getDomain();
        doReturn(USER_IDENTIFIERS).when(config).getUserIdentifiers();

        resetSingleton();
    }

    public void resetSingleton() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field instance = AttentiveEventTracker.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @Test
    public void getInstance_doesNotThrow() {
        // Verify does not throw
        AttentiveEventTracker.getInstance();
    }

    @Test
    public void getInstance_calledTwice_returnsSameInstance() {
        assertEquals(AttentiveEventTracker.getInstance(), AttentiveEventTracker.getInstance());
    }

    @Test
    public void initialize_nullConfig_throws() {
        assertThrows(IllegalArgumentException.class, () -> AttentiveEventTracker.getInstance().initialize(null));
    }

    @Test
    public void initialize_validConfig_success() {
        AttentiveEventTracker.getInstance().initialize(config);
    }

    @Test
    public void initialize_calledTwice_doesNotThrow() {
        AttentiveEventTracker.getInstance().initialize(config);
    }

    @Test
    public void recordEvent_nullEvent_throws() {
        AttentiveEventTracker.getInstance().initialize(config);
        assertThrows(IllegalArgumentException.class, () -> AttentiveEventTracker.getInstance().recordEvent(null));
    }

    @Test
    public void recordEvent_validEvent_sendsToApi() {
        // Arrange
        AttentiveEventTracker.getInstance().initialize(config);
        Event eventToSend = mock(Event.class);

        // Act
        AttentiveEventTracker.getInstance().recordEvent(eventToSend);

        // Assert
        verify(attentiveApi).sendEvent(eventToSend, USER_IDENTIFIERS, DOMAIN);
    }
}