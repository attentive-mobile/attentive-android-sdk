# Attentive Android SDK — Agent Integration Guide

This file is for AI coding agents (Claude Code, Cursor, Copilot, Codex, etc.) integrating the **Attentive Android SDK** into a host Android app. It is an alternative to reading the full `README.md` — it tells you exactly what to inspect in the client's codebase and what to write.

If you are an agent and the user has asked you to "set up Attentive", "integrate the Attentive SDK", or similar, follow this guide top-to-bottom. Do not add features beyond the base case unless explicitly asked.

---

## Scope

This guide covers the **minimum viable integration**: dependency wiring, `AttentiveConfig` creation, and `AttentiveSdk.initialize` in the host `Application`. That is all.

Do **not**, in this pass:
- Add identify/clearUser/updateUser calls (the user will wire those at their own login/logout sites)
- Add event recording (PurchaseEvent, AddToCartEvent, etc.)
- Add Creative rendering
- Add or configure push notifications, Firebase, `google-services.json`, or any push manifest entries
- Modify `AndroidManifest.xml` for push (no `tools:node="remove"` for the messaging service unless the user explicitly asks)

If the user asks for more after the base case is working, refer them to `README.md` in the SDK repo.

---

## Inputs you must collect from the user before writing code

1. **Attentive domain** — a short string identifying their Attentive account (e.g. `myshop`). If unknown, insert `"YOUR_ATTENTIVE_DOMAIN"` and tell the user to replace it.
2. **Mode** — `PRODUCTION` or `DEBUG`. Default to `DEBUG` for first-time integration so the user gets verbose logs; tell them to switch to `PRODUCTION` for release builds.

Do not invent a domain. If the user has not provided one, leave a clearly-marked placeholder.

---

## Step 1 — Inspect the client codebase

Before editing anything, determine:

1. **Build system**: Gradle Groovy (`build.gradle`) or Kotlin DSL (`build.gradle.kts`)? Check the app module.
2. **Repositories**: Are repositories declared in the root `build.gradle`(.kts), in `settings.gradle`(.kts) under `dependencyResolutionManagement`, or both? Whichever is authoritative is where `mavenCentral()` must exist.
3. **Application class**: Does the app already have a custom `Application` subclass?
   - Look for `android:name=".SomeApp"` (or fully-qualified) on the `<application>` tag in `AndroidManifest.xml`.
   - If yes, edit that class.
   - If no, create one (e.g. `MainApplication.kt` in the app's root package) and register it in the manifest.
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

---

## Step 3 — Initialize the SDK in `Application.onCreate`

The SDK must be initialized as early as possible after process start so app-open events are tracked correctly.

### If the client has no Application class

Create `app/src/main/java/<their-package>/MainApplication.kt`:

```kotlin
package com.example.myapp

import android.app.Application
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveSdk

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val attentiveConfig = AttentiveConfig.Builder()
            .applicationContext(this)
            .domain("YOUR_ATTENTIVE_DOMAIN")
            .mode(AttentiveConfig.Mode.DEBUG)
            .build()

        AttentiveSdk.initialize(attentiveConfig)
    }
}
```

Then register it in `AndroidManifest.xml` on the `<application>` tag:

```xml
<application
    android:name=".MainApplication"
    ...>
```

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

## Step 4 — Verify

1. Run a Gradle sync (`./gradlew :app:dependencies` or let the IDE sync).
2. Build the app (`./gradlew :app:assembleDebug`).
3. If the build succeeds, the base case is done. Tell the user:
   - Replace `"YOUR_ATTENTIVE_DOMAIN"` with their real domain if a placeholder was used.
   - Switch `Mode.DEBUG` to `Mode.PRODUCTION` for release builds (or wire it to `BuildConfig.DEBUG`).
   - For identify, events, creatives, or push, see the SDK's `README.md`.

Do not run the app on a device or emulator unless asked.

---

## Things NOT to do

- Do not add `identify()`, `clearUser()`, `updateUser()`, `recordEvent()`, or `Creative` calls.
- Do not touch Firebase, push permissions, or `google-services.json`.
- Do not remove or add the `AttentiveFirebaseMessagingService` manifest entry.
- Do not add `notificationIconId(...)` to the builder.
- Do not bump the user's `minSdk`, `targetSdk`, AGP version, or Kotlin version.
- Do not introduce DI frameworks (Hilt/Dagger/Koin) to wire this — direct construction in `onCreate` is correct here.
- Do not write tests for the integration unless asked.

---

## Reference

Full documentation: https://github.com/attentive-mobile/attentive-android-sdk/blob/main/README.md
Sample app: `bonni/` in the SDK repo.
