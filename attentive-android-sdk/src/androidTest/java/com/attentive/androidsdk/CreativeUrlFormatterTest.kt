package com.attentive.androidsdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.internal.util.AppInfo.attentiveSDKVersion
import com.attentive.androidsdk.internal.util.CreativeUrlFormatter
import junit.framework.TestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.util.Map

@RunWith(AndroidJUnit4::class)
class CreativeUrlFormatterTest {
    lateinit var attentiveConfig: AttentiveConfigInterface
    private var creativeUrlBuilder: CreativeUrlFormatter? = null

    @Before
    fun setup() {
        attentiveConfig = Mockito.mock(AttentiveConfigInterface::class.java)
        Mockito.`when`(attentiveConfig.domain).thenReturn(DOMAIN)
        Mockito.`when`(attentiveConfig.mode).thenReturn(AttentiveConfig.Mode.PRODUCTION)
        Mockito.`when`(attentiveConfig.userIdentifiers)
            .thenReturn(UserIdentifiers.Builder().build())

        creativeUrlBuilder = CreativeUrlFormatter()
    }

    @Test
    fun buildCompanyCreativeUrl_productionMode_buildsProdUrl() {
        val url = creativeUrlBuilder!!.buildCompanyCreativeUrl(attentiveConfig!!, null)

        TestCase.assertEquals(
            BASE_TEST_URL + "&sdkVersion=" + attentiveSDKVersion + "&sdkName=attentive-android-sdk&skipFatigue=false",
            url
        )
    }

    @Test
    fun buildCompanyCreativeUrl_DebugMode_buildsDebugUrl() {
        Mockito.`when`(attentiveConfig!!.mode).thenReturn(AttentiveConfig.Mode.DEBUG)

        val url = creativeUrlBuilder!!.buildCompanyCreativeUrl(attentiveConfig!!, null)

        TestCase.assertEquals(
            BASE_TEST_URL + "&debug=matter-trip-grass-symbol&sdkVersion=" + attentiveSDKVersion + "&sdkName=attentive-android-sdk&skipFatigue=false",
            url
        )
    }

    @Test
    fun buildCompanyCreativeUrl_withUserIdentifiers_buildsUrlWithIdentifierQueryParams() {
        val userIdentifiers = UserIdentifiers.Builder()
            .withVisitorId("visitorId")
            .withClientUserId("clientId")
            .withPhone("+14156667777")
            .withEmail("email@gmail.com")
            .withShopifyId("12345")
            .withKlaviyoId("54321")
            .withCustomIdentifiers(Map.of("key1", "value1", "key2", "value2"))
            .build()
        Mockito.`when`(attentiveConfig!!.userIdentifiers).thenReturn(userIdentifiers)

        val url = creativeUrlBuilder!!.buildCompanyCreativeUrl(attentiveConfig!!, null)

        TestCase.assertEquals(
            BASE_TEST_URL + "&sdkVersion=" + attentiveSDKVersion + "&sdkName=attentive-android-sdk&skipFatigue=false&vid=visitorId&cuid=clientId&p=%2B14156667777&e=email%40gmail.com&kid=54321&sid=12345&cstm=%7B%22key1%22%3A%22value1%22%2C%22key2%22%3A%22value2%22%7D",
            url
        )
    }

    @Test
    fun buildCompanyCreativeUrl_productionMode_buildsProdUrl_withCreativeId() {
        val creativeId = "22233"
        val url = creativeUrlBuilder!!.buildCompanyCreativeUrl(attentiveConfig!!, creativeId)

        TestCase.assertEquals(
            BASE_TEST_URL + "&sdkVersion=" + attentiveSDKVersion + "&sdkName=attentive-android-sdk&skipFatigue=false&attn_creative_id=" + creativeId,
            url
        )
    }

    companion object {
        private const val BASE_TEST_URL =
            "https://creatives.attn.tv/mobile-apps/index.html?domain=testDomain"
        private const val DOMAIN = "testDomain"
    }
}
