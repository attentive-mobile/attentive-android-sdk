package com.attentive.androidsdk;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonInclude(JsonInclude.Include.NON_NULL)
class ExternalVendorId {
    private Vendor vendor;
    private String id;
    private String name;

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public enum Vendor {
        SHOPIFY("0"),
        KLAVIYO("1"),
        CLIENT_USER("2"),
        CUSTOM_USER("6");

        private final String id;

        Vendor(String id) {
            this.id = id;
        }

        @JsonValue
        String getVendorId() {
            return id;
        }
    }
}
