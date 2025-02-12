package com.attentive.androidsdk

import android.os.Bundle
import com.attentive.androidsdk.AttentiveSettingsService.Companion.handleResetSettingsExtra
import com.attentive.androidsdk.AttentiveSettingsService.Companion.handleSkipFatigueExtra
import com.attentive.androidsdk.SettingsService
import org.junit.Test
import org.mockito.Mockito

class AttentiveSettingsServiceTest {
    @Test
    fun testHandleSkipFatigueExtra_asTrue() {
        val bundle = Mockito.mock(Bundle::class.java)
        val settingsService = Mockito.mock(SettingsService::class.java)
        Mockito.`when`(bundle.containsKey(AttentiveSettingsService.EXTRA_SET_SKIP_FATIGUE))
            .thenReturn(true)
        Mockito.`when`(bundle.getBoolean(AttentiveSettingsService.EXTRA_SET_SKIP_FATIGUE, false))
            .thenReturn(true)

        handleSkipFatigueExtra(bundle, settingsService)

        Mockito.verify(settingsService).isSkipFatigueEnabled = true
    }

    @Test
    fun testHandleSkipFatigueExtra_asFalse() {
        val bundle = Mockito.mock(Bundle::class.java)
        val settingsService = Mockito.mock(SettingsService::class.java)
        Mockito.`when`(bundle.containsKey(AttentiveSettingsService.EXTRA_SET_SKIP_FATIGUE))
            .thenReturn(true)
        Mockito.`when`(bundle.getBoolean(AttentiveSettingsService.EXTRA_SET_SKIP_FATIGUE, false))
            .thenReturn(false)

        handleSkipFatigueExtra(bundle, settingsService)

        Mockito.verify(settingsService).isSkipFatigueEnabled = false
    }

    @Test
    fun testHandleSkipFatigueExtra_noValuePassed() {
        val bundle = Mockito.mock(Bundle::class.java)
        val settingsService = Mockito.mock(SettingsService::class.java)
        Mockito.`when`(bundle.containsKey(AttentiveSettingsService.EXTRA_SET_SKIP_FATIGUE))
            .thenReturn(false)

        handleSkipFatigueExtra(bundle, settingsService)

        Mockito.verify(settingsService, Mockito.times(0)).isSkipFatigueEnabled = false
    }

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