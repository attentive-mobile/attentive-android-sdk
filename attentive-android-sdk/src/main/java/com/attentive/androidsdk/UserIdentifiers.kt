package com.attentive.androidsdk

import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonDeserialize(builder = UserIdentifiers.Builder::class)
data class UserIdentifiers private constructor(
    val visitorId: String?,
    val clientUserId: String?,
    val phone: String?,
    val email: String?,
    val shopifyId: String?,
    val klaviyoId: String?,
    val customIdentifiers: Map<String, String>
) {
    class Builder {
        internal var visitorId: String? = null
        internal var clientUserId: String? = null
        internal var phone: String? = null
        internal var email: String? = null
        internal var shopifyId: String? = null
        internal var klaviyoId: String? = null
        internal var customIdentifiers: Map<String, String> = emptyMap()

        fun withVisitorId(visitorId: String) = apply {
            ParameterValidation.verifyNotNull(visitorId, "visitorId")
            this.visitorId = visitorId
        }

        fun withClientUserId(clientUserId: String) = apply {
            ParameterValidation.verifyNotEmpty(clientUserId, "clientUserId")
            this.clientUserId = clientUserId
        }

        fun withPhone(phone: String) = apply {
            ParameterValidation.verifyNotEmpty(phone, "phone")
            this.phone = phone
        }

        fun withEmail(email: String) = apply {
            ParameterValidation.verifyNotEmpty(email, "email")
            this.email = email
        }

        fun withShopifyId(shopifyId: String) = apply {
            ParameterValidation.verifyNotEmpty(shopifyId, "shopifyId")
            this.shopifyId = shopifyId
        }

        fun withKlaviyoId(klaviyoId: String) = apply {
            ParameterValidation.verifyNotEmpty(klaviyoId, "klaviyoId")
            this.klaviyoId = klaviyoId
        }

        fun withCustomIdentifiers(customIdentifiers: Map<String, String>) = apply {
            ParameterValidation.verifyNotNull(customIdentifiers, "customIdentifiers")
            this.customIdentifiers = customIdentifiers.toMap() // Ensures immutability
        }

        fun build() = UserIdentifiers(
            visitorId,
            clientUserId,
            phone,
            email,
            shopifyId,
            klaviyoId,
            customIdentifiers
        )
    }

    companion object {
        fun merge(first: UserIdentifiers, second: UserIdentifiers): UserIdentifiers {
            return Builder()
                .apply {
                    visitorId = second.visitorId ?: first.visitorId
                    clientUserId = second.clientUserId ?: first.clientUserId
                    phone = second.phone ?: first.phone
                    email = second.email ?: first.email
                    klaviyoId = second.klaviyoId ?: first.klaviyoId
                    shopifyId = second.shopifyId ?: first.shopifyId
                    customIdentifiers = first.customIdentifiers + second.customIdentifiers // second's values overwrite first's
                }
                .build()
        }
    }

    override fun toString(): String {
        return "UserIdentifiers(visitorId=$visitorId, clientUserId=$clientUserId, phone=$phone, email=$email, " +
                "shopifyId=$shopifyId, klaviyoId=$klaviyoId, customIdentifiers=$customIdentifiers)"
    }
}
