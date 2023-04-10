package com.attentive.androidsdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.attentive.androidsdk.events.AddToCartEvent;
import com.attentive.androidsdk.events.Cart;
import com.attentive.androidsdk.events.CustomEvent;
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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;


public class AttentiveApiTestIT {
    private static final String DOMAIN = "mobileapps";
    // Update this accordingly when running on VPN
    private static final String GEO_ADJUSTED_DOMAIN = "mobileapps";
    private static final int EVENT_SEND_TIMEOUT_MS = 5000;
    private static final UserIdentifiers ALL_USER_IDENTIFIERS = buildAllUserIdentifiers();

    private CountDownLatch countDownLatch;
    private ObjectMapper objectMapper;
    private OkHttpClient okHttpClient;
    private AttentiveApi attentiveApi;
    private AttentiveApiCallback attentiveApiCallback;

    private final ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);


    @Before
    public void setup() {
        countDownLatch = new CountDownLatch(1);
        okHttpClient = spy(new OkHttpClient());
        objectMapper = new ObjectMapper();
        attentiveApi = new AttentiveApi(okHttpClient, objectMapper);
        attentiveApiCallback = new AttentiveApiCallback() {
            @Override
            public void onFailure(String message) {}

            @Override
            public void onSuccess() {
                countDownLatch.countDown();
            }
        };
    }

    @Test
    public void sendUserIdentifiersCollectedEvent_userIdentifierCollectedWithAllParams_sendsCorrectUserIdentifierCollectedEvent() throws InterruptedException, JsonProcessingException {
        // Act
        attentiveApi.sendUserIdentifiersCollectedEvent(DOMAIN, ALL_USER_IDENTIFIERS, attentiveApiCallback);
        countDownLatch.await(EVENT_SEND_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // Assert
        verify(okHttpClient, times(2)).newCall(requestArgumentCaptor.capture());
        Optional<Request> uicRequest = requestArgumentCaptor.getAllValues().stream().filter(request -> request.url().toString().contains("t=idn")).findFirst();
        assertTrue(uicRequest.isPresent());
        HttpUrl url = uicRequest.get().url();

        Metadata m = objectMapper.readValue(url.queryParameter("m"), Metadata.class);
        verifyCommonEventFields(url, "idn", m);

        assertEquals("[{\"id\":\"someClientUserId\",\"vendor\":\"2\"},{\"id\":\"someShopifyId\",\"vendor\":\"0\"},{\"id\":\"someKlaviyoId\",\"vendor\":\"1\"},{\"id\":\"value1\",\"name\":\"key1\",\"vendor\":\"6\"},{\"id\":\"value2\",\"name\":\"key2\",\"vendor\":\"6\"}]", url.queryParameter("evs"));
    }

    @Test
    public void sendEvent_purchaseEventWithAllParams_sendsCorrectPurchaseAndOrderConfirmedEvents() throws JsonProcessingException, InterruptedException {
        // Arrange
        PurchaseEvent purchaseEvent = buildPurchaseEventWithAllFields();

        // Act
        attentiveApi.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN, attentiveApiCallback);
        countDownLatch.await(EVENT_SEND_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // Assert
        verify(okHttpClient, times(3)).newCall(requestArgumentCaptor.capture());


        // Verify Purchase event
        Optional<Request> purchaseRequest = requestArgumentCaptor.getAllValues().stream().filter(request -> request.url().toString().contains("t=p")).findFirst();
        assertTrue(purchaseRequest.isPresent());
        HttpUrl purchaseUrl = purchaseRequest.get().url();

        PurchaseMetadataDto m = objectMapper.readValue(purchaseUrl.queryParameter("m"), PurchaseMetadataDto.class);
        verifyCommonEventFields(purchaseUrl, "p", m);

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


        // Verify Order Confirmed event
        Optional<Request> orderConfirmedRequest = requestArgumentCaptor.getAllValues().stream().filter(request -> request.url().toString().contains("t=oc")).findFirst();
        assertTrue(orderConfirmedRequest.isPresent());
        HttpUrl orderConfirmedUrl = orderConfirmedRequest.get().url();

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Metadata metadata = objectMapper.readValue(orderConfirmedUrl.queryParameter("m"), Metadata.class);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        verifyCommonEventFields(orderConfirmedUrl, "oc", metadata);

        // Can't convert directly to a OrderConfirmedMetadataDto because of the special serialization for products
        Map<String, Object> ocMetadata = objectMapper.readValue(orderConfirmedUrl.queryParameter("m"), Map.class);

        assertEquals(purchaseEvent.getOrder().getOrderId(), ocMetadata.get("orderId"));
        Item expectedItem = purchaseEvent.getItems().get(0);
        assertEquals(expectedItem.getPrice().getPrice().toString(), ocMetadata.get("cartTotal"));
        assertEquals(expectedItem.getPrice().getCurrency().getCurrencyCode(), ocMetadata.get("currency"));

        List<ProductDto> products = Arrays.asList((ProductDto[]) objectMapper.readValue((String) ocMetadata.get("products"), ProductDto[].class));

        assertEquals(1, products.size());
        assertEquals(expectedItem.getPrice().getPrice().toString(), products.get(0).getPrice());
        assertEquals(expectedItem.getProductId(), products.get(0).getProductId());
        assertEquals(expectedItem.getProductVariantId(), products.get(0).getSubProductId());
        assertEquals(expectedItem.getCategory(), products.get(0).getCategory());
        assertEquals(expectedItem.getProductImage(), products.get(0).getImage());
    }

    @Test
    public void sendEvent_productViewEventWithAllParams_sendsCorrectProductViewEvent() throws JsonProcessingException, InterruptedException {
        // Arrange
        ProductViewEvent productViewEvent = buildProductViewEventWithAllFields();

        // Act
        attentiveApi.sendEvent(productViewEvent, ALL_USER_IDENTIFIERS, DOMAIN);
        countDownLatch.await(EVENT_SEND_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // Assert
        verify(okHttpClient, times(2)).newCall(requestArgumentCaptor.capture());

        Optional<Request> addToCartRequest = requestArgumentCaptor.getAllValues().stream().filter(request -> request.url().toString().contains("t=d")).findFirst();
        assertTrue(addToCartRequest.isPresent());
        HttpUrl url = addToCartRequest.get().url();

        ProductViewMetadataDto m = objectMapper.readValue(url.queryParameter("m"), ProductViewMetadataDto.class);
        verifyCommonEventFields(url, "d", m);

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
    public void sendEvent_addToCartEventWithAllParams_sendsCorrectAddToCartEvent() throws JsonProcessingException, InterruptedException {
        // Arrange
        AddToCartEvent addToCartEvent = buildAddToCartEventWithAllFields();

        // Act
        attentiveApi.sendEvent(addToCartEvent, ALL_USER_IDENTIFIERS, DOMAIN);
        countDownLatch.await(EVENT_SEND_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // Assert
        verify(okHttpClient, times(2)).newCall(requestArgumentCaptor.capture());
        Optional<Request> addToCartRequest = requestArgumentCaptor.getAllValues().stream().filter(request -> request.url().toString().contains("t=c")).findFirst();
        assertTrue(addToCartRequest.isPresent());
        HttpUrl url = addToCartRequest.get().url();

        AddToCartMetadataDto m = objectMapper.readValue(url.queryParameter("m"), AddToCartMetadataDto.class);
        verifyCommonEventFields(url, "c", m);

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
    public void sendEvent_customEventWithAllParams_sendsCorrectCustomEvent() throws JsonProcessingException, InterruptedException {
        // Arrange
        CustomEvent customEvent = buildCustomEventWithAllFields();

        // Act
        attentiveApi.sendEvent(customEvent, ALL_USER_IDENTIFIERS, DOMAIN);
        countDownLatch.await(EVENT_SEND_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // Assert
        verify(okHttpClient, times(2)).newCall(requestArgumentCaptor.capture());
        Optional<Request> customEventRequest = requestArgumentCaptor.getAllValues().stream().filter(request -> request.url().toString().contains("t=ce")).findFirst();
        assertTrue(customEventRequest.isPresent());
        HttpUrl customEventUrl = customEventRequest.get().url();

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Metadata metadata = objectMapper.readValue(customEventUrl.queryParameter("m"), Metadata.class);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        verifyCommonEventFields(customEventUrl,"ce", metadata);

        // Can't convert directly to a CustomEventMetadataDto because of the special serialization for properties
        Map<String, Object> customEventMetadata = objectMapper.readValue(customEventUrl.queryParameter("m"), Map.class);

        assertEquals(customEvent.getType(), customEventMetadata.get("type"));
        Map<String, String> properties = objectMapper.readValue((String)customEventMetadata.get("properties"), Map.class);
        assertEquals(customEvent.getProperties(), properties);
    }

    private static void verifyCommonEventFields(HttpUrl url, String eventType, Metadata m) {
        assertEquals("modern", url.queryParameter("tag"));
        assertEquals("mobile-app", url.queryParameter("v"));
        assertEquals("0", url.queryParameter("lt"));
        assertEquals(GEO_ADJUSTED_DOMAIN, url.queryParameter("c"));
        assertEquals(eventType, url.queryParameter("t"));
        assertEquals(ALL_USER_IDENTIFIERS.getVisitorId(), url.queryParameter("u"));

        assertEquals(ALL_USER_IDENTIFIERS.getPhone(), m.getPhone());
        assertEquals(ALL_USER_IDENTIFIERS.getEmail(), m.getEmail());
        assertEquals("msdk", m.getSource());
    }

    private static UserIdentifiers buildAllUserIdentifiers() {
        return new UserIdentifiers.Builder()
                .withClientUserId("someClientUserId")
                .withPhone("+14156667777")
                .withEmail("email@gmail.com")
                .withShopifyId("someShopifyId")
                .withKlaviyoId("someKlaviyoId")
                .withCustomIdentifiers(Map.of("key1", "value1", "key2", "value2"))
                .build();
    }

    private static PurchaseEvent buildPurchaseEventWithAllFields() {
        return new PurchaseEvent.Builder(
                List.of(buildItemWithAllFields()),
                new Order.Builder("5555").build()
        )
                .cart(new Cart.Builder().cartCoupon("cartCoupon").cartId("cartId").build())
                .build();
    }

    private static AddToCartEvent buildAddToCartEventWithAllFields() {
        return new AddToCartEvent.Builder(List.of(buildItemWithAllFields())).build();
    }

    private static ProductViewEvent buildProductViewEventWithAllFields() {
        return new ProductViewEvent.Builder(List.of(buildItemWithAllFields())).build();
    }

    private static Item buildItemWithAllFields() {
        return new Item.Builder(
                "11",
                "22",
                new Price.Builder(new BigDecimal("15.99"), Currency.getInstance("USD")).build()
        )
                .category("categoryValue")
                .name("nameValue")
                .productImage("imageUrl")
                .build();
    }

    private static CustomEvent buildCustomEventWithAllFields() {
        return new CustomEvent.Builder("typeValue", Map.of("propertyKey1", "propertyValue1")).build();
    }
}
