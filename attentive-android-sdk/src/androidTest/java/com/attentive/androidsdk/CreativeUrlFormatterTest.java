package com.attentive.androidsdk;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.attentive.androidsdk.internal.util.CreativeUrlFormatter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class CreativeUrlFormatterTest {
    private static String DOMAIN = "testDomain";

    private AttentiveConfig attentiveConfig;
    private CreativeUrlFormatter creativeUrlBuilder;

    @Before
    public void setup() {
        attentiveConfig = mock(AttentiveConfig.class);
        when(attentiveConfig.getDomain()).thenReturn(DOMAIN);
        when(attentiveConfig.getMode()).thenReturn(AttentiveConfig.Mode.PRODUCTION);
        when(attentiveConfig.getUserIdentifiers()).thenReturn(new UserIdentifiers.Builder().build());

        creativeUrlBuilder = new CreativeUrlFormatter(new ObjectMapper());
    }

    @Test
    public void buildCompanyCreativeUrl_productionMode_buildsProdUrl() {
        String url = creativeUrlBuilder.buildCompanyCreativeUrl(attentiveConfig);

        assertEquals("https://creatives.attn.tv/mobile-apps/index.html?domain=testDomain", url);
    }

    @Test
    public void buildCompanyCreativeUrl_DebugMode_buildsDebugUrl() {
        when(attentiveConfig.getMode()).thenReturn(AttentiveConfig.Mode.DEBUG);

        String url = creativeUrlBuilder.buildCompanyCreativeUrl(attentiveConfig);

        assertEquals("https://creatives.attn.tv/mobile-apps/index.html?domain=testDomain&debug=matter-trip-grass-symbol", url);
    }

    @Test
    public void buildCompanyCreativeUrl_withUserIdentifiers_buildsUrlWithIdentifierQueryParams() {

        UserIdentifiers userIdentifiers = new UserIdentifiers.Builder()
                .withVisitorId("visitorId")
                .withClientUserId("clientId")
                .withPhone("+14156667777")
                .withEmail("email@gmail.com")
                .withShopifyId("12345")
                .withKlaviyoId("54321")
                .withCustomIdentifiers(Map.of("key1", "value1", "key2", "value2"))
                .build();
        when(attentiveConfig.getUserIdentifiers()).thenReturn(userIdentifiers);

        String url = creativeUrlBuilder.buildCompanyCreativeUrl(attentiveConfig);

        assertEquals("https://creatives.attn.tv/mobile-apps/index.html?domain=testDomain&vid=visitorId&cuid=clientId&p=%2B14156667777&e=email%40gmail.com&kid=54321&sid=12345&cstm=%7B%22key1%22%3A%22value1%22%2C%22key2%22%3A%22value2%22%7D", url);
    }

    @Test
    public void buildCompanyCreativeUrl_customIdentifiersCannotBeSerialized_doesNotThrow() throws JsonProcessingException {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(any(Object.class))).thenThrow(JsonProcessingException.class);

        creativeUrlBuilder = new CreativeUrlFormatter(objectMapper);

        UserIdentifiers userIdentifiers = new UserIdentifiers.Builder()
                .withCustomIdentifiers(Map.of("badFormatKey", "badFormatValue"))
                .build();
        when(attentiveConfig.getUserIdentifiers()).thenReturn(userIdentifiers);

        String url = creativeUrlBuilder.buildCompanyCreativeUrl(attentiveConfig);

        // assert custom identifiers set to {} and no error is thrown
        assertEquals("https://creatives.attn.tv/mobile-apps/index.html?domain=testDomain&cstm=%7B%7D", url);
    }
}
