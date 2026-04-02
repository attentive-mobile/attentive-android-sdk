package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
enum class IdType {
    ShopifyId,
    KlaviyoId,
    ClientUserId,
    CustomId,
    ShopifyY,
    BluecoreId,
    SalesforceId,
    EdgetagId,
    SalesforceMcId,
    CordialId,
    GenericClickId,
    AttnEatId,
}
