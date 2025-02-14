package com.attentive.androidsdk

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue

@JsonInclude(JsonInclude.Include.NON_NULL)
internal open class ExternalVendorId {
    var vendor: Vendor? = null
    var id: String? = null
    var name: String? = null

    enum class Vendor(@get:JsonValue val vendorId: String) {
        SHOPIFY("0"),
        KLAVIYO("1"),
        CLIENT_USER("2"),
        CUSTOM_USER("6")
    }
}
