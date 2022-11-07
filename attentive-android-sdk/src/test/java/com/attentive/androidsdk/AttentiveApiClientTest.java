package com.attentive.androidsdk;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

public class AttentiveApiClientTest {

    @Test
    public void collectUserIdentifiers() {
        HttpClient httpClient = new HttpClient();
        AttentiveApiClient attentiveApiClient = new AttentiveApiClient(httpClient, new ObjectMapper());
        attentiveApiClient.collectUserIdentifiers("eekca", new UserIdentifiers.Builder("myApp1111").withEmail("wyattapp1111@gmail.com").withShopifyId("wyattshopify1111").build());
    }
}