package com.attentive.bonni

import com.attentive.androidsdk.screens.AttentiveScreens
import com.attentive.androidsdk.screens.ScreenName

@AttentiveScreens
enum class Routes() {
    WelcomeScreenRoute,

    @ScreenName("product_list")
    ProductScreenRoute,
    PushPermissionScreen,
    CartScreen,
    ShippingScreen,
    PaymentScreen,
    ThankYouScreen,
    SettingsScreen,
    InboxScreen,
    LegacyInboxScreen,
}
