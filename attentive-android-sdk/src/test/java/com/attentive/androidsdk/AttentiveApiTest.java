package com.attentive.androidsdk;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.attentive.androidsdk.events.Cart;
import com.attentive.androidsdk.events.Item;
import com.attentive.androidsdk.events.Order;
import com.attentive.androidsdk.events.Price;
import com.attentive.androidsdk.events.PurchaseEvent;
import com.attentive.androidsdk.internal.network.PurchaseMetadataDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.matchers.Or;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class AttentiveApiTest {
    private static final String DOMAIN = "adj";
    private static final String GEO_ADJUSTED_DOMAIN = "domainAdj";
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
    public void sendEvent_purchaseEventWithOnlyRequiredParams_callsOkHttpClientWithCorrectPayload() throws JsonProcessingException {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully();
        givenOkHttpClientReturnsSuccessFromEventsEndpoint();
        PurchaseEvent purchaseEvent = buildPurchaseEventWithRequiredFields();

        // Act
        attentiveApi.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN);

        // Assert
        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(okHttpClient).newCall(requestArgumentCaptor.capture());
        HttpUrl url = requestArgumentCaptor.getValue().url();

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
        verify(okHttpClient).newCall(requestArgumentCaptor.capture());
        HttpUrl url = requestArgumentCaptor.getValue().url();

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

    private PurchaseEvent buildPurchaseEventWithRequiredFields() {
        return new PurchaseEvent.Builder(List.of(new Item.Builder("11", "22", new Price.Builder(new BigDecimal("15.99"), Currency.getInstance("USD")).build()).build()), new Order.Builder("5555").build()).build();
    }

    private PurchaseEvent buildPurchaseEventWithAllFields() {
        return new PurchaseEvent.Builder(List.of(new Item.Builder("11", "22", new Price.Builder(new BigDecimal("15.99"), Currency.getInstance("USD")).build()).category("categoryValue").name("nameValue").productImage("imageUrl").build()), new Order.Builder("5555").build()).cart(new Cart.Builder().cartCoupon("cartCoupon").cartId("cartId").build()).build();
    }

    private static UserIdentifiers buildAllUserIdentifiers() {
        return new UserIdentifiers.Builder().withClientUserId("someClientUserId").withPhone("+15556667777").withEmail("Youknow@email.com").withShopifyId("someShopifyId").withKlaviyoId("someKlaviyoId").withVisitorId("someVisitorId").build();
    }

    private void givenOkHttpClientReturnsSuccessFromEventsEndpoint() {
        Call call = mock(Call.class);
        doReturn(call).when(okHttpClient).newCall(any());
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Callback argument = invocation.getArgument(0, Callback.class);
                argument.onResponse(call, buildSuccessfulResponse());
                return null;
            }
        }).when(call).enqueue(any());
    }

    private Response buildSuccessfulResponse() {
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