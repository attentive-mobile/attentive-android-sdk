package com.attentive.androidsdk

import kotlinx.serialization.Serializable

@Serializable
data class UserIdentifiers(
    val visitorId: String? = null,
    val clientUserId: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val shopifyId: String? = null,
    val klaviyoId: String? = null,
    val customIdentifiers: Map<String, String> = emptyMap()
) {
    @Serializable
    class Builder {
        private var visitorId: String? = null
        private var clientUserId: String? = null
        private var phone: String? = null
        private var email: String? = null
        private var shopifyId: String? = null
        private var klaviyoId: String? = null
        private var customIdentifiers: Map<String, String> = emptyMap()

        fun withVisitorId(visitorId: String): Builder = apply {
            ParameterValidation.verifyNotNull(visitorId, "visitorId")
            this.visitorId = visitorId
        }

        fun withClientUserId(clientUserId: String): Builder = apply {
            ParameterValidation.verifyNotEmpty(clientUserId, "clientUserId")
            this.clientUserId = clientUserId
        }

        fun withPhone(phone: String): Builder = apply {
            this.phone = phone
        }

        fun withEmail(email: String): Builder = apply {
            this.email = email
        }

        fun withShopifyId(shopifyId: String): Builder = apply {
            ParameterValidation.verifyNotEmpty(shopifyId, "shopifyId")
            this.shopifyId = shopifyId
        }

        fun withKlaviyoId(klaviyoId: String): Builder = apply {
            ParameterValidation.verifyNotEmpty(klaviyoId, "klaviyoId")
            this.klaviyoId = klaviyoId
        }

        fun withCustomIdentifiers(customIdentifiers: Map<String, String>): Builder = apply {
            ParameterValidation.verifyNotNull(customIdentifiers, "customIdentifiers")
            this.customIdentifiers = customIdentifiers.toMap() // Ensures immutability
        }

        fun build(): UserIdentifiers = UserIdentifiers(
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
            return Builder().apply {
                second.visitorId?.let { withVisitorId(it) } ?: first.visitorId?.let { withVisitorId(it) }
                second.clientUserId?.let { withClientUserId(it) } ?: first.clientUserId?.let { withClientUserId(it) }
                second.phone?.let { withPhone(it) } ?: first.phone?.let { withPhone(it) }
                second.email?.let { withEmail(it) } ?: first.email?.let { withEmail(it) }
                second.klaviyoId?.let { withKlaviyoId(it) } ?: first.klaviyoId?.let { withKlaviyoId(it) }
                second.shopifyId?.let { withShopifyId(it) } ?: first.shopifyId?.let { withShopifyId(it) }
                withCustomIdentifiers(first.customIdentifiers + second.customIdentifiers)
            }.build()
        }
    }

    override fun toString(): String {
        return "UserIdentifiers(visitorId=$visitorId, clientUserId=$clientUserId, phone=$phone, email=$email, " +
                "shopifyId=$shopifyId, klaviyoId=$klaviyoId, customIdentifiers=$customIdentifiers)"
    }
}