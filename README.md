# Attentive Mobile SDK for Android
The Attentive Mobile SDK for Android provides the functionality to render Attentive creative units in Android mobile applications.

## Installation
Add the Attentive Mobile SDK GitHub Package maven repository to your `build.gradle` `buildscript` or
`settings.gradle` `dependencyResolutionManagement`:
```groovy
repositories {
    ...
    maven {
        url = uri("https://maven.pkg.github.com/attentive-mobile/mobile-sdk-android")
    }
}
```

Add the `attentive-mobile-sdk` package to your `build.gradle`:
```groovy
implementation 'com.attentive:attentive-mobile-sdk:VERSION_NUMBER'
```

## Usage
See the [Example Project](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/example/src/main/java/com/attentive/example)
for a sample of how the Attentive Mobile SDK is used.

### Create the AttentiveConfig
```groovy
// Create an AttentiveConfig with your attentive domain, in production mode
AttentiveConfig attentiveConfig = new AttentiveConfig("YOUR_ATTENTIVE_DOMAIN", AttentiveConfig.Mode.PRODUCTION);

// Alternatively, enable the SDK in debug mode for more information about your creative and filtering rules
AttentiveConfig attentiveConfig = new AttentiveConfig("YOUR_ATTENTIVE_DOMAIN", AttentiveConfig.Mode.DEBUG);
```

### Identify the current user
```groovy
// Before loading the creative, you will need to register the User's ID with the attentive config.
attentiveConfig.identify("APP_USER_ID");
```

### Loading the Creative
```java
// Create a new creative and attach it to a parent View. This will not render the creative.
Creative creative = new Creative(attentiveConfig, parentView);

// Load and render the creative
creative.trigger();

// Destroy the creative and it's associated WebView. You must call the destroy method when the creative
// is no longer in use to properly clean up the WebView and it's resources
creative.destroy();
```
