# Attentive Android SDK
The Attentive Android SDK provides the functionality to render Attentive creative units and collect Attentive events in Android mobile applications.

## Installation

**First**, if you don't have a GitHub account, create a free one.

**Second**, follow the [GitHub documentation](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token#creating-a-personal-access-token-classic) to create a GitHub Personal Access Token (PAT). When creating the PAT, ensure you add the scope `read:packages`. 

**Third**, add the Attentive Android SDK GitHub Package maven repository to your `build.gradle` `buildscript` or
`settings.gradle` `dependencyResolutionManagement`:
```groovy
repositories {
    // ...
    maven {
        url = uri("https://maven.pkg.github.com/attentive-mobile/attentive-android-sdk")
        credentials {
            username = "YOUR_GITHUB_USERNAME"
            password = "YOUR_GITHUB_PERSONAL_ACCESS_TOKEN_WITH_READ_PACKAGE_SCOPE"
        }
    }
}
```

**Fourth**, add the `attentive-android-sdk` package to your `build.gradle`:
```groovy
implementation 'com.attentive:attentive-android-sdk:VERSION_NUMBER'
```

## Usage
See the [Example Project](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/example/src/main/java/com/attentive/example)
for a sample of how the Attentive Android SDK is used.

__*** NOTE: Please refrain from using any private or undocumented classes or methods as they may change between releases. ***__

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
__*** NOTE: You must call the destroy method when the creative is no longer in use to properly clean up the WebView and it's resources ***__


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

## Minimum Version Support
The Attentive Android SDK currently supports Android API Level 26 and above. The SDK will still build on versions below 26, but functionality will no-op.
