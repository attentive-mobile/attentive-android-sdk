package com.attentive.androidsdk

import android.os.Bundle
import com.attentive.androidsdk.AttentiveSettingsService.Companion.handleResetSettingsExtra
import org.junit.Test
import org.mockito.Mockito

class AttentiveSettingsServiceTest {
    @Test
    fun testHandleResetSettingsExtra() {
        val bundle = Mockito.mock(Bundle::class.java)
        val settingsService = Mockito.mock(SettingsService::class.java)
        Mockito.`when`(bundle.containsKey(AttentiveSettingsService.EXTRA_RESET_SETTINGS))
            .thenReturn(true)
        Mockito.`when`(bundle.getBoolean(AttentiveSettingsService.EXTRA_RESET_SETTINGS, false))
            .thenReturn(true)

        handleResetSettingsExtra(bundle, settingsService)

        Mockito.verify(settingsService).resetSettings()
    }

    @Test
    fun testHandleResetSettingsExtra_notExecuted_noExtra() {
        val bundle = Mockito.mock(Bundle::class.java)
        val settingsService = Mockito.mock(SettingsService::class.java)
        Mockito.`when`(bundle.containsKey(AttentiveSettingsService.EXTRA_RESET_SETTINGS))
            .thenReturn(false)

        handleResetSettingsExtra(bundle, settingsService)

        Mockito.verify(settingsService, Mockito.times(0)).resetSettings()
    }

    @Test
    fun testHandleResetSettingsExtra_notExecuted_false() {
        val bundle = Mockito.mock(Bundle::class.java)
        val settingsService = Mockito.mock(SettingsService::class.java)
        Mockito.`when`(bundle.containsKey(AttentiveSettingsService.EXTRA_RESET_SETTINGS))
            .thenReturn(true)
        Mockito.`when`(bundle.getBoolean(AttentiveSettingsService.EXTRA_RESET_SETTINGS, false))
            .thenReturn(false)

        handleResetSettingsExtra(bundle, settingsService)

        Mockito.verify(settingsService, Mockito.times(0)).resetSettings()
    }
}
