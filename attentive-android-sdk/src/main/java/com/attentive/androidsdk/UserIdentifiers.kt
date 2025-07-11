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

        internal fun withVisitorId(visitorId: String?): Builder = apply {
            visitorId?.let {
                this.visitorId = it
            }
        }

        fun withClientUserId(clientUserId: String?): Builder = apply {
            clientUserId?.let {
                this.clientUserId = clientUserId
            }
        }

        fun withPhone(phone: String?): Builder = apply {
            phone?.let {
                this.phone = phone
            }
        }

        fun withEmail(email: String?): Builder = apply {
            email.let {
                this.email = email
            }
        }

        fun withShopifyId(shopifyId: String?): Builder = apply {
            shopifyId.let {
                this.shopifyId = shopifyId
            }
        }

        fun withKlaviyoId(klaviyoId: String?): Builder = apply {
            klaviyoId?.let {
                this.klaviyoId = klaviyoId
            }
        }

        fun withCustomIdentifiers(customIdentifiers: Map<String, String>?): Builder = apply {
            if (customIdentifiers == null) {
                this.customIdentifiers = emptyMap() // Default to empty map if null
                return@apply
            } else if (customIdentifiers.keys.isEmpty() || customIdentifiers.values.isEmpty()) {
                this.customIdentifiers = emptyMap() // Default to empty map if empty
                return@apply
            } else {
                this.customIdentifiers = customIdentifiers.toMutableMap().filter {
                    it.key.isNotEmpty() && it.value.isNotEmpty()
                }
            }
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