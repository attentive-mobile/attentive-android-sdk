package com.attentive.androidsdk.internal.network

import com.attentive.androidsdk.UserIdentifiers
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Polymorphic
open class Metadata {
    var phone: String? = null
    var email: String? = null

    @EncodeDefault
    var source: String = "msdk"

    fun enrichWithIdentifiers(userIdentifiers: UserIdentifiers) {
        if (this.email == null && userIdentifiers.email != null) {
            this.email = userIdentifiers.email
        }
        if (this.phone == null && userIdentifiers.phone != null) {
            this.phone = userIdentifiers.phone
        }
    }
}    var currency: String? = null
