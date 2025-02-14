package com.attentive.androidsdk.internal.network

import com.attentive.androidsdk.UserIdentifiers
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
open class Metadata {
    @JvmField
    var phone: String? = null
    @JvmField
    var email: String? = null
    @JvmField
    var source: String = "msdk"

    fun enrichWithIdentifiers(userIdentifiers: UserIdentifiers) {
        if (this.email == null && userIdentifiers.email != null) {
            this.email = userIdentifiers.email
        }
        if (this.phone == null && userIdentifiers.phone != null) {
            this.phone = userIdentifiers.phone
        }
    }
}
