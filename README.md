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

See the [Java Example Project](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/example/src/main/java/com/attentive/example) 
or the [Kotlin Example Project](https://github.com/attentive-mobile/attentive-android-sdk/tree/main/example-kotlin/src/main/java/com/attentive/example_kotlin)
for a sample of how the Attentive Android SDK is used.

__*** NOTE: Please refrain from using any private or undocumented classes or methods as they may change between releases. ***__

## Step 1 - SDK initialization
### In Java:
```java
// Create an AttentiveConfig with your attentive domain, in production mode, with any Android context *
AttentiveConfig attentiveConfig = new AttentiveConfig.Builder()
        .applicationContext(getApplicationContext())
        .domain("YOUR_ATTENTIVE_DOMAIN")
        .mode(AttentiveConfig.Mode.PRODUCTION)
        .logLevel(AttentiveLogLevel.VERBOSE)
        .build();

// Alternatively, enable the SDK in debug mode for more information about your creative and filtering rules
AttentiveConfig attentiveConfig = new AttentiveConfig.Builder()
        .applicationContext(getApplicationContext())
        .domain("YOUR_ATTENTIVE_DOMAIN")
        .mode(AttentiveConfig.Mode.DEBUG)
        .logLevel(AttentiveLogLevel.VERBOSE)
        .build();
```
### In Kotlin:
```kotlin
// Create an AttentiveConfig with your attentive domain, in production mode, with any Android context *
val attentiveConfig = AttentiveConfig.Builder()
        .applicationContext(getApplicationContext())
        .domain("YOUR_ATTENTIVE_DOMAIN")
        .mode(AttentiveConfig.Mode.PRODUCTION)
        .build()

// Alternatively, enable the SDK in debug mode for more information about your creative and filtering rules
val attentiveConfig = AttentiveConfig.Builder()
        .applicationContext(getApplicationContext())
        .domain("YOUR_ATTENTIVE_DOMAIN")
        .mode(AttentiveConfig.Mode.DEBUG)
        .build()
```

\* The `context` constructor parameter is of type [Context](https://developer.android.com/reference/android/content/Context)

### Initialize the Event Tracker
```java
// Right after defining the config, initialize the Event Tracker in order to send ecommerce and identification events *
AttentiveEventTracker.getInstance().initialize(attentiveConfig);
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

```java
UserIdentifiers userIdentifiers = new UserIdentifiers.Builder().withClientUserId("APP_USER_ID").withPhone("+15556667777").build();
attentiveConfig.identify(userIdentifiers);
```

```java
// If new identifiers are available for the user, register them with the existing AttentiveConfig instance
UserIdentifiers userIdentifiers = new UserIdentifiers.Builder().withEmail("theusersemail@gmail.com").build();
attentiveConfig.identify(userIdentifers);
```

```java
// Calling `identify` multiple times will combine the identifiers.
UserIdentifiers userIdentifiers = new UserIdentifiers.Builder().withShopifyId("555").build();
attentiveConfig.identify(userIdentifers);
userIdentifiers = new UserIdentifiers.Builder().withKlaviyoId("777").build();
attentiveConfig.identify(userIdentifers);

UserIdentifiers allIdentifiers = attentiveConfig.getUserIdentifiers();
allIdentifiers.getShopifyId(); // == 555
allIdentifiers.getKlaviyoId(); // == 777
```

### Clearing user data

If the user "logs out" of your application, you can call `clearUser` to remove all current identifiers.

```java
// If the user logs out then the current user identifiers should be deleted
attentiveConfig.clearUser();
// When/if a user logs back in, `identify` should be called again with the logged in user's identfiers
```

## Step 3 - Record user events

Next, call Attentive's event functions when each important event happens in your app, so that Attentive can understand user behavior. These events allow Attentive to trigger journeys, attribute revenue, and more. 

The SDK currently supports `PurchaseEvent`, `AddToCartEvent`, `ProductViewEvent`, and `CustomEvent`.

```java
// Construct one or more "Item"s, which represents the product(s) purchased
Price price = new Price.Builder(new BigDecimal("19.99"), Currency.getInstance("USD")).build();
Item item = new Item.Builder("11111", "222", price).quantity(1).build();

// Construct an "Order", which represents the order for the purchase
Order order = new Order.Builder().orderId("23456").build();

// (Optional) Construct a "Cart", which represents the cart this Purchase was made from
Cart cart = new Cart.Builder().cartId("7878").cartCoupon("SomeCoupon").build();

// Construct a PurchaseEvent, which ties together the preceding objects
PurchaseEvent purchaseEvent = new PurchaseEvent.Builder(List.of(item), order).cart(cart).build();

// Record the PurchaseEvent
AttentiveEventTracker.getInstance().recordEvent(purchaseEvent);
```

For the ProductViewEvent and AddToCartEvent you can build the event with a deeplink to the product/products 
the user is seeing to complete a customer journey in case the user drops off the flow. To give an 
example on how this looks like, check the following code snippet:

```java
// Construct one or more "Item"s, which represents the product(s) purchased
Price price = new Price.Builder(new BigDecimal("19.99"), Currency.getInstance("USD")).build();
Item item = new Item.Builder("11111", "222", price).quantity(1).build();

final AddToCartEvent addToCartEvent =  new AddToCartEvent.Builder()
                .items(List.of(item))
                .deeplink("https://mydeeplink.com/products/32432423")
                .build();

AttentiveEventTracker.getInstance().recordEvent(addToCartEvent);
```

You can also implement `CustomEvent` to send application-specific event schemas. These are simply key/value pairs which will be transmitted and stores in Attentive's systems for later use. Please discuss with your CSM to understand how and where these events can be use in orchestration.

Custom event implementation example:

```java
CustomEvent customEvent = new CustomEvent.Builder("Concert Viewed", Map.of("band", "The Beatles")).build();

AttentiveEventTracker.getInstance().recordEvent(customEvent);
```


## Step 3 (optional) - Show Creatives

A "creative" is a popup display init that can collect email or SMS signups. These are controlled via the Attentive UI, and will be displayed via a web view overlay.

#### 1. Create the Creative

```java
// Create a new creative and attach it to a parent View. This will not render the creative.
Creative creative = new Creative(attentiveConfig, parentView);

// Alternatively, attach the creative lifecycle to the activity lifecycle to
// automatically clear up resources. Recommended implementation if
// targeting only users above Build.VERSION_CODES.Q.
Creative creative = new Creative(attentiveConfig, parentView, activity);
```

#### 2. Trigger the Creative

When you've reached the point in the app where you'd like to show the creative, call the `trigger` function to display it. Note: the creative may not display if it's not enabled or configured properly via Attentive admin UI.

```java
// Load and render the creative, with a callback handler. 
// You may choose which of these methods to implement, they are all optional.
creative.trigger(new CreativeTriggerCallback() {
    @Override
    public void onCreativeNotOpened() {
        Log.e(this.getClass().getName(), "Couldn't open the creative!");
    }

    @Override
    public void onOpen() {
        Log.i(this.getClass().getName(), "Opened the creative!");
    }

    @Override
    public void onCreativeNotClosed() {
        Log.e(this.getClass().getName(), "Couldn't close the creative!");
    }

    @Override
    public void onClose() {
        Log.i(this.getClass().getName(), "Closed the creative!");
    }
});

// Alternatively, you can trigger the creative without a callback handler:
creative.trigger();
```
See [CreativeTriggerCallback.java](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/attentive-android-sdk/src/main/java/com/attentive/androidsdk/creatives/CreativeTriggerCallback.java) for more information on the callback handler methods.

#### 3. Destroy the Creative
```java
// Destroy the creative and it's associated WebView.
creative.destroy();
```
__*** NOTE 1: You must call the destroy method when the creative is no longer in use to properly clean up the WebView and it's resources.***__
__*** NOTE 2: Starting from Build.VERSION_CODES.Q this will be called on the destroy lifecycle callback of the activity if the activity is provided to automatically clear up resources and avoid memory leaks.***__


## Step 4 - Integrate With Push

Push tokens will automatically be sent to Attentive when your app is launched. Push notifications can only be shown if your user has granted push permissions.

To request push permissions via the Attentive SDK, pass requestPermission = true into the AttentiveEventTracker.instance.getPushToken()
To only query for a token pass false.
If you pass true and permissions are already granted, the token will simply be retrieved.


Fetch a push token and optionally show permission request:
```
    AttentiveEventTracker.instance.getPushToken(requestPermission = false).let {
        if (it.isSuccess) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Push token: ${it.getOrNull()?.token}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
```

## Other functionality

### Change domain

```java
// If you want to change domain to handle some user flow, you can do so changing the domain on attentive config. Please contact your CSM before using this use case.
attentiveConfig.changeDomain("YOUR_NEW_DOMAIN");
// Keep in mind that the new domain shouldn't be null / empty / or the same value as it's already 
// assigned, if one of those cases happens, no change will be executed.
```

### Log Level
We currently support 3 log levels. Each level is more verbose than the next one.
You can configure the log level on the Builder for the AttentiveConfig. Please keep 
in mind that this configuration only works for debuggable builds.
```java
    VERBOSE(1),
    STANDARD(2),
    LIGHT(3);

    // To set it on the builder
    .logLevel(AttentiveLogLevel.LIGHT)
```

