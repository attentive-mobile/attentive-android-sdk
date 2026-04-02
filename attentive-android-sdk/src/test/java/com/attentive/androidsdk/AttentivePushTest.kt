package com.attentive.androidsdk

import android.content.Context
import android.content.pm.PackageManager
import com.attentive.androidsdk.push.AttentivePush
import com.attentive.androidsdk.tracking.AppLaunchTracker
import org.junit.Assert
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AttentivePushTest {
    @Mock
    private val context: Context = mock()

    @Mock
    private val pmMock: PackageManager = mock()

    @Mock
    private val packageName = "com.example.app"

    @Test
    fun testBuildLaunchIntentWithoutDeepLink() {
        whenever(context.packageManager).thenReturn(pmMock)
        whenever(context.packageName).thenReturn(packageName)
        val intent = AttentivePush.getInstance().buildLaunchIntent(context, mapOf())
        val launchedFromNotification = intent?.extras?.getBoolean(AppLaunchTracker.LAUNCHED_FROM_NOTIFICATION)
        Assert.assertEquals(launchedFromNotification, null)
    }

    @Test
    fun testBuildLaunchIntentWithDeepLink() {
        whenever(context.packageManager).thenReturn(pmMock)
        whenever(context.packageName).thenReturn(packageName)
        val intent = AttentivePush.getInstance().buildLaunchIntent(context, mapOf())
        Mockito.verify(pmMock, Mockito.times(1)).getLaunchIntentForPackage(packageName)
    }
}
