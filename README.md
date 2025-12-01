# Attentive SDK

The Attentive mobile SDK provides identity, data collection, and creative rendering functionality for your app. This enables cross-platform journeys, enhanced reporting, and a great experience for your mobile users.

## Android - Prerequisites

The Attentive Android SDK currently supports Android API Level 26 and above. The SDK will still build on versions below 26, but functionality will no-op.

**First**, add the Maven Central repository to your `build.gradle` `buildscript` or
`settings.gradle` `dependencyResolutionManagement`:
```groovy
repositories {
    // ...
    mavenCentral()
}
```

**Second**, add the `attentive-android-sdk` package to your `build.gradle`:
```groovy
implementation 'com.attentive:attentive-android-sdk:VERSION_NUMBER'
```

## Usage

See the [Bonni Example Project](https://github.com/attentive-mobile/attentive-android-sdk/tree/main/bonni/src/main/java/com/attentive/bonni)
for a sample of how the Attentive Android SDK is used.

__*** NOTE: Please refrain from using any private or undocumented classes or methods as they may change between releases. ***__

## Step 1 - SDK initialization

__*** NOTE: To function properly, the SDK must be initialized as soon as possible after application startup. This is required for us to properly track metrics (app open events, etc) ***__

```kotlin
// Create an AttentiveConfig with your attentive domain, in production mode, with an Application context
val attentiveConfig = AttentiveConfig.Builder()
        .applicationContext(getApplicationContext())
        .domain("YOUR_ATTENTIVE_DOMAIN")
        .mode(AttentiveConfig.Mode.PRODUCTION)
        // Add a notification icon drawable id if using push
        .notificationIconId(R.drawable.your_notification_icon)
        .build()



// Alternatively, enable the SDK in debug mode for more information about your creative and filtering rules
val attentiveConfig = AttentiveConfig.Builder()
        .applicationContext(getApplicationContext())
        .domain("YOUR_ATTENTIVE_DOMAIN")
        .mode(AttentiveConfig.Mode.DEBUG)
        .build()
```

### Initialize the SDK
```kotlin
// Right after defining the config, initialize the SDK in order to send ecommerce and identification events
AttentiveSdk.initialize(attentiveConfig)
```

## Step 2 - Identify the current user

When you have information about the current user (user ID, email, phone, etc), you can pass it to Attentive for identification purposes via the `identify` function. You can call identify every time you have additional information about the user during your app's flows.

| Identifier Name    | Type                  | Description                                                                                                             |
| ------------------ | --------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| Client User ID     | String                | Your unique identifier for the user. This should be consistent across the user's lifetime. For example, a database id.  |
| Phone              | String                | The users's phone number in E.164 format                                                                                |
| Email              | String                | The users's email                                                                                                       |
| Shopify ID         | String                | The users's Shopify ID                                                                                                  |
| Klaviyo ID         | String                | The users's Klaviyo ID                                                                                                  | 
| Custom Identifiers | Map<String,String>    | Key-value pairs of custom identifier names and values. The values should be unique to this user.                        |

Examples:

```kotlin
val userIdentifiers =
            UserIdentifiers
                .Builder()
                .withClientUserId("APP_USER_ID")
                .withPhone("+15556667777")
                .build()

attentiveConfig.identify(userIdentifiers)
```

```kotlin
// If new identifiers are available for the user, register them with the existing AttentiveConfig instance
val userIdentifiers =
            UserIdentifiers
                .Builder()
                .withEmail("theusersemail@gmail.com")
                .withPhone("+15556667777")
                .build()

attentiveConfig.identify(userIdentifiers)
```

```kotlin
// Calling `identify` multiple times will combine the identifiers.
 val userIdentifiers = UserIdentifiers.Builder().withShopifyId("555").build()
attentiveConfig.identify(userIdentifers)
userIdentifiers = UserIdentifiers.Builder().withKlaviyoId("777").build()
attentiveConfig.identify(userIdentifers)

val allIdentifiers = attentiveConfig.userIdentifiers
allIdentifiers.shopifyId // == 555
allIdentifiers.klaviyoId // == 777
```

### Clearing user data

If the user "logs out" of your application, you can call `clearUser` to remove all current identifiers.

```kotlin
// If the user logs out then the current user identifiers should be deleted
attentiveConfig.clearUser();
// When/if a user logs back in, `identify` should be called again with the logged in user's identfiers
```

## Step 3 - Record user events

Next, call Attentive's event functions when each important event happens in your app, so that Attentive can understand user behavior. These events allow Attentive to trigger journeys, attribute revenue, and more. 

The SDK currently supports `PurchaseEvent`, `AddToCartEvent`, `ProductViewEvent`, and `CustomEvent`.

```kotlin
// Construct one or more "Item"s, which represents the product(s) purchased
 val price: Price = Price.Builder().price(BigDecimal("19.99")).currency(Currency.getInstance("USD")).build()
val item: Item = Item.Builder(productId = "111", productVariantId = "3235", price = price).name("Product Name").quantity(1).build()


// Construct an "Order", which represents the order for the purchase
val order: Order = Order.Builder().orderId("23456").build()


// (Optional) Construct a "Cart", which represents the cart this Purchase was made from
val cart = Cart.Builder().cartId("7878").cartCoupon("SomeCoupon").build()


// Construct a PurchaseEvent, which ties together the preceding objects
val purchaseEvent = PurchaseEvent.Builder(listOf(item), order).cart(cart).build()


// Record the PurchaseEvent
AttentiveSdk.sendEvent(purchaseEvent)
```

For the ProductViewEvent and AddToCartEvent you can build the event with a deeplink to the product/products 
the user is seeing to complete a customer journey in case the user drops off the flow. To give an 
example on how this looks like, check the following code snippet:

```kotlin
// Construct one or more "Item"s, which represents the product(s) purchased
val price: Price = Price.Builder().price(BigDecimal("19.99")).currency(Currency.getInstance("USD")).build()
val item: Item = Item.Builder(productId = "111", productVariantId = "3235", price = price)
            .name("Product Name").quantity(1).build()
val addToCartEvent = AddToCartEvent.Builder()
            .items(listOf(item))
            .deeplink("https://mydeeplink.com/products/32432423")
            .build()

AttentiveSdk.sendEvent(addToCartEvent)
```

You can also implement `CustomEvent` to send application-specific event schemas. These are simply key/value pairs which will be transmitted and stores in Attentive's systems for later use. Please discuss with your CSM to understand how and where these events can be use in orchestration.

Custom event implementation example:

```kotlin
val customEvent =
            CustomEvent.Builder("Concert Viewed", mapOf("band" to "The Beatles"))
                .build()

AttentiveSdk.sendEvent(customEvent)
```


## Step 3 (optional) - Show Creatives

A "creative" is a popup display init that can collect email or SMS signups. These are controlled via the Attentive UI, and will be displayed via a web view overlay.

#### 1. Create the Creative

```kotlin
// Create a new creative and attach it to a parent View. This will not render the creative.
val creative = Creative(attentiveConfig, parentView)


// Alternatively, attach the creative lifecycle to the activity lifecycle to
// automatically clear up resources. Recommended implementation if
// targeting only users above Build.VERSION_CODES.Q.
val creative = Creative(attentiveConfig, parentView, activity)
```

#### 2. Trigger the Creative

When you've reached the point in the app where you'd like to show the creative, call the `trigger` function to display it. Note: the creative may not display if it's not enabled or configured properly via Attentive admin UI.

```kotlin
// Load and render the creative, with a callback handler. 
// You may choose which of these methods to implement, they are all optional.
creative.trigger(object : CreativeTriggerCallback {
            override fun onCreativeNotOpened() {
                Log.e(this.javaClass.getName(), "Couldn't open the creative!")
            }

            override fun onOpen() {
                Log.i(this.javaClass.getName(), "Opened the creative!")
            }

            override fun onCreativeNotClosed() {
                Log.e(this.javaClass.getName(), "Couldn't close the creative!")
            }

            override fun onClose() {
                Log.i(this.javaClass.getName(), "Closed the creative!")
            }
        })


// Alternatively, you can trigger the creative without a callback handler:
creative.trigger()
```
See [CreativeTriggerCallback.java](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/attentive-android-sdk/src/main/java/com/attentive/androidsdk/creatives/CreativeTriggerCallback.java) for more information on the callback handler methods.

#### 3. Destroy the Creative
```kotlin
// Destroy the creative and it's associated WebView.
creative.destroy();
```
__*** NOTE 1: You must call the destroy method when the creative is no longer in use to properly clean up the WebView and it's resources.***__
__*** NOTE 2: Starting from Build.VERSION_CODES.Q this will be called on the destroy lifecycle callback of the activity if the activity is provided to automatically clear up resources and avoid memory leaks.***__


## Step 4 - Integrate With Push

The conditions for your users to receive push notifications are as follows:
1. You have initialized the Attentive SDK with a valid domain and a notification icon resource id.
2. Your app must be registered with Firebase and you must have a valid `google-services.json` file in your project. See https://firebase.google.com/docs/android/setup for setup.
3. Your user must have granted push notification permissions to your app. You can request permissions with our sdk, or do it yourself.

After these conditions are met, you can send push notifications to your users via the Attentive web UI.

### Tokens and Permissions
If you want to grab your users push token, or request permissions with the sdk, do as follows.
It is not necessary to call ```getPushToken()``` before a user will receive push notifications, but if you want to
know the token value you can call this function.
To request push permissions via the Attentive SDK, pass ```requestPermission = true``` into ```AttentiveSdk.getPushToken(application = yourApplicationInstance, requestPermission = true)```
To only query for a token pass ```false```.



Fetch a push token and optionally show permission request:
```kotlin
        CoroutineScope(Dispatchers.IO).launch {
            AttentiveSdk.getPushToken(application = yourApplicationInstance, requestPermission = true)
        }
```

For Java interop use the ```getPushTokenWithCallback()``` function instead :
```java
AttentiveSdk.getPushTokenWithCallback(application, requestPermission, new AttentiveSdk.PushTokenCallback() {
            @Override
            public void onSuccess(@NotNull TokenFetchResult result) {
                String token = result.getToken();
                System.out.println("Push token fetched successfully: " + token);
            }

            @Override
            public void onFailure(@NotNull Exception exception) {

            }
        });
    }
```

If you decide to handle permission requests yourself, you should update the push permission status after you receive your permission callback from the system:

```kotlin
    // After receiving the permission result from the system
    AttentiveSdk.updatePushPermissionStatus(context)
```

```java
    AttentiveSdk.INSTANCE.updatePushPermissionStatus(context);
```

This will automatically fetch the permission status on the device and report it to Attentive. If you fail to call this, it will not get reported until the next time the app is foregrounded.

You do not need a subclass of `FirebaseMessagingService`, the sdk will handle that for you.
If you have an existing subclass of `FirebaseMessagingService` you can route messages received there to the Attentive SDK.

First check that is a message from Attentive, then send it over.

```kotlin
    class YourFirebaseMessagingService : FirebaseMessagingService() {

        override fun onMessageReceived(remoteMessage: RemoteMessage) {
            super.onMessageReceived(remoteMessage)
            if(AttentiveSdk.isAttentiveFirebaseMessage(remoteMessage)) {
                AttentiveSdk.sendNotification(remoteMessage)
            }
        }
    }
```

### Deeplinking
The SDK will package a ```PendingIntent``` with the notification that will trigger when tapped. If no deep link is provided via the attentive web ui, the launcher activity will be opened when then notification is tapped. You must setup your host app with an ```intent filter``` for the deeplink you provided to launch your desired activity.

## Other functionality

### Change domain

If you want to change domain to handle some user flow, you can do so changing the domain on attentive config. Please contact your CSM before using this use case.
```kotlin
    attentiveConfig.changeDomain("YOUR_NEW_DOMAIN");
```
Keep in mind that the new domain shouldn't be null / empty / or the same value as it's already 
assigned, if one of those cases happens, no change will be executed.


### Manage subscriptions for email and phone number
Our SDK allows you to directly manage marketing subscriptions for emails and phone numbers. Your app is solely responsible for displaying any required legal information. To opt users in or out, you must provide at least one of either an email address or a phone number. Phone numbers must be in [E.164 format](https://support.google.com/google-ads/answer/16355235?hl=en).

To opt users in and/or out simply call
```kotlin
    AttentiveSdk.optUserIntoMarketingSubscription(optionalEmail, optionalPhoneNumber)
```
and

```kotlin
    AttentiveSdk.optUserOutOfMarketingSubscription(optionalEmail, optionalPhoneNumber)
```

### Update user via email and/or phone

Our SDK supports switching the identified user via email and/or phone (at least one identifier must be provided). 
Calling this method will clear all identifiers previously associated with the current user (the sdk will automatically call clearUser()), and associate the app with the new identifier(s) you provide. 
This ensures that all subsequent events and messages are attributed to the newly identified user.

```kotlin
AttentiveSdk.updateUser(email = "someOptionalEmail", phone = "someOptionalPhone")
```


### Log Level
All Attentive logs can be filtered by the string "Attentive"

We currently support 3 log levels. Each level is more verbose than the next one.
The log levels will dictate sdk and network logging levels
You can configure the log level on the Builder for the AttentiveConfig. Please keep 
in mind that this configuration only works for debuggable builds.
```kotlin
    VERBOSE(1),
    STANDARD(2),
    LIGHT(3);

    // To set it on the builder
    .logLevel(AttentiveLogLevel.LIGHT)
```

