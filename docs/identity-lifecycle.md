# Android SDK Identifiers Lifecycle Guide

## The Four Identifiers

The Android SDK manages four categories of user-related identifiers. Each has a different lifecycle and purpose.

| Identifier | What it is | Where it lives | Created when | Destroyed when |
| --- | --- | --- | --- | --- |
| **Visitor ID** | 32-char hex string (e.g. `E79AFB5C69904529A309957F1AFCC3DA`) that represents "a person on this device right now" | On device (persists across app launches in SharedPreferences) | SDK initialized for the first time (or after `clearUser`/`updateUser`) | Replaced with a new ID on `clearUser()` or `updateUser()` |
| **Push Token** | FCM registration token — identifies this Firebase app installation for push delivery | In-memory + sent to backend | Firebase delivers it after app registers for FCM | Detached from the current user on `clearUser()`; re-associated on next foreground or `updateUser()` |
| **Email / Phone / Other IDs (`identify`)** | User-provided contact info attached to a visitor for analytics and identity resolution | In-memory on `AttentiveConfig.userIdentifiers` | App calls `identify()` with email/phone | Cleared on `clearUser()` or `updateUser()` |
| **Email / Phone (opt-in subscriptions)** | Contact info used to create a marketing subscription (SMS/email) | Sent directly to backend — not stored locally | App calls `optUserIntoMarketingSubscription()` | Backend removes on `optUserOutOfMarketingSubscription()` |

> **Key distinction:** The email/phone passed to `identify()` says "this visitor is reachable at this address" (identity resolution). The email/phone passed to `optUserIntoMarketingSubscription()` says "this person consents to receive marketing at this address" (subscription management). They can be the same values, but serve different purposes and go to different backend systems.

## Identifier Lifecycle Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         APP LIFECYCLE                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────┐                                                       │
│  │ SDK Init     │──▶ Visitor ID created (or loaded from SharedPrefs)    │
│  └──────────────┘                                                       │
│         │                                                               │
│         ▼                                                               │
│  ┌──────────────┐                                                       │
│  │ FCM Token    │──▶ Push Token registered with backend on every        │
│  │ Available +  │    app foreground (POST /token with visitorId +       │
│  │ App Foreground│   token + permission)                                │
│  └──────────────┘                                                       │
│         │                                                               │
│         ▼                                                               │
│  ┌──────────────┐                                                       │
│  │ identify()   │──▶ Email/phone merged into current Visitor ID         │
│  │              │    (POST /e?t=idn — identity event)                   │
│  └──────────────┘                                                       │
│         │                                                               │
│         ▼                                                               │
│  ┌──────────────┐                                                       │
│  │ optIn()      │──▶ Subscription created for email/phone               │
│  │              │    (POST /opt-in-subscriptions)                       │
│  └──────────────┘                                                       │
│         │                                                               │
│         ▼                                                               │
│  ┌──────────────┐    ┌─────────────────────────────────────────┐        │
│  │ clearUser()  │──▶ │ 1. New Visitor ID generated              │        │
│  │  (logout)    │    │ 2. All local identifiers wiped           │        │
│  └──────────────┘    │ 3. Push token detached from old visitor  │        │
│                      │    (POST /user-update with empty data)   │        │
│                      │ 4. Push token re-associated with new     │        │
│                      │    anonymous visitor                     │        │
│                      └─────────────────────────────────────────┘        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## What Each Operation Does

### `identify(userIdentifiers)`

> **Think of it as:** "Here's more info about whoever is using this device right now."

| Effect | Detail |
| --- | --- |
| Visitor ID | **Unchanged** — merges into existing visitor |
| Push Token | **Unchanged** |
| Email/Phone (local) | **Added/merged** — identifiers accumulate, never overwrite |
| Backend call | `POST /e?t=idn` — identity event merges identifiers into visitor profile |

**When to call:** Anytime you learn the user's email, phone, or other identifier (e.g., after login, after form submission). Safe to call multiple times — identifiers accumulate.

---

### `clearUser()`

> **Think of it as:** "The current user logged out. Forget everything about them on this device."

| Effect | Detail |
| --- | --- |
| Visitor ID | **Replaced** — new ID generated immediately |
| Push Token | **Detached** from old visitor, re-associated with new anonymous visitor |
| Email/Phone (local) | **Cleared** — all local identity data wiped |
| Backend call | `POST /user-update` with empty metadata (only if push token exists) |

**When to call:** On user logout. Prevents the next user on this device from receiving the previous user's push notifications or being associated with their identity.

> **⚠️ Use `AttentiveSdk.clearUser()`, not the deprecated `AttentiveConfig.clearUser()`.** The deprecated version only clears local state and does NOT call `/user-update`, so the backend still thinks the logged-out user owns the push token.

---

### `updateUser(email, phoneNumber)`

> **Think of it as:** "A different person just logged into this device. Switch everything over to them."

| Effect | Detail |
| --- | --- |
| Visitor ID | **Replaced** — new ID generated |
| Push Token | **Re-associated** with the new visitor + new email/phone |
| Email/Phone (local) | **Cleared then set** — old wiped, new email/phone merged in |
| Backend calls | `POST /user-update` with new visitorId + push token + email/phone, **plus** `POST /e?t=idn` (side effect of local `identify()` invocation during the switch) |

**When to call:** User switches accounts without the app restarting (e.g., "Log in as different user" flow). Only needed for multi-user apps.

> **Android/iOS divergence:** Android fires both `/user-update` and `/e?t=idn`; iOS fires only `/user-update`. Identity graphs end up correctly populated on both platforms (because `/user-update` is consumed by `mobile-subscription-consumer` and calls identity-api directly), but iOS does not emit a `UserIdentifierCollected` analytics event on user switch.

---

### `optUserIntoMarketingSubscription(email, phoneNumber)`

> **Think of it as:** "This person consents to receive marketing messages at this address."

| Effect | Detail |
| --- | --- |
| Visitor ID | **Unchanged** |
| Push Token | **Unchanged** (but included in the request) |
| Email/Phone (local) | **Unchanged** — not stored locally, sent directly to backend |
| Backend call | `POST /opt-in-subscriptions` — creates subscription record |

> **Android/iOS divergence:** Unlike iOS, Android does **not** queue the request if the push token is unavailable — it fires immediately. If a token isn't present, the backend may reject or silently drop the request.

---

### `optUserOutOfMarketingSubscription(email, phoneNumber)`

> **Think of it as:** "This person revokes consent for marketing at this address."

| Effect | Detail |
| --- | --- |
| Visitor ID | **Unchanged** |
| Push Token | **Unchanged** |
| Email/Phone (local) | **Unchanged** |
| Backend call | `POST /opt-out-subscriptions` — removes subscription record |

Same no-queueing behavior as opt-in.

## State Transition Summary

| Operation | Visitor ID | Push Token | Local Email/Phone | Backend Subscriptions |
| --- | --- | --- | --- | --- |
| `identify()` | — | — | Adds to existing | — |
| `clearUser()` | New ID | Detach → re-attach to new anonymous | Wiped | — |
| `updateUser()` | New ID | Re-attach to new user | Replaced | — |
| `optIn()` | — | — | — | Created |
| `optOut()` | — | — | — | Removed |

"—" means no change.

## Push Token Lifecycle (Deep Dive)

The push token is managed partly by Firebase and partly by the SDK:

1. **Creation:** Firebase generates and delivers the FCM token asynchronously after app init. Available via `FirebaseMessaging.getInstance().getToken()` at any time once Firebase is initialized.
2. **Registration (automatic):** On every app foreground (`ProcessLifecycleOwner.onStart`), `AppLaunchTracker` fetches the token and calls `POST /token` with visitorId + token + permission status. No manual integration step required.
3. **Token rotation (automatic):** `AttentiveFirebaseMessagingService.onNewToken` fires when Firebase rotates the token, calling `POST /token` immediately. This only fires if the SDK's `FirebaseMessagingService` wins dispatch — host apps that declare their own service (default intent-filter priority 0) will win over the SDK's service (priority -500).
4. **Permission change (manual):** Android exposes `AttentiveSdk.updatePushPermissionStatus(context)` to re-register with updated permission state immediately. Otherwise, the next app foreground will pick up the new permission automatically.
5. **Detachment (on `clearUser`):** Token is detached from the old visitor and re-associated with the new anonymous visitor.
6. **Transfer (on `updateUser`):** Token is re-associated with the new identified visitor.

> **Push disabled entirely:** If the host app uses `AttentiveConfig.Builder.pushEnabled(false)` or does not integrate Firebase at all, no push token will be available. In that case `updateUser()` and `clearUser()` currently silently no-op on the backend side (see [MSDK-345](https://attentivemobile.atlassian.net/browse/MSDK-345)).

## Visitor ID Lifecycle (Deep Dive)

1. **Created once** on first SDK init — stored in SharedPreferences, survives app restarts and updates.
2. **Persists** across all `identify()` calls — identity merges into the same visitor.
3. **Replaced** (not cleared) on `clearUser()` or `updateUser()` — old ID is gone, new ID takes over.
4. **Never shared** across devices — each device has its own Visitor ID.

> **Common misconception:** Visitor ID ≠ user account. A single human can have multiple Visitor IDs (one per device, or a new one after each logout). The backend identity graph resolves these into a unified profile using email/phone from `identify()` calls.

## FAQ

**Q: Does calling `identify()` multiple times create duplicate visitors?**
No. Identifiers accumulate on the same Visitor ID. Calling `identify(email)` then `identify(phone)` results in one visitor with both.

**Q: What happens if I call `optIn()` before the push token is available?**
Unlike iOS (which queues for up to 60 seconds), Android fires the request immediately. If no token is present, the backend may reject or silently drop the request. Best practice is to wait for the first app foreground (which triggers `/token` registration) before calling `optIn()`.

**Q: If I call `clearUser()`, does that unsubscribe them from marketing?**
No. `clearUser()` only affects the device-level identity (Visitor ID, push token association, local identifiers). Marketing subscriptions created via `optIn()` persist on the backend until explicitly removed via `optOut()`.

**Q: Can the same email appear in both `identify()` and `optIn()`?**
Yes, and this is common. `identify()` tells the identity system "this visitor is this person." `optIn()` tells the subscription system "this person consents to marketing." They serve different purposes even with the same value.

**Q: What backend endpoints should I monitor for identity issues?**

* `POST /e?t=idn` — identity events from `identify()` and (on Android only) as a side-effect of `updateUser()`
* `POST /token` — push token registration (fires on every app foreground)
* `POST /user-update` — `clearUser()` and `updateUser()`
* `POST /opt-in-subscriptions` and `POST /opt-out-subscriptions` — marketing consent
* `POST /mtctrl` — app launch tracking (not identity, but often correlated)

**Q: Is there a risk of push notifications going to the wrong person?**
Only if the app fails to call `AttentiveSdk.clearUser()` on logout. Without it, the push token remains associated with the previous user's Visitor ID, so they could receive pushes intended for someone else on a shared device. Using the deprecated `AttentiveConfig.clearUser()` has the same problem — it only clears local state.

**Q: Can a host app use its own `FirebaseMessagingService`?**
Yes. The SDK's `AttentiveFirebaseMessagingService` declares its intent-filter at `android:priority="-500"` so a host-declared service (default priority 0) wins FCM dispatch. The host app is then responsible for calling `AttentiveSdk.isAttentiveFirebaseMessage(remoteMessage)` and `AttentiveSdk.sendNotification(remoteMessage)` to forward Attentive pushes. A host app can also opt out of the SDK's push pipeline entirely with `AttentiveConfig.Builder.pushEnabled(false)`.

---

*See also:* [Attentive Android SDK — Identity Calls](./identity.md) (detailed technical reference with source code locations and backend implementation details), and [iOS SDK Identifiers Lifecycle Guide](https://attentivemobile.atlassian.net/wiki/spaces/ENG/pages/6712098863) for the iOS counterpart.
