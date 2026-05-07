# Attentive Android SDK — Identity calls

This document describes the identity-related SDK methods, when they fire, what the backend does with them, and when an integrator needs to call them manually.

See [`identity-ios.md`](./identity-ios.md) for the iOS companion.

## Overview

Every identity call reads from or writes to `AttentiveConfig.userIdentifiers`, an in-memory object holding:

- `visitorId` — auto-generated UUID, persisted in SharedPreferences, regenerated on user switch or logout
- `clientUserId`, `email`, `phone`, `shopifyId`, `klaviyoId`, `customIdentifiers`

**Minimum integration** (events + auto push registration work):

1. `AttentiveConfig.Builder()…build()` — required
2. `AttentiveSdk.initialize(config)` — required
3. FCM set up in the host app — required *only if* you want push notifications

Everything else (`identify`, `updateUser`, `optIn/Out`, `clearUser`) is optional and driven by the host app's user-account lifecycle.

## What fires automatically

| Trigger | Endpoint | What it does |
| --- | --- | --- |
| `AttentiveConfig.Builder.build()` | `POST events.attentivemobile.com/e?t=i` | Ping ("info event") — not identity |
| App foreground (`ProcessLifecycleOwner.onStart`) | `POST mobile.attentivemobile.com/token` | Registers current FCM push token + permission state on the current visitor |
| Activity resume after foreground | `POST mobile.attentivemobile.com/mtctrl` | App-launch tracking (`APP_LAUNCHED` or `DIRECT_OPEN` if launched from push). Debounced to 3 seconds |
| FCM token rotation (`onNewToken`) | `POST /token` | Same as foreground-triggered `/token` call |

The foreground `/token` call happens every time the app comes to the foreground from background, not just on process start. If FCM has not yet delivered a token, this is a no-op.

## Sequence diagram

```mermaid
sequenceDiagram
    participant App as Host App
    participant SDK as AttentiveSdk
    participant Tracker as AppLaunchTracker
    participant FCM as Firebase FCM
    participant BE as Attentive Backend

    Note over App,BE: One-time initialization
    App->>SDK: AttentiveConfig.Builder…build()
    SDK->>BE: POST /e?t=i (info event)
    App->>SDK: AttentiveSdk.initialize(config)
    SDK->>Tracker: observe ProcessLifecycleOwner

    Note over App,BE: Every app foreground (automatic)
    App-->>Tracker: onStart
    Tracker->>FCM: getToken()
    FCM-->>Tracker: token
    Tracker->>BE: POST /token

    Note over App,BE: Every Activity resume after foreground (automatic)
    App-->>Tracker: onActivityResumed
    Tracker->>BE: POST /mtctrl

    Note over App,BE: FCM token rotation (automatic, only if our service wins dispatch)
    FCM->>SDK: onNewToken
    SDK->>BE: POST /token

    Note over App,BE: User identifies themselves (manual)
    App->>SDK: config.identify(email/phone)
    SDK->>BE: POST /e?t=idn (merge into current visitor)

    Note over App,BE: User logs in as different user (manual)
    App->>SDK: AttentiveSdk.updateUser(email, phone)
    SDK->>SDK: resetIdentifiers() — new visitor ID
    SDK->>BE: POST /e?t=idn
    SDK->>BE: POST /user-update

    Note over App,BE: User logs out (manual)
    App->>SDK: AttentiveSdk.clearUser()
    SDK->>SDK: resetIdentifiers() — new visitor ID
    SDK->>BE: POST /user-update (empty metadata)

    Note over App,BE: Subscription actions (manual)
    App->>SDK: optUserIntoMarketingSubscription
    SDK->>BE: POST /opt-in-subscriptions
```

## Methods

### `AttentiveConfig.identify(userIdentifiers)`

**Purpose:** Attach an email, phone, or other identifier to the current visitor.

**Fires immediately:** `POST events.attentivemobile.com/e?t=idn`

**Backend effect:** `UserIdentifierService.generateUserIdentity()` merges the submitted identifiers into the existing visitor profile. This is a merge, not a replace — identifiers accumulate across calls.

**Changes visitor ID?** No.

**When to call:** whenever you learn the user's email, phone, or vendor IDs (sign-in, form submit, etc.). Safe to call multiple times.

**Required?** No. The SDK works without it — visitor-level events still land.

---

### `AttentiveSdk.updateUser(email, phoneNumber)`

**Also known as "switch user".** There is no separate `switchUser` SDK method — `updateUser()` is the call that performs a user switch. The bonni demo app exposes it as "Switch User with email" / "Switch User with phone".

**Purpose:** Declare that the current device is now a different user (multi-user apps).

**Fires:**

1. **Synchronously, locally:** `resetIdentifiers()` generates a new visitor ID and clears prior identifiers.
2. **Async:** `POST /user-update` with the new visitor ID, current push token, and email/phone.
3. **Async (triggered by step 2):** `POST /e?t=idn` with the new identifiers.

**Backend effect:** `UserUpdateController.handleUserUpdateRequest()` re-associates the push token with the new visitor. The `/e?t=idn` emission merges email/phone into the new visitor's profile.

**Changes visitor ID?** **Yes.**

**When to call:** user logs in as a different account on the same device.

**Required?** Only for apps that support multiple user accounts on one device.

> ⚠️ **Known limitation:** `/user-update` is silently dropped by the backend (204) if `pushToken` is blank. This affects apps without Firebase Cloud Messaging, and apps using `pushEnabled = false`. Under review — see [MSDK-345](https://attentivemobile.atlassian.net/browse/MSDK-345).

---

### `AttentiveSdk.clearUser()`

**Purpose:** Log the current user out. Resets local visitor identity and tells the backend to detach the push token from the prior user.

**Fires:**

1. **Synchronously, locally:** `resetIdentifiers()` generates a new visitor ID.
2. **Async:** `POST /user-update` with empty metadata and the new visitor ID.

**Backend effect:** Detaches the push token from the previous user's identity and re-associates it with the new anonymous visitor.

**Changes visitor ID?** **Yes.**

**When to call:** on logout.

**Required?** Strongly recommended on logout. Without it, the push token stays mapped to the logged-out user on the backend and will keep receiving targeted messages.

> ⚠️ **Known limitation:** Same as `updateUser()` — silent no-op on backend if `pushToken` is blank. See [MSDK-345](https://attentivemobile.atlassian.net/browse/MSDK-345).

> ⚠️ **Deprecated:** `AttentiveConfig.clearUser()` only clears local state and does NOT call `/user-update`. Always use `AttentiveSdk.clearUser()`. The deprecated method will be removed in a future release.

---

### `AttentiveSdk.optUserIntoMarketingSubscription(email, phoneNumber)`

**Purpose:** Subscribe the user to Attentive marketing on email and/or SMS.

**Fires:** `POST mobile.attentivemobile.com/opt-in-subscriptions`

**Backend effect:** `NonPushSubscriptionController` creates a subscription record (`ACTION_TYPE_SUBSCRIBE`). Idempotent.

**Changes visitor ID?** No.

**When to call:** user explicitly consents to marketing.

**Required?** Only if your app drives marketing opt-in UI. If subscriptions are managed entirely through Attentive's web/SMS flows, you never need to call this.

---

### `AttentiveSdk.optUserOutOfMarketingSubscription(email, phoneNumber)`

**Purpose:** Unsubscribe the user from Attentive marketing on email and/or SMS.

**Fires:** `POST /opt-out-subscriptions`

**Backend effect:** `ACTION_TYPE_UNSUBSCRIBE`. Idempotent.

**Changes visitor ID?** No.

**Required?** Only if your app drives subscription UI.

---

### `AttentiveSdk.updatePushPermissionStatus(context)`

**Purpose:** Re-register the push token with the latest `permissionGranted` state.

**Fires:** `POST /token` with current token and permission.

**When to call:** after the user returns from system settings and may have changed notification permission. Optional — the next app foreground will register permission state automatically.

**Required?** No.

## Comparison: `identify()` vs `updateUser()`

|  | `identify()` | `updateUser()` |
| --- | --- | --- |
| Changes visitor ID | No — merges into current | Yes — generates new |
| Calls `/user-update` | No | Yes |
| Emits `/e?t=idn` | Yes | Yes (via `/user-update` side-effect) |
| Meaning | "Here is additional info about this visitor" | "This is a different person now" |

## Known limitations / follow-ups

- **[MSDK-345](https://attentivemobile.atlassian.net/browse/MSDK-345)** — `/user-update` requires a non-blank FCM push token or it is silently discarded. Affects `updateUser()` and `clearUser()` for apps without Firebase or with `pushEnabled = false`. Under review.
  - For reference, the iOS SDK already handles the analogous tokenless case for `/opt-in-subscriptions` and `/opt-out-subscriptions` by **queueing the request in memory for up to 60 seconds** and flushing it as soon as a push token arrives (`ATTNSDK.swift:730–757`). If MSDK-345 lands as a client-side fix, the same queueing pattern is a natural template for Android and could also be extended to `/user-update` on both platforms.
- **Deprecated `AttentiveConfig.clearUser()`** — will be removed in a future release. Use `AttentiveSdk.clearUser()`.

## Naming proposal

The current method names bury the semantics under implementation language. The following is a proposed renaming to be discussed and tracked — not yet agreed.

### SDK public API

| Current | Proposed | Why |
| --- | --- | --- |
| `AttentiveConfig.identify(userIdentifiers)` | `setUserIdentifiers(...)` or `addUserIdentifiers(...)` | "Identify" is overloaded jargon from other analytics SDKs (Segment, Amplitude, Mixpanel) and doesn't signal what it does. "Add" emphasizes the merge semantic. |
| `AttentiveSdk.updateUser(email, phone)` | `loginUser(email, phone)` or `setCurrentUser(...)` | "Update" suggests modifying the existing user. This call *replaces* the user — new visitor ID, detaches token from prior user. "Login" captures the intent. |
| `AttentiveSdk.clearUser()` | `logoutUser()` | "Clear" is vague. This is specifically a logout — it's the symmetric pair to `loginUser`. |
| `AttentiveSdk.optUserIntoMarketingSubscription(...)` | `subscribe(email, phone)` or `subscribeToMarketing(...)` | Current name is five words for one concept. "Opt in to marketing subscription" reads as bureaucratic compliance language; "subscribe" is the verb the backend actually uses (`ACTION_TYPE_SUBSCRIBE`). |
| `AttentiveSdk.optUserOutOfMarketingSubscription(...)` | `unsubscribe(email, phone)` | Same. |
| `AttentiveSdk.updatePushPermissionStatus(context)` | `refreshPushPermission(context)` | "Update" makes it sound like you're *setting* the permission. You're not — you're re-registering so the backend learns what the OS now says. |
| `AttentiveConfig.clearUser()` (deprecated) | delete | Already deprecated. Don't rename, just remove. |

### Bonni settings labels

The bonni debug screen currently mixes "action you're performing" with "field you're editing" in a confusing way. Proposed grouping:

**Current user (edit-in-place fields):**

- "Change current domain" → keep
- "Change current email" → **"Email"** (it's just a field)
- "Change current phone number" → **"Phone"**

**User lifecycle actions:**

- "Switch User with email" / "Switch User with phone" → **"Log in as different user"** (single button, uses whichever fields are populated)
- "Identify User" → **"Attach identifiers to current user"** (or remove entirely — it does the same thing as editing the email/phone field above)
- "Clear Users" → **"Log out"**

**Marketing subscription:**

- "Opt-In User email" / "Opt-In User Phone Number" → **"Subscribe email" / "Subscribe SMS"**
- "Opt-Out User email" / "Opt-Out User Phone Number" → **"Unsubscribe email" / "Unsubscribe SMS"**

### Things to flag before renaming

1. **`identify()` is the oldest public API.** Renaming it is a breaking change. Would need a deprecation cycle: add `setUserIdentifiers()` that delegates to `identify()`, mark `identify()` deprecated, remove in a major version.
2. **"Login/logout" vs. "update/clear" is opinionated.** `updateUser()` technically doesn't require a "login" concept — an app could call it to correct a typo'd email too. But the fact that it *resets the visitor ID* means it's semantically a login, even if you call it for other reasons. Worth discussing with the team.
3. **Bonni's "Identify User" button currently does the same thing as saving the email field.** That's a UX bug, not a naming issue. Worth deleting that button entirely.
4. **iOS SDK parity.** If the iOS SDK has matching names, renames should happen in both to keep the cross-platform surface consistent.
