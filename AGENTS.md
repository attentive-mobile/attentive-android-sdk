# Attentive Android SDK — Agent Integration Guide

This file is for AI coding agents (Claude Code, Cursor, Copilot, Codex, etc.) integrating the **Attentive Android SDK** into a host Android app. It is an alternative to reading the full `README.md` — it tells you exactly what to inspect in the client's codebase and what to write.

If you are an agent and the user has asked you to "set up Attentive", "integrate the Attentive SDK", or similar, follow this guide top-to-bottom. Do not add features beyond the base case unless explicitly asked.

---

## Scope

This guide covers:
1. **Base integration** (always): dependency wiring, `AttentiveConfig` creation, and `AttentiveSdk.initialize` in the host `Application`.
2. **Push setup** (conditional, after asking the user): see Step 5.

Do **not**, in this pass:
- Add identify/clearUser/updateUser calls (the user will wire those at their own login/logout sites)
- Add event recording (PurchaseEvent, AddToCartEvent, etc.)
- Add Creative rendering

If the user asks for more after the base case is working, refer them to `README.md` in the SDK repo.

---

## Inputs you must collect from the user before writing code

1. **Attentive domain** — a short string identifying their Attentive account (e.g. `myshop`). Ask:

   > "Do you know your Attentive domain? It's the short identifier for your account (e.g. `myshop`)."

   - If the user says **yes**, immediately follow up with: "What is it?" Wait for their answer and use that exact string in the config. Do not proceed until they've given you the domain.
   - If the user says **no** (or doesn't know), insert `"YOUR_ATTENTIVE_DOMAIN"` as a placeholder and tell them to replace it before shipping.

Do not invent a domain. Always initialize the SDK in `Mode.DEBUG`; tell the user to switch to `Mode.PRODUCTION` for release builds.

---

## Step 1 — Inspect the client codebase

Before editing anything, determine:

1. **Build system**: Gradle Groovy (`build.gradle`) or Kotlin DSL (`build.gradle.kts`)? Check the app module.
2. **Repositories**: Are repositories declared in the root `build.gradle`(.kts), in `settings.gradle`(.kts) under `dependencyResolutionManagement`, or both? Whichever is authoritative is where `mavenCentral()` must exist.
3. **Application class**: Does the app already have a custom `Application` subclass?
   - Look for `android:name=".SomeApp"` (or fully-qualified) on the `<application>` tag in `AndroidManifest.xml`.
   - If yes, edit that class.
   - If no, **stop and ask the user**. Do not create an `Application` subclass on their behalf — adding one has app-wide implications (lifecycle, DI, ContentProvider init order) that the user should own. Tell them they need a custom `Application` class for SDK init and let them decide whether to add one.
4. **Language**: Is the existing Application class Kotlin or Java? Match it.
5. **`minSdk`**: Note the value. The SDK supports API 26+; on lower API levels it no-ops but still builds. If the user's `minSdk < 26`, mention this once — do not raise their `minSdk` without permission.

---

## Step 2 — Add the dependency

In the **app module's** `build.gradle` or `build.gradle.kts`, add to `dependencies`:

**Groovy (`build.gradle`):**
```groovy
implementation 'com.attentive:attentive-android-sdk:2.1.7'
```

**Kotlin DSL (`build.gradle.kts`):**
```kotlin
implementation("com.attentive:attentive-android-sdk:2.1.7")
```

> Use `2.1.7` as the default version. If the client uses a version catalog (`libs.versions.toml`), add an entry there instead and reference it via `libs.attentive.android.sdk`.

Ensure `mavenCentral()` is in the authoritative repositories block. If `dependencyResolutionManagement` exists in `settings.gradle`(.kts), add it there; otherwise add to the root `build.gradle` `allprojects { repositories { ... } }`.

After editing the build file, **sync Gradle** so the SDK classes resolve before you write any code that imports them. Run:

```bash
./gradlew :app:dependencies --configuration debugRuntimeClasspath
```

(adjust the module name if it isn't `app`). The IDE's "Sync Now" prompt does the same thing — if the user is driving the IDE, ask them to sync. Do not move on to Step 3 until the sync succeeds; otherwise the `AttentiveConfig` / `AttentiveSdk` imports you add next will fail to resolve.

---

## Step 3 — Initialize the SDK in `Application.onCreate`

The SDK must be initialized as early as possible after process start so app-open events are tracked correctly.

### If the client has no Application class

**Stop.** Do not create one. Tell the user:

> "The Attentive SDK needs to be initialized in `Application.onCreate()`, but this app doesn't have a custom `Application` subclass. Adding one has app-wide implications, so I'd like you to decide whether to add it. Once you've created an `Application` subclass and registered it in the manifest, I can wire up the Attentive init."

Wait for the user to either add the class themselves or explicitly ask you to create one. Do not proceed.

### If the client already has an Application class

Add the same two blocks (`AttentiveConfig.Builder()...build()` and `AttentiveSdk.initialize(...)`) inside the existing `onCreate()`, **after** `super.onCreate()` and after any logging/crash-reporter initialization the client already has. Do not reorder existing initialization. Match the file's language (Kotlin or Java).

**Java equivalent:**
```java
AttentiveConfig attentiveConfig = new AttentiveConfig.Builder()
    .applicationContext(this)
    .domain("YOUR_ATTENTIVE_DOMAIN")
    .mode(AttentiveConfig.Mode.DEBUG)
    .build();

AttentiveSdk.initialize(attentiveConfig);
```

---

## Step 4 — Verify the base build

1. Run a Gradle sync (`./gradlew :app:dependencies` or let the IDE sync).
2. Build the app (`./gradlew :app:assembleDebug`).
3. If the build fails, fix it before moving on. Once it succeeds, proceed to Step 5.

Do not run the app on a device or emulator unless asked.

---

## Step 5 — Ask about push

Push notifications are **enabled by default** in the SDK. Before doing anything else, ask the user a single question:

> "Do you plan to send push notifications to your users via Attentive? (yes/no)"

### If the user answers **no**

Disable push in the config to keep the SDK from fetching FCM tokens or sending app-launch / direct-open events:

```kotlin
val attentiveConfig = AttentiveConfig.Builder()
    .applicationContext(this)
    .domain("YOUR_ATTENTIVE_DOMAIN")
    .mode(AttentiveConfig.Mode.DEBUG)
    .pushEnabled(false)
    .build()
```

Stop here. Do not modify `AndroidManifest.xml`, do not add Firebase, do not add a notification icon. The base integration is complete.

### If the user answers **yes**

Walk through the following sub-questions in order. Ask one at a time and act on each before moving to the next.

#### 5a. Firebase prerequisites — detect, do not install

Check whether the project is already set up with Firebase Cloud Messaging:

- Look for `google-services.json` in the **app module** root (`app/google-services.json` or equivalent).
- Look for the Google Services Gradle plugin: `id("com.google.gms.google-services")` in the app module's `build.gradle(.kts)` and the classpath/`plugins {}` declaration in the project root or `settings.gradle(.kts)`.
- Look for `com.google.firebase:firebase-messaging` (or a `firebase-bom` import) in the app module dependencies.

If any of these are missing, **stop and tell the user**. Do not add Firebase yourself — adding it has implications (project registration, FCM credentials, app-side Firebase initialization order) that the user must own. Tell them:

> "Before I can wire up push, this app needs Firebase Cloud Messaging set up: a `google-services.json` from the Firebase console, the `com.google.gms.google-services` plugin, and a `firebase-messaging` (or `firebase-bom`) dependency. Set those up via the Firebase console (https://firebase.google.com/docs/android/setup) and let me know once `./gradlew :app:assembleDebug` still builds. Then I'll continue."

Wait for confirmation before proceeding.

#### 5b. Existing `FirebaseMessagingService` subclass — detect and offer to forward

Search the host app for an existing `FirebaseMessagingService` subclass:

- Grep all source files (`.kt` and `.java`) under the app module's `src/main/java` and `src/main/kotlin` for `: FirebaseMessagingService` (Kotlin) or `extends FirebaseMessagingService` (Java).
- Cross-check `AndroidManifest.xml` for a `<service>` whose `<intent-filter>` declares `com.google.firebase.MESSAGING_EVENT`.

**If a subclass is found**, look at its `onMessageReceived` and check whether it already forwards Attentive messages (`AttentiveSdk.isAttentiveFirebaseMessage(...)` followed by `AttentiveSdk.sendNotification(...)`). If it does not, ask:

> "I found `com.example.MyFirebaseMessagingService` extending `FirebaseMessagingService`. Want me to add the Attentive forwarding so push notifications from Attentive are displayed? It's a 3-line change inside `onMessageReceived`."

If yes, add the forwarding inside `onMessageReceived` **after** `super.onMessageReceived(remoteMessage)` and **before** the host's existing message handling, so Attentive messages short-circuit before the host tries to render them:

```kotlin
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    if (AttentiveSdk.isAttentiveFirebaseMessage(remoteMessage)) {
        AttentiveSdk.sendNotification(remoteMessage)
        return
    }
    // existing host handling stays here
}
```

Add the `AttentiveSdk` import. Match Kotlin/Java to the existing class. If the host's service has a higher `android:priority` than the SDK's (the SDK declares `-500`), this forwarding is what makes Attentive notifications still display.

**If no subclass is found**, do nothing — the SDK's built-in `AttentiveFirebaseMessagingService` will receive Attentive messages directly. Skip to 5c.

#### 5c. Notification icon

Ask:

> "Push notifications need a small monochrome icon (Android requires a flat white-on-transparent drawable for the status bar). Do you already have one in `res/drawable/`? If so, tell me its name and I'll wire it up. If not, I can leave a placeholder."

If the user provides a drawable name, add it to the config:

```kotlin
.notificationIconId(R.drawable.your_icon_name)
```

If they don't have one, add a clearly-marked placeholder comment in the builder and tell them to set it before sending pushes:

```kotlin
// TODO(attentive): set a notification icon (small monochrome drawable in res/drawable/)
// .notificationIconId(R.drawable.attentive_notification_icon)
```

Ask follow-up only if relevant: "Do you want to set a background color for the icon?" If yes, add `.notificationIconBackgroundColor(R.color.your_color)`.

#### 5d. Push permission prompt (Android 13+)

Ask:

> "On Android 13+ apps must request `POST_NOTIFICATIONS` permission to show notifications. Do you want the SDK to handle this prompt for you the first time the user opens the app? (If you already manage notification permission yourself, say no.)"

If **yes**, add a one-shot call in the launcher activity's `onCreate` (after `super.onCreate(savedInstanceState)`). Match the activity's language. Kotlin example:

```kotlin
lifecycleScope.launch {
    AttentiveSdk.getPushToken(application = application, requestPermission = true)
}
```

Add the `AttentiveSdk` import and `androidx.lifecycle:lifecycle-runtime-ktx` import for `lifecycleScope` if not present (it usually is via Compose / AppCompat). For Java, use `AttentiveSdk.getPushTokenWithCallback(application, true, callback)`.

If **no**, tell the user to call `AttentiveSdk.updatePushPermissionStatus(context)` themselves after their own permission flow resolves so Attentive learns the result.

#### 5e. `singleTask` launcher activity — detect and patch `onNewIntent`

Inspect the manifest's launcher activity (the one whose `<intent-filter>` declares `android.intent.action.MAIN` + `android.intent.category.LAUNCHER`). Check `android:launchMode`.

If it is `singleTask` or `singleInstance`, the SDK cannot detect notification taps when the app is brought from background unless the activity calls `setIntent(...)` on incoming intents. Look at the activity class for an existing `onNewIntent` override:

- If `onNewIntent` exists and does not call `setIntent(intent)`, ask before editing — there may be a reason. If they say yes, add `setIntent(intent)` after `super.onNewIntent(intent)`.
- If `onNewIntent` does not exist, add it:

```kotlin
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    intent?.let { setIntent(it) }
}
```

If the launch mode is the default (`standard`) or `singleTop`, skip this step.

---

## Step 6 — Re-verify

1. Build the app (`./gradlew :app:assembleDebug`).
2. If you added a `notificationIconId` placeholder TODO, remind the user to fill it in before testing.
3. Tell the user:
   - Replace `"YOUR_ATTENTIVE_DOMAIN"` with their real domain if a placeholder was used.
   - Switch `Mode.DEBUG` to `Mode.PRODUCTION` for release builds (or wire it to `BuildConfig.DEBUG`).
   - For identify, events, and creatives, see the SDK's `README.md`.

Do not run the app on a device or emulator unless asked.

---

## Things NOT to do

- Do not add `identify()`, `clearUser()`, `updateUser()`, `recordEvent()`, or `Creative` calls.
- Do not install Firebase or create a `google-services.json` — only detect what's already there.
- Do not add `<service tools:node="remove">` for `AttentiveFirebaseMessagingService`. Use `pushEnabled(false)` instead.
- Do not create an `Application` subclass for the user. If one doesn't exist, ask them to add it themselves.
- Do not bump the user's `minSdk`, `targetSdk`, AGP version, or Kotlin version.
- Do not introduce DI frameworks (Hilt/Dagger/Koin) to wire this — direct construction in `onCreate` is correct here.
- Do not write tests for the integration unless asked.

---

## Reference

Full documentation: https://github.com/attentive-mobile/attentive-android-sdk/blob/main/README.md
Sample app: `bonni/` in the SDK repo.

### What this guide intentionally skipped

After the base integration is working, point the user at the README for any of the following — none of them are wired up by this guide:

When you list these for the user, format each README reference as a Markdown link to the section anchor on GitHub (e.g. `[Step 2 - Identify the current user](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/README.md#step-2---identify-the-current-user)`) so they're clickable. GitHub anchors are the heading lowercased with spaces → `-` and punctuation stripped.

- **Identifying the user** (`AttentiveSdk.identify(...)`) — call at login or whenever you learn the user's email, phone, `clientUserId`, Shopify ID, Klaviyo ID, or custom identifiers. → [Step 2 - Identify the current user](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/README.md#step-2---identify-the-current-user).
- **Clearing user data** (`AttentiveSdk.clearUser()`) — call on logout. Resets identifiers and regenerates the visitor ID. → [Clearing user data](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/README.md#clearing-user-data).
- **Updating identity post-login** (`updateUser`, `identify` merge semantics) → [Managing User Identity](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/README.md#managing-user-identity).
- **Recording events** — `PurchaseEvent`, `AddToCartEvent`, `ProductViewEvent`, `CustomEvent`, plus the `Item` / `Price` / `Order` / `Cart` metadata models. → [Step 3 - Record user events](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/README.md#step-3---record-user-events).
- **Creatives** — in-app messages rendered in a WebView. Create / trigger / destroy lifecycle, triggering a specific creative, and skipping fatigue rules. → [Step 3 (optional) - Show Creatives](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/README.md#step-3-optional---show-creatives).
- **Push deep linking** — handling notification taps that open a specific screen. → [Deeplinking](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/README.md#deeplinking).
- **`FirebaseMessagingService` priority** — what to do if multiple services are declared and Attentive's isn't winning. → [Service priority when multiple FirebaseMessagingService declarations exist](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/README.md#service-priority-when-multiple-firebasemessagingservice-declarations-exist).
- **Subscription management** — email/SMS opt-in and opt-out helpers. → [Manage subscriptions for email and phone number](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/README.md#manage-subscriptions-for-email-and-phone-number).
- **Changing domain at runtime** — for apps that switch Attentive accounts. → [Change domain](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/README.md#change-domain).
- **Log level** — quieting or verbosing the SDK logger. → [Log Level](https://github.com/attentive-mobile/attentive-android-sdk/blob/main/README.md#log-level).

Tell the user: "The base integration is in. For identify/clearUser, event tracking, creatives, deep links, and subscription management, see the README — I left those out on purpose so you can wire them at the right call sites."
