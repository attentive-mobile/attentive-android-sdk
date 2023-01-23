# Attentive Android SDK
The Attentive Android SDK provides the functionality to render Attentive creative units in Android mobile applications.

## Installation
Follow the [GitHub documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package)
on using a GitHub Package to set up your Personal Access Token.

Add the Attentive Android SDK GitHub Package maven repository to your `build.gradle` `buildscript` or
`settings.gradle` `dependencyResolutionManagement`:
```groovy
repositories {
    // ...
    maven {
        url = uri("https://maven.pkg.github.com/attentive-mobile/attentive-android-sdk")
        credentials {
            username = properties.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = properties.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
}
```

Add the `attentive-android-sdk` package to your `build.gradle`:
```groovy
implementation 'com.attentive:attentive-android-sdk:VERSION_NUMBER'
```

## Usage
See the [Example Project](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/example/src/main/java/com/attentive/example)
for a sample of how the Attentive Android SDK is used.

### Create the AttentiveConfig
```java
// Create an AttentiveConfig with your attentive domain, in production mode, with any Android context *
AttentiveConfig attentiveConfig = new AttentiveConfig("YOUR_ATTENTIVE_DOMAIN", AttentiveConfig.Mode.PRODUCTION, context);

// Alternatively, enable the SDK in debug mode for more information about your creative and filtering rules
AttentiveConfig attentiveConfig = new AttentiveConfig("YOUR_ATTENTIVE_DOMAIN", AttentiveConfig.Mode.DEBUG, context);
```

\* The `context` constructor parameter is of type [Context](https://developer.android.com/reference/android/content/Context)

### Identify the current user
```java
// Before loading the creative, if you have any user identifiers, they will need to be registered with the attentive config. It is okay to skip this step if you have no identifiers about the user yet.
UserIdentifiers userIdentifiers = new UserIdentifiers.Builder().withClientUserId("APP_USER_ID").withPhone("+15556667777").build();
attentiveConfig.identify(userIdentifiers);
```

The more identifiers that are passed to `identify`, the better the SDK will function. See the [`UserIdentifiers`](src/main/java/com/attentive/androidsdk/UserIdentifiers.java) object for all identifier types. Here is a subset of identifiers and their descriptions:
* Client User Id - Your unique identifier for the user. This should be consistent across the user's lifetime. For example, a database id.

### Load the Creative
```java
// Create a new creative and attach it to a parent View. This will not render the creative.
Creative creative = new Creative(attentiveConfig, parentView);

// Load and render the creative
creative.trigger();

// Destroy the creative and it's associated WebView. You must call the destroy method when the creative
// is no longer in use to properly clean up the WebView and it's resources
creative.destroy();
```

### Record user events

The SDK currently supports `PurchaseEvent`, `AddToCartEvent`, and `ProductViewEvent`.

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
