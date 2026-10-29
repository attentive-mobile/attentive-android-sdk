package com.attentive.androidsdk.internal.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenUrlParserTest {
    private val parser = OpenUrlParser()

    @Test
    fun parse_allUtmKeys_extractsEach() {
        val opened =
            parser.parse(
                "https://shop.example.com/?utm_source=s&utm_medium=m&utm_campaign=c&utm_term=t&utm_content=x",
            )

        assertEquals(
            mapOf(
                UtmKey.SOURCE to "s",
                UtmKey.MEDIUM to "m",
                UtmKey.CAMPAIGN to "c",
                UtmKey.TERM to "t",
                UtmKey.CONTENT to "x",
            ),
            opened?.utms,
        )
    }

    @Test
    fun parse_encodedValues_decodes() {
        val opened = parser.parse("https://shop.example.com/?utm_campaign=fall%20sale&utm_source=a+b")

        assertEquals(mapOf(UtmKey.CAMPAIGN to "fall sale", UtmKey.SOURCE to "a b"), opened?.utms)
    }

    @Test
    fun parse_nonUtmAndEmptyParams_dropsThem() {
        val opened = parser.parse("https://shop.example.com/?email=a%40b.com&utm_source=&UTM_MEDIUM=x&phone=1")

        assertEquals(emptyMap<UtmKey, String>(), opened?.utms)
    }

    @Test
    fun parse_longValue_truncates() {
        val opened = parser.parse("https://shop.example.com/?utm_source=" + "a".repeat(300))

        assertEquals(256, opened?.utms?.getValue(UtmKey.SOURCE)?.length)
    }

    @Test
    fun parse_landingUrl_stripsQueryAndFragment() {
        assertEquals(
            "https://shop.example.com:8443/p/1",
            parser.parse("https://shop.example.com:8443/p/1?utm_source=a&secret=b#frag")?.landingUrl,
        )
    }

    @Test
    fun parse_opaqueUri_readsQuery() {
        val opened = parser.parse("myshop:product?utm_source=sms")

        assertEquals(LaunchSource.DEEP_LINK, opened?.source)
        assertEquals("myshop:product", opened?.landingUrl)
        assertEquals(mapOf(UtmKey.SOURCE to "sms"), opened?.utms)
    }

    @Test
    fun parse_relativeOrInvalid_returnsNull() {
        assertNull(parser.parse("/product/1?utm_source=a"))
        assertNull(parser.parse("not a url"))
    }

    @Test
    fun parse_customClassifier_isUsed() {
        val pushParser = OpenUrlParser { LaunchSource.PUSH }

        assertEquals(LaunchSource.PUSH, pushParser.parse("https://shop.example.com/")?.source)
    }
}
