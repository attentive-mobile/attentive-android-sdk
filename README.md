# Attentive Android SDK
The Attentive Android SDK provides the functionality to render Attentive creative units and collect Attentive events in Android mobile applications.

## Installation

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

## Create the AttentiveConfig
### In Java:
```java
// Create an AttentiveConfig with your attentive domain, in production mode, with any Android context *
AttentiveConfig attentiveConfig = new AttentiveConfig.Builder()
        .context(getApplicationContext())
        .domain("YOUR_ATTENTIVE_DOMAIN")
        .mode(AttentiveConfig.Mode.PRODUCTION)
        .logLevel(AttentiveLogLevel.VERBOSE)
        .build();

// Alternatively, enable the SDK in debug mode for more information about your creative and filtering rules
AttentiveConfig attentiveConfig = new AttentiveConfig.Builder()
        .context(getApplicationContext())
        .domain("YOUR_ATTENTIVE_DOMAIN")
        .mode(AttentiveConfig.Mode.DEBUG)
        .logLevel(AttentiveLogLevel.VERBOSE)
        .build();
```
### In Kotlin:
```kotlin
// Create an AttentiveConfig with your attentive domain, in production mode, with any Android context *
val attentiveConfig = AttentiveConfig.Builder()
        .context(getApplicationContext())
        .domain("YOUR_ATTENTIVE_DOMAIN")
        .mode(AttentiveConfig.Mode.PRODUCTION)
        .build()

// Alternatively, enable the SDK in debug mode for more information about your creative and filtering rules
val attentiveConfig = AttentiveConfig.Builder()
        .context(getApplicationContext())
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

### Identify the current user
```java
// Before loading the creative, if you have any user identifiers, they will need to be registered with the attentive config. It is okay to skip this step if you have no identifiers about the user yet.
UserIdentifiers userIdentifiers = new UserIdentifiers.Builder().withClientUserId("APP_USER_ID").withPhone("+15556667777").build();
attentiveConfig.identify(userIdentifiers);
```

The more identifiers that are passed to `identify`, the better the SDK will function. Here is the list of possible identifiers:
| Identifier Name    | Type                  | Description                                                                                                             |
| ------------------ | --------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| Client User ID     | String                | Your unique identifier for the user. This should be consistent across the user's lifetime. For example, a database id.  |
| Phone              | String                | The users's phone number in E.164 format                                                                                |
| Email              | String                | The users's email                                                                                                       |
| Shopify ID         | String                | The users's Shopify ID                                                                                                  |
| Klaviyo ID         | String                | The users's Klaviyo ID                                                                                                  | 
| Custom Identifiers | Map<String,String>    | Key-value pairs of custom identifier names and values. The values should be unique to this user.                        |

### Load the Creative
#### 1. Create the Creative
```java
// Create a new creative and attach it to a parent View. This will not render the creative.
Creative creative = new Creative(attentiveConfig, parentView);

// A variation to create the creative, only difference is that it will
// attach the creative lifecycle to the activity lifecycle to
// automatically clear up resources. Recommended implementation if
// targeting only users above Build.VERSION_CODES.Q.
Creative creative = new Creative(attentiveConfig, parentView, activity);
```

#### 2. Trigger the Creative
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


### Record user events

The SDK currently supports `PurchaseEvent`, `AddToCartEvent`, `ProductViewEvent`, and `CustomEvent`.

```java
// Construct one or more "Item"s, which represents the product(s) purchased
Price price = new Price.Builder(new BigDecimal("19.99"), Currency.getInstance("USD")).build();
Item item = new Item.Builder("11111", "222", price).quantity(1).build();

// Construct an "Order", which represents the order for the purchase
Order order = new Order.Builder("23456").build();

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
                .buildIt();

AttentiveEventTracker.getInstance().recordEvent(addToCartEvent);
```

### Update the current user when new identifiers are available

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

### Clear the current user
```java
// If the user logs out then the current user identifiers should be deleted
attentiveConfig.clearUser();
// When/if a user logs back in, `identify` should be called again with the logged in user's identfiers
```

### Change domain
```java
// If you want to change domain to handle some user flow, you can do so changing the domain on attentive config
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

## Minimum Version Support
The Attentive Android SDK currently supports Android API Level 26 and above. The SDK will still build on versions below 26, but functionality will no-op.
