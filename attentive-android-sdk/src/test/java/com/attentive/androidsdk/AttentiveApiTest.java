package com.attentive.androidsdk;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.util.Log;

import com.attentive.androidsdk.events.AddToCartEvent;
import com.attentive.androidsdk.events.Cart;
import com.attentive.androidsdk.events.Item;
import com.attentive.androidsdk.events.Order;
import com.attentive.androidsdk.events.Price;
import com.attentive.androidsdk.events.ProductViewEvent;
import com.attentive.androidsdk.events.PurchaseEvent;
import com.attentive.androidsdk.internal.network.AddToCartMetadataDto;
import com.attentive.androidsdk.internal.network.Metadata;
import com.attentive.androidsdk.internal.network.ProductDto;
import com.attentive.androidsdk.internal.network.ProductViewMetadataDto;
import com.attentive.androidsdk.internal.network.PurchaseMetadataDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

public class AttentiveApiTest {
    private static final String DOMAIN = "adj";
    private static final String GEO_ADJUSTED_DOMAIN = "domainAdj";
    private static final String DTAG_URL = String.format(AttentiveApi.ATTENTIVE_DTAG_URL, DOMAIN);
    private static final UserIdentifiers ALL_USER_IDENTIFIERS = buildAllUserIdentifiers();

    private AttentiveApi attentiveApi;
    private OkHttpClient okHttpClient;
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        okHttpClient = mock(OkHttpClient.class);

        objectMapper = new ObjectMapper();

        attentiveApi = spy(new AttentiveApi(okHttpClient, objectMapper));
    }

    @Test
    public void sendUserIdentifiersCollectedEvent_userIdentifierCollectedEvent_callsOkHttpClientWithCorrectPayload() throws JsonProcessingException {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully();
        givenOkHttpClientReturnsSuccessFromEventsEndpoint();

        // Act
        attentiveApi.sendUserIdentifiersCollectedEvent(DOMAIN, ALL_USER_IDENTIFIERS, new AttentiveApiCallback() {
            private static final String tag = "AttentiveApiTest";

            @Override
            public void onFailure(String message) {
                Log.e(tag, "Could not send the user identifiers. Error: " + message);
            }

            @Override
            public void onSuccess() {
                Log.i(tag, "Successfully sent the user identifiers");
            }
        });

        // Assert
        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(okHttpClient, times(1)).newCall(requestArgumentCaptor.capture());
        Optional<Request> userIdentifiersRequest = requestArgumentCaptor.getAllValues().stream().findFirst();
        assertTrue(userIdentifiersRequest.isPresent());
        HttpUrl url = userIdentifiersRequest.get().url();

        assertEquals("mobile-app", url.queryParameter("v"));
        assertEquals(GEO_ADJUSTED_DOMAIN, url.queryParameter("c"));
        assertEquals("0", url.queryParameter("lt"));
        assertEquals(ALL_USER_IDENTIFIERS.getVisitorId(), url.queryParameter("u"));
        assertEquals("idn", url.queryParameter("t"));
        assertEquals("[{\"vendor\":\"2\",\"id\":\"someClientUserId\"},{\"vendor\":\"0\",\"id\":\"someShopifyId\"},{\"vendor\":\"1\",\"id\":\"someKlaviyoId\"}]", url.queryParameter("evs"));

        Metadata m = objectMapper.readValue(url.queryParameter("m"), Metadata.class);
        assertEquals(ALL_USER_IDENTIFIERS.getPhone(), m.getPhone());
        assertEquals(ALL_USER_IDENTIFIERS.getEmail(), m.getEmail());
        assertEquals("msdk", m.getSource());
    }

    @Test
    public void sendEvent_purchaseEventWithOnlyRequiredParams_callsOkHttpClientWithCorrectPayload() throws JsonProcessingException {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully();
        givenOkHttpClientReturnsSuccessFromEventsEndpoint();
        PurchaseEvent purchaseEvent = buildPurchaseEventWithRequiredFields();

        // Act
        attentiveApi.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN);

        // Assert
        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(okHttpClient, times(2)).newCall(requestArgumentCaptor.capture());
        Optional<Request> purchaseRequest = requestArgumentCaptor.getAllValues().stream().filter(request -> request.url().toString().contains("t=p")).findFirst();
        assertTrue(purchaseRequest.isPresent());
        HttpUrl url = purchaseRequest.get().url();

        assertEquals("modern", url.queryParameter("tag"));
        assertEquals("mobile-app", url.queryParameter("v"));
        assertEquals("0", url.queryParameter("lt"));
        assertEquals(GEO_ADJUSTED_DOMAIN, url.queryParameter("c"));
        assertEquals("p", url.queryParameter("t"));
        assertEquals(ALL_USER_IDENTIFIERS.getVisitorId(), url.queryParameter("u"));

        PurchaseMetadataDto m = objectMapper.readValue(url.queryParameter("m"), PurchaseMetadataDto.class);
        assertEquals("USD", m.getCurrency());
        Item purchasedItem = purchaseEvent.getItems().get(0);
        assertEquals(purchasedItem.getPrice().getPrice().toString(), m.getPrice());
        assertEquals(purchasedItem.getProductId(), m.getProductId());
        assertEquals(purchasedItem.getProductVariantId(), m.getSubProductId());
        assertEquals(purchaseEvent.getOrder().getOrderId(), m.getOrderId());
    }

    @Test
    public void sendEvent_purchaseEventWithAllParams_callsOkHttpClientWithCorrectPayload() throws JsonProcessingException {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully();
        givenOkHttpClientReturnsSuccessFromEventsEndpoint();
        PurchaseEvent purchaseEvent = buildPurchaseEventWithAllFields();

        // Act
        attentiveApi.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN);

        // Assert
        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(okHttpClient, times(2)).newCall(requestArgumentCaptor.capture());
        Optional<Request> purchaseRequest = requestArgumentCaptor.getAllValues().stream().filter(request -> request.url().toString().contains("t=p")).findFirst();
        assertTrue(purchaseRequest.isPresent());
        HttpUrl url = purchaseRequest.get().url();

        assertEquals("modern", url.queryParameter("tag"));
        assertEquals("mobile-app", url.queryParameter("v"));
        assertEquals("0", url.queryParameter("lt"));
        assertEquals(GEO_ADJUSTED_DOMAIN, url.queryParameter("c"));
        assertEquals("p", url.queryParameter("t"));
        assertEquals(ALL_USER_IDENTIFIERS.getVisitorId(), url.queryParameter("u"));

        PurchaseMetadataDto m = objectMapper.readValue(url.queryParameter("m"), PurchaseMetadataDto.class);
        assertEquals("USD", m.getCurrency());
        Item purchasedItem = purchaseEvent.getItems().get(0);
        assertEquals(purchasedItem.getPrice().getPrice().toString(), m.getPrice());
        assertEquals(purchasedItem.getProductId(), m.getProductId());
        assertEquals(purchasedItem.getProductImage(), m.getImage());
        assertEquals(purchasedItem.getName(), m.getName());
        assertEquals(String.valueOf(purchasedItem.getQuantity()), m.getQuantity());
        assertEquals(purchasedItem.getCategory(), m.getCategory());
        assertEquals(purchasedItem.getProductVariantId(), m.getSubProductId());

        assertEquals(purchaseEvent.getCart().getCartId(), m.getCartId());
        assertEquals(purchaseEvent.getCart().getCartCoupon(), m.getCartCoupon());
        assertEquals(purchaseEvent.getOrder().getOrderId(), m.getOrderId());
    }

    @Test
    public void sendEvent_purchaseEventWithAllParams_sendsCorrectOrderConfirmedEvent() throws JsonProcessingException {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully();
        givenOkHttpClientReturnsSuccessFromEventsEndpoint();
        PurchaseEvent purchaseEvent = buildPurchaseEventWithAllFields();

        // Act
        attentiveApi.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN);

        // Assert
        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(okHttpClient, times(2)).newCall(requestArgumentCaptor.capture());
        Optional<Request> orderConfirmedRequest = requestArgumentCaptor.getAllValues().stream().filter(request -> request.url().toString().contains("t=oc")).findFirst();
        assertTrue(orderConfirmedRequest.isPresent());
        HttpUrl url = orderConfirmedRequest.get().url();

        assertEquals("modern", url.queryParameter("tag"));
        assertEquals("mobile-app", url.queryParameter("v"));
        assertEquals("0", url.queryParameter("lt"));
        assertEquals(GEO_ADJUSTED_DOMAIN, url.queryParameter("c"));
        assertEquals("oc", url.queryParameter("t"));
        assertEquals(ALL_USER_IDENTIFIERS.getVisitorId(), url.queryParameter("u"));

        String metadataString = url.queryParameter("m");
        Map<String, Object> metadata = objectMapper.readValue(metadataString, Map.class);
        assertEquals(purchaseEvent.getOrder().getOrderId(), metadata.get("orderId"));
        Item expectedItem = purchaseEvent.getItems().get(0);
        assertEquals(expectedItem.getPrice().getPrice().toString(), metadata.get("cartTotal"));
        assertEquals(expectedItem.getPrice().getCurrency().getCurrencyCode(), metadata.get("currency"));

        List<ProductDto> products = Arrays.asList((ProductDto[])objectMapper.readValue((String)metadata.get("products"), ProductDto[].class));
        assertEquals(1, products.size());
        assertEquals(expectedItem.getPrice().getPrice().toString(), products.get(0).getPrice());
        assertEquals(expectedItem.getProductId(), products.get(0).getProductId());
        assertEquals(expectedItem.getProductVariantId(), products.get(0).getSubProductId());
        assertEquals(expectedItem.getCategory(), products.get(0).getCategory());
        assertEquals(expectedItem.getProductImage(), products.get(0).getImage());
    }

    @Test
    public void sendEvent_purchaseEventWithTwoProducts_callsEventsEndpointTwiceForPurchasesAndOnceForOrderConfirmed() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully();
        givenOkHttpClientReturnsSuccessFromEventsEndpoint();
        PurchaseEvent purchaseEvent = buildPurchaseEventWithTwoItems();

        // Act
        attentiveApi.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN);

        // Assert
        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(okHttpClient, times(3)).newCall(requestArgumentCaptor.capture());
        List<Request> allValues = new ArrayList<Request>(requestArgumentCaptor.getAllValues());
        assertEquals(3, allValues.size());

        int purchaseCount = 0;
        int orderConfirmedCount= 0;
        for (Request request : allValues) {
            final String urlString = request.url().toString();
            if (urlString.contains("t=p")) {
                purchaseCount++;
            } else if (urlString.contains("t=oc")) {
                orderConfirmedCount++;
            } else {
                fail("Unknown event type was sent to the server");
            }
        }
        assertEquals(2, purchaseCount);
        assertEquals(1, orderConfirmedCount);
    }

    @Test
    public void sendEvent_addToCartEventWithAllParams_callsOkHttpClientWithCorrectPayload() throws JsonProcessingException {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully();
        givenOkHttpClientReturnsSuccessFromEventsEndpoint();
        AddToCartEvent addToCartEvent = buildAddToCartEventWithAllFields();

        // Act
        attentiveApi.sendEvent(addToCartEvent, ALL_USER_IDENTIFIERS, DOMAIN);

        // Assert
        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(okHttpClient, times(1)).newCall(requestArgumentCaptor.capture());
        Optional<Request> addToCartRequest = requestArgumentCaptor.getAllValues().stream().filter(request -> request.url().toString().contains("t=c")).findFirst();
        assertTrue(addToCartRequest.isPresent());
        HttpUrl url = addToCartRequest.get().url();

        assertEquals("modern", url.queryParameter("tag"));
        assertEquals("mobile-app", url.queryParameter("v"));
        assertEquals("0", url.queryParameter("lt"));
        assertEquals(GEO_ADJUSTED_DOMAIN, url.queryParameter("c"));
        assertEquals("c", url.queryParameter("t"));
        assertEquals(ALL_USER_IDENTIFIERS.getVisitorId(), url.queryParameter("u"));

        AddToCartMetadataDto m = objectMapper.readValue(url.queryParameter("m"), AddToCartMetadataDto.class);
        assertEquals("USD", m.getCurrency());
        Item addToCartItem = addToCartEvent.getItems().get(0);
        assertEquals(addToCartItem.getPrice().getPrice().toString(), m.getPrice());
        assertEquals(addToCartItem.getProductId(), m.getProductId());
        assertEquals(addToCartItem.getProductImage(), m.getImage());
        assertEquals(addToCartItem.getName(), m.getName());
        assertEquals(String.valueOf(addToCartItem.getQuantity()), m.getQuantity());
        assertEquals(addToCartItem.getCategory(), m.getCategory());
        assertEquals(addToCartItem.getProductVariantId(), m.getSubProductId());
    }

    @Test
    public void sendEvent_productViewEventWithAllParams_callsOkHttpClientWithCorrectPayload() throws JsonProcessingException {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully();
        givenOkHttpClientReturnsSuccessFromEventsEndpoint();
        ProductViewEvent productViewEvent = buildProductViewEventWithAllFields();

        // Act
        attentiveApi.sendEvent(productViewEvent, ALL_USER_IDENTIFIERS, DOMAIN);

        // Assert
        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(okHttpClient, times(1)).newCall(requestArgumentCaptor.capture());
        Optional<Request> addToCartRequest = requestArgumentCaptor.getAllValues().stream().filter(request -> request.url().toString().contains("t=d")).findFirst();
        assertTrue(addToCartRequest.isPresent());
        HttpUrl url = addToCartRequest.get().url();

        assertEquals("modern", url.queryParameter("tag"));
        assertEquals("mobile-app", url.queryParameter("v"));
        assertEquals("0", url.queryParameter("lt"));
        assertEquals(GEO_ADJUSTED_DOMAIN, url.queryParameter("c"));
        assertEquals("d", url.queryParameter("t"));
        assertEquals(ALL_USER_IDENTIFIERS.getVisitorId(), url.queryParameter("u"));

        ProductViewMetadataDto m = objectMapper.readValue(url.queryParameter("m"), ProductViewMetadataDto.class);
        assertEquals("USD", m.getCurrency());
        Item addToCartItem = productViewEvent.getItems().get(0);
        assertEquals(addToCartItem.getPrice().getPrice().toString(), m.getPrice());
        assertEquals(addToCartItem.getProductId(), m.getProductId());
        assertEquals(addToCartItem.getProductImage(), m.getImage());
        assertEquals(addToCartItem.getName(), m.getName());
        assertEquals(addToCartItem.getCategory(), m.getCategory());
        assertEquals(addToCartItem.getProductVariantId(), m.getSubProductId());
    }

    @Test
    public void sendEvent_multipleEvents_onlyGetsGeoAdjustedDomainOnce() throws JsonProcessingException {
        // Arrange
        givenOkHttpClientReturnsGeoAdjustedDomainFromDtagEndpoint();
        givenOkHttpClientReturnsSuccessFromEventsEndpoint();
        ProductViewEvent productViewEvent = buildProductViewEventWithAllFields();

        // Act
        attentiveApi.sendEvent(productViewEvent, ALL_USER_IDENTIFIERS, DOMAIN);
        attentiveApi.sendEvent(productViewEvent, ALL_USER_IDENTIFIERS, DOMAIN);

        // Assert
        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(okHttpClient, times(3)).newCall(requestArgumentCaptor.capture());

        long eventsSentCount =
            requestArgumentCaptor.getAllValues().stream().filter(request -> request.url().toString().contains("t=d"))
                .count();
        assertEquals(2, eventsSentCount);

        long geoAdjustedDomainCallsSent =
            requestArgumentCaptor.getAllValues().stream().filter(request -> request.url().toString().equals(DTAG_URL))
                .count();
        assertEquals(1, geoAdjustedDomainCallsSent);
    }

    @Test
    public void sendEvent_geoAdjustedDomainRetrieved_domainValueIsCorrect() throws JsonProcessingException {
        // Arrange
        givenOkHttpClientReturnsGeoAdjustedDomainFromDtagEndpoint();
        givenOkHttpClientReturnsSuccessFromEventsEndpoint();
        ProductViewEvent productViewEvent = buildProductViewEventWithAllFields();

        assertNull(attentiveApi.getCachedGeoAdjustedDomain());

        // Act
        attentiveApi.sendEvent(productViewEvent, ALL_USER_IDENTIFIERS, DOMAIN);

        // Assert
        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(okHttpClient, times(2)).newCall(requestArgumentCaptor.capture());

        assertEquals(GEO_ADJUSTED_DOMAIN, attentiveApi.getCachedGeoAdjustedDomain());
    }

    private PurchaseEvent buildPurchaseEventWithRequiredFields() {
        return new PurchaseEvent.Builder(List.of(new Item.Builder("11", "22", new Price.Builder(new BigDecimal("15.99"), Currency.getInstance("USD")).build()).build()), new Order.Builder("5555").build()).build();
    }

    private PurchaseEvent buildPurchaseEventWithTwoItems() {
        return new PurchaseEvent.Builder(
            List.of(
                new Item.Builder("11", "22", new Price.Builder(new BigDecimal("15.99"), Currency.getInstance("USD")).build()).build(),
                new Item.Builder("77", "99", new Price.Builder(new BigDecimal("20.00"), Currency.getInstance("USD")).build()).build()),
            new Order.Builder("5555").build()).build();
    }

    private PurchaseEvent buildPurchaseEventWithAllFields() {
        return new PurchaseEvent.Builder(List.of(buildItemWithAllFields()), new Order.Builder("5555").build()).cart(new Cart.Builder().cartCoupon("cartCoupon").cartId("cartId").build()).build();
    }

    private AddToCartEvent buildAddToCartEventWithAllFields() {
        return new AddToCartEvent.Builder(List.of(buildItemWithAllFields())).build();
    }
    private ProductViewEvent buildProductViewEventWithAllFields() {
        return new ProductViewEvent.Builder(List.of(buildItemWithAllFields())).build();
    }

    private Item buildItemWithAllFields() {
        return new Item.Builder("11", "22", new Price.Builder(new BigDecimal("15.99"), Currency.getInstance("USD")).build()).category("categoryValue").name("nameValue").productImage("imageUrl").build();
    }

    private static UserIdentifiers buildAllUserIdentifiers() {
        return new UserIdentifiers.Builder().withClientUserId("someClientUserId").withPhone("+15556667777").withEmail("Youknow@email.com").withShopifyId("someShopifyId").withKlaviyoId("someKlaviyoId").withVisitorId("someVisitorId").build();
    }

    private void givenOkHttpClientReturnsSuccessFromEventsEndpoint() {
        Call call = mock(Call.class);
        doReturn(call).when(okHttpClient).newCall(argThat(request -> request.url().host().equals("events.attentivemobile.com")));
        doAnswer((Answer<Void>) invocation -> {
            Callback argument = invocation.getArgument(0, Callback.class);
            argument.onResponse(call, buildSuccessfulResponseMock());
            return null;
        }).when(call).enqueue(any());
    }

    private void givenOkHttpClientReturnsGeoAdjustedDomainFromDtagEndpoint() {
        Call call = mock(Call.class);
        doReturn(call).when(okHttpClient).newCall(argThat(request -> request.url().host().equals("cdn.attn.tv")));
        doAnswer((Answer<Void>) invocation -> {
            Callback argument = invocation.getArgument(0, Callback.class);
            ResponseBody responseBody =
                ResponseBody.create(String.format("window.__attentive_domain='%s.attn.tv'", GEO_ADJUSTED_DOMAIN), null);
            Response dtagResponse = buildSuccessfulResponseMock();
            doReturn(responseBody).when(dtagResponse).body();
            argument.onResponse(call, dtagResponse);
            return null;
        }).when(call).enqueue(any());
    }

    private Response buildSuccessfulResponseMock() {
        Response mock = mock(Response.class);
        doReturn(true).when(mock).isSuccessful();
        doReturn(200).when(mock).code();
        return mock;
    }

    private void givenAttentiveApiGetsGeoAdjustedDomainSuccessfully() {
        doAnswer((Answer<Void>) invocation -> {
            AttentiveApi.GetGeoAdjustedDomainCallback argument =
                invocation.getArgument(1, AttentiveApi.GetGeoAdjustedDomainCallback.class);
            argument.onSuccess(GEO_ADJUSTED_DOMAIN);
            return null;
        }).when(attentiveApi).getGeoAdjustedDomainAsync(eq(DOMAIN), any());
    }
}