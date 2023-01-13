package com.attentive.androidsdk;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.annotation.VisibleForTesting;
import com.attentive.androidsdk.events.AddToCartEvent;
import com.attentive.androidsdk.events.Event;
import com.attentive.androidsdk.events.Item;
import com.attentive.androidsdk.events.ProductViewEvent;
import com.attentive.androidsdk.events.PurchaseEvent;
import com.attentive.androidsdk.internal.network.AddToCartMetadataDto;
import com.attentive.androidsdk.internal.network.Metadata;
import com.attentive.androidsdk.internal.network.OrderConfirmedMetadataDto;
import com.attentive.androidsdk.internal.network.ProductDto;
import com.attentive.androidsdk.internal.network.ProductViewMetadataDto;
import com.attentive.androidsdk.internal.network.PurchaseMetadataDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class AttentiveApi {
    private static final String ATTENTIVE_EVENTS_ENDPOINT_HOST = "events.attentivemobile.com";
    private static final String ATTENTIVE_DTAG_URL = "https://cdn.attn.tv/%s/dtag.js";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AttentiveApi(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    // TODO refactor to use the 'sendEvent' method
    public void sendUserIdentifiersCollectedEvent(String domain, UserIdentifiers userIdentifiers, AttentiveApiCallback callback) {
        // first get the geo-adjusted domain, and then call the events endpoint
        getGeoAdjustedDomainAsync(domain, new GetGeoAdjustedDomainCallback() {
            @Override
            public void onFailure(String reason) {
                callback.onFailure(reason);
            }

            @Override
            public void onSuccess(String geoAdjustedDomain) {
                internalSendUserIdentifiersCollectedEventAsync(geoAdjustedDomain, userIdentifiers, callback);
            }
        });
    }

    public void sendEvent(Event event, UserIdentifiers userIdentifiers, String domain) {
        sendEvent(event, userIdentifiers, domain, null);
    }

    public void sendEvent(Event event, UserIdentifiers userIdentifiers, String domain, @Nullable AttentiveApiCallback callback) {
        getGeoAdjustedDomainAsync(domain, new GetGeoAdjustedDomainCallback() {
            @Override
            public void onFailure(String reason) {
                Log.w(this.getClass().getName(), "Could not get geo-adjusted domain. Trying to use the original domain.");
                sendEvent(event, userIdentifiers, domain);
            }

            @Override
            public void onSuccess(String geoAdjustedDomain) {
                sendEvent(event, userIdentifiers, geoAdjustedDomain);
            }

            private void sendEvent(Event event, UserIdentifiers userIdentifiers, String domain) {
                sendEventInternalAsync(getEventRequestsFromEvent(event), userIdentifiers, domain, callback);
            }
        });
    }

    @VisibleForTesting
    interface GetGeoAdjustedDomainCallback {
        void onFailure(String reason);
        void onSuccess(String geoAdjustedDomain);
    }

    @VisibleForTesting
    void getGeoAdjustedDomainAsync(String domain, GetGeoAdjustedDomainCallback callback) {
        // TODO cache geo-adjusted domain
        final String url = String.format(ATTENTIVE_DTAG_URL, domain);
        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure("Getting geo-adjusted domain failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // Check explicitly for 200 (instead of response.isSuccessful()) because the response only has the tag in the body when its code is 200
                if (response.code() != 200) {
                    callback.onFailure(String.format("Getting geo-adjusted domain returned invalid code: '%d', message: '%s'", response.code(), response.message()));
                    return;
                }

                ResponseBody body = response.body();

                if (body == null) {
                    callback.onFailure("Getting geo-adjusted domain returned no body");
                    return;
                }

                String fullTag = response.body().string();
                String geoAdjustedDomain = parseAttentiveDomainFromTag(fullTag);

                if (geoAdjustedDomain == null) {
                    callback.onFailure("Could not parse the domain from the full tag");
                    return;
                }

                callback.onSuccess(geoAdjustedDomain);
            }
        });
    }

    // TODO replace with the generic 'sendEvent' code
    private void internalSendUserIdentifiersCollectedEventAsync(String geoAdjustedDomain, UserIdentifiers userIdentifiers, AttentiveApiCallback callback) {
        String externalVendorIdsJson = null;
        try {
            List<ExternalVendorId> externalVendorIds = buildExternalVendorIds(userIdentifiers);
            externalVendorIdsJson = objectMapper.writeValueAsString(externalVendorIds);
        } catch (JsonProcessingException e) {
            callback.onFailure(String.format("Could not serialize the UserIdentifiers. Message: '%s'", e.getMessage()));
            return;
        }

        String metadataJson = null;
        try {
            Metadata metadata = buildMetadata(userIdentifiers);
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            callback.onFailure(String.format("Could not serialize metadata. Message: '%s'", e.getMessage()));
            return;
        }

        HttpUrl.Builder urlBuilder = getHttpUrlEventsEndpointBuilder()
            .addQueryParameter("v", "mobile-app")
            .addQueryParameter("c", geoAdjustedDomain)
            .addQueryParameter("t", "idn")
            .addQueryParameter("evs", externalVendorIdsJson)
            .addQueryParameter("m", metadataJson)
            .addQueryParameter("lt", "0");

        if (userIdentifiers.getVisitorId() != null) {
            urlBuilder.addQueryParameter("u", userIdentifiers.getVisitorId());
        }

        HttpUrl url = urlBuilder.build();

        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(String.format("Error when calling the event endpoint: '%s'", e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    callback.onFailure(String.format("Invalid response code when calling the event endpoint: '%d', message: '%s'", response.code(), response.message()));
                    return;
                }

                callback.onSuccess();
            }
        });
    }

    private HttpUrl.Builder getHttpUrlEventsEndpointBuilder() {
        return new HttpUrl.Builder()
                .scheme("https")
                .host(ATTENTIVE_EVENTS_ENDPOINT_HOST)
                .addPathSegment("e");
    }

    @Nullable
    private String parseAttentiveDomainFromTag(String tag) {
        Pattern pattern = Pattern.compile("window.__attentive_domain='(.*?).attn.tv'");
        Matcher matcher = pattern.matcher(tag);
        if (matcher.find()) {
            if (matcher.groupCount() == 1) {
                return matcher.group(1);
            }
        }

        return null;
    }

    private Metadata buildMetadata(UserIdentifiers userIdentifiers) {
        Metadata metadata = new Metadata();

        if (userIdentifiers.getPhone() != null) {
            metadata.setPhone(userIdentifiers.getPhone());
        }

        if (userIdentifiers.getEmail() != null) {
            metadata.setEmail(userIdentifiers.getEmail());
        }

        return metadata;
    }

    private List<ExternalVendorId> buildExternalVendorIds(UserIdentifiers userIdentifiers) {
        List<ExternalVendorId> externalVendorIdList = new ArrayList<>();

        if (userIdentifiers.getClientUserId() != null) {
            externalVendorIdList.add(
                new ExternalVendorId() {{
                    setVendor(Vendor.CLIENT_USER);
                    setId(userIdentifiers.getClientUserId());
                }});
        }

        if (userIdentifiers.getShopifyId() != null) {
            externalVendorIdList.add(new ExternalVendorId() {{
                setVendor(Vendor.SHOPIFY);
                setId(userIdentifiers.getShopifyId());
            }});
        }

        if (userIdentifiers.getKlaviyoId() != null) {
            externalVendorIdList.add(new ExternalVendorId() {{
                setVendor(Vendor.KLAVIYO);
                setId(userIdentifiers.getKlaviyoId());
            }});
        }

        for (Map.Entry<String, String> customIdentifier : userIdentifiers.getCustomIdentifiers().entrySet()) {
            externalVendorIdList.add(new ExternalVendorId() {{
                setVendor(Vendor.CUSTOM_USER);
                setId(customIdentifier.getValue());
                setName(customIdentifier.getKey());
            }});
        }

        return externalVendorIdList;
    }

    private static class EventRequest {
        private enum Type {
            PURCHASE("p"),
            USER_IDENTIFIER_COLLECTED("idn"),
            ORDER_CONFIRMED("oc"),
            PRODUCT_VIEW("d"),
            ADD_TO_CART("c");

            private final String abbreviation;

            Type(String abbreviation) {
                this.abbreviation = abbreviation;
            }

            public String getAbbreviation() {
                return abbreviation;
            }
        }

        private final Metadata metadata;
        private final Type type;

        public EventRequest(Metadata metadata, Type type) {
            this.metadata = metadata;
            this.type = type;
        }

        public Metadata getMetadata() {
            return metadata;
        }

        public Type getType() {
            return type;
        }
    }

    // One event can produce multiple requests (e.g. one PurchaseEvent with multiple items should be broken into separate Purchase requests)
    private List<EventRequest> getEventRequestsFromEvent(Event event) {
        List<EventRequest> eventRequests = new ArrayList<>();
        if (event instanceof PurchaseEvent) {
            PurchaseEvent purchaseEvent = (PurchaseEvent) event;

            if (purchaseEvent.getItems().isEmpty()) {
                Log.w(this.getClass().getName(), "Purchase event has no items. Skipping.");
                return List.of();
            }

            BigDecimal cartTotal = BigDecimal.ZERO;
            for (Item item : purchaseEvent.getItems()) {
                cartTotal = cartTotal.add(item.getPrice().getPrice());
            }
            final String cartTotalString = cartTotal.setScale(2, RoundingMode.DOWN).toPlainString();

            // Create Purchase requests
            for (Item item : purchaseEvent.getItems()) {
                PurchaseMetadataDto purchaseMetadataDto = new PurchaseMetadataDto();
                purchaseMetadataDto.setCurrency(item.getPrice().getCurrency().getCurrencyCode());
                purchaseMetadataDto.setPrice(item.getPrice().getPrice().toPlainString());
                purchaseMetadataDto.setName(item.getName());
                purchaseMetadataDto.setImage(item.getProductImage());
                purchaseMetadataDto.setProductId(item.getProductId());
                purchaseMetadataDto.setSubProductId(item.getProductVariantId());
                purchaseMetadataDto.setCategory(item.getCategory());
                purchaseMetadataDto.setQuantity(String.valueOf(item.getQuantity()));
                purchaseMetadataDto.setOrderId(purchaseEvent.getOrder().getOrderId());
                purchaseMetadataDto.setCartTotal(cartTotalString);
                if (purchaseEvent.getCart() != null) {
                    purchaseMetadataDto.setCartId(purchaseEvent.getCart().getCartId());
                    purchaseMetadataDto.setCartCoupon(purchaseEvent.getCart().getCartCoupon());
                }
                eventRequests.add(new EventRequest(purchaseMetadataDto, EventRequest.Type.PURCHASE));
            }

            // Create OrderConfirmed request
            OrderConfirmedMetadataDto ocMetadata = new OrderConfirmedMetadataDto();
            ocMetadata.setOrderId(purchaseEvent.getOrder().getOrderId());
            ocMetadata.setCurrency(purchaseEvent.getItems().get(0).getPrice().getCurrency().getCurrencyCode());
            ocMetadata.setCartTotal(cartTotalString);
            List<ProductDto> products = new ArrayList<>();
            for (Item item : purchaseEvent.getItems()) {
                ProductDto product = new ProductDto();
                product.setProductId(item.getProductId());
                product.setSubProductId(item.getProductVariantId());
                product.setCurrency(item.getPrice().getCurrency().getCurrencyCode());
                product.setCategory(item.getCategory());
                product.setQuantity(String.valueOf(item.getQuantity()));
                product.setName(item.getName());
                product.setPrice(item.getPrice().getPrice().toPlainString());
                product.setImage(item.getProductImage());
                products.add(product);
            }
            ocMetadata.setProducts(products);
            eventRequests.add(new EventRequest(ocMetadata, EventRequest.Type.ORDER_CONFIRMED));
        } else if (event instanceof ProductViewEvent) {
            ProductViewEvent productViewEvent = (ProductViewEvent) event;

            if (productViewEvent.getItems().isEmpty()) {
                Log.w(this.getClass().getName(), "Product View event has no items. Skipping.");
                return List.of();
            }

            for (Item item : productViewEvent.getItems()) {
                ProductViewMetadataDto productViewMetadata = new ProductViewMetadataDto();
                productViewMetadata.setCurrency(item.getPrice().getCurrency().getCurrencyCode());
                productViewMetadata.setPrice(item.getPrice().getPrice().toPlainString());
                productViewMetadata.setName(item.getName());
                productViewMetadata.setImage(item.getProductImage());
                productViewMetadata.setProductId(item.getProductId());
                productViewMetadata.setSubProductId(item.getProductVariantId());
                productViewMetadata.setCategory(item.getCategory());
                eventRequests.add(new EventRequest(productViewMetadata, EventRequest.Type.PRODUCT_VIEW));
            }
        } else if (event instanceof AddToCartEvent) {
            AddToCartEvent addToCartEvent = (AddToCartEvent) event;

            if (addToCartEvent.getItems().isEmpty()) {
                Log.w(this.getClass().getName(), "Add to Cart event has no items. Skipping.");
                return List.of();
            }

            for (Item item : addToCartEvent.getItems()) {
                AddToCartMetadataDto addToCartMetadataDto = new AddToCartMetadataDto();
                addToCartMetadataDto.setCurrency(item.getPrice().getCurrency().getCurrencyCode());
                addToCartMetadataDto.setPrice(item.getPrice().getPrice().toPlainString());
                addToCartMetadataDto.setName(item.getName());
                addToCartMetadataDto.setImage(item.getProductImage());
                addToCartMetadataDto.setProductId(item.getProductId());
                addToCartMetadataDto.setSubProductId(item.getProductVariantId());
                addToCartMetadataDto.setCategory(item.getCategory());
                eventRequests.add(new EventRequest(addToCartMetadataDto, EventRequest.Type.ADD_TO_CART));
            }
        } else {
            final String error = "Unknown Event type: " + event.getClass().getName();
            Log.e(this.getClass().getName(), error);
            throw new IllegalStateException(error);
        }

        return eventRequests;
    }

    private void sendEventInternalAsync(List<EventRequest> eventRequests, UserIdentifiers userIdentifiers, String domain, @Nullable AttentiveApiCallback callback) {
        for (EventRequest eventRequest: eventRequests) {
            sendEventInternalAsync(eventRequest, userIdentifiers, domain, callback);
        }
    }

    private void sendEventInternalAsync(EventRequest eventRequest, UserIdentifiers userIdentifiers, String domain, @Nullable AttentiveApiCallback callback) {
        Metadata metadata = eventRequest.getMetadata();
        metadata.enrichWithIdentifiers(userIdentifiers);

        String externalVendorIdsJson = null;
        try {
            List<ExternalVendorId> externalVendorIds = buildExternalVendorIds(userIdentifiers);
            externalVendorIdsJson = objectMapper.writeValueAsString(externalVendorIds);
        } catch (JsonProcessingException e) {
            Log.w(this.getClass().getName(), "Could not serialize external vendor ids. Using empty array. Error: " + e.getMessage());
            externalVendorIdsJson = "[]";
        }

        HttpUrl.Builder urlBuilder = getHttpUrlEventsEndpointBuilder()
            .addQueryParameter("v", "mobile-app")
            .addQueryParameter("lt", "0")
            .addQueryParameter("tag", "modern")
            .addQueryParameter("evs", externalVendorIdsJson)
            .addQueryParameter("c", domain)
            .addQueryParameter("t", eventRequest.getType().getAbbreviation())
            .addQueryParameter("u", userIdentifiers.getVisitorId())
            .addQueryParameter("m", serialize(metadata));

        HttpUrl url = urlBuilder.build();

        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                final String error = "Could not send the request. Error: " + e.getMessage();
                Log.e(this.getClass().getName(), error);
                if (callback != null) {
                    callback.onFailure(error);
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    String error = "Could not send the request. Invalid response code: " + response.code() + ", message: "
                                 + response.message();
                    Log.e(this.getClass().getName(), error);
                    if (callback != null) {
                        callback.onFailure(error);
                    }
                    return;
                }

                Log.d(this.getClass().getName(), "Sent the '" + eventRequest.getType() + "' request successfully.");
            }
        });
    }

    @Nullable
    private <T> String serialize(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize. Error: " + e.getMessage(), e);
        }
    }
}
