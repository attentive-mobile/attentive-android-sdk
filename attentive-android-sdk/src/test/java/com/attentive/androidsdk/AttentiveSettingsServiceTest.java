package com.attentive.androidsdk;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Bundle;
import org.junit.Test;

public class AttentiveSettingsServiceTest {

    @Test
    public void testHandleSkipFatigueExtra_asTrue() {
        final Bundle bundle = mock(Bundle.class);
        final SettingsService settingsService = mock(SettingsService.class);
        when(bundle.containsKey(AttentiveSettingsService.EXTRA_SET_SKIP_FATIGUE)).thenReturn(true);
        when(bundle.getBoolean(AttentiveSettingsService.EXTRA_SET_SKIP_FATIGUE, false)).thenReturn(true);

        AttentiveSettingsService.handleSkipFatigueExtra(bundle, settingsService);

        verify(settingsService).setSkipFatigueEnabled(true);
    }

    @Test
    public void testHandleSkipFatigueExtra_asFalse() {
        final Bundle bundle = mock(Bundle.class);
        final SettingsService settingsService = mock(SettingsService.class);
        when(bundle.containsKey(AttentiveSettingsService.EXTRA_SET_SKIP_FATIGUE)).thenReturn(true);
        when(bundle.getBoolean(AttentiveSettingsService.EXTRA_SET_SKIP_FATIGUE, false)).thenReturn(false);

        AttentiveSettingsService.handleSkipFatigueExtra(bundle, settingsService);

        verify(settingsService).setSkipFatigueEnabled(false);
    }

    @Test
    public void testHandleSkipFatigueExtra_noValuePassed() {
        final Bundle bundle = mock(Bundle.class);
        final SettingsService settingsService = mock(SettingsService.class);
        when(bundle.containsKey(AttentiveSettingsService.EXTRA_SET_SKIP_FATIGUE)).thenReturn(false);

        AttentiveSettingsService.handleSkipFatigueExtra(bundle, settingsService);

        verify(settingsService, times(0)).setSkipFatigueEnabled(false);
    }

    @Test
    public void testHandleResetSettingsExtra() {
        final Bundle bundle = mock(Bundle.class);
        final SettingsService settingsService = mock(SettingsService.class);
        when(bundle.containsKey(AttentiveSettingsService.EXTRA_RESET_SETTINGS)).thenReturn(true);
        when(bundle.getBoolean(AttentiveSettingsService.EXTRA_RESET_SETTINGS, false)).thenReturn(true);

        AttentiveSettingsService.handleResetSettingsExtra(bundle, settingsService);

        verify(settingsService).resetSettings();
    }

    @Test
    public void testHandleResetSettingsExtra_notExecuted_noExtra() {
        final Bundle bundle = mock(Bundle.class);
        final SettingsService settingsService = mock(SettingsService.class);
        when(bundle.containsKey(AttentiveSettingsService.EXTRA_RESET_SETTINGS)).thenReturn(false);

        AttentiveSettingsService.handleResetSettingsExtra(bundle, settingsService);

        verify(settingsService, times(0)).resetSettings();
    }

    @Test
    public void testHandleResetSettingsExtra_notExecuted_false() {
        final Bundle bundle = mock(Bundle.class);
        final SettingsService settingsService = mock(SettingsService.class);
        when(bundle.containsKey(AttentiveSettingsService.EXTRA_RESET_SETTINGS)).thenReturn(true);
        when(bundle.getBoolean(AttentiveSettingsService.EXTRA_RESET_SETTINGS, false)).thenReturn(false);

        AttentiveSettingsService.handleResetSettingsExtra(bundle, settingsService);

        verify(settingsService, times(0)).resetSettings();
    }
}