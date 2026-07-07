# CLAUDE.md

## Project Overview

Attentive Android SDK — provides identity, event tracking, creatives, push notifications, and inbox for Android apps. Supports Kotlin and Java consumers.

## Internal API Documentation

Internal Attentive API documentation and instructions live in the private [`attentive-mobile/claude-plugins`](https://github.com/attentive-mobile/claude-plugins) repo as the `mobile-sdk-internal` plugin. To load this context into Claude Code:

```
/plugin marketplace add attentive-mobile/claude-plugins
/plugin install mobile-sdk-internal@attentive-marketplace
```

If you've previously added the marketplace but don't see `mobile-sdk-internal`, refresh the local cache first:

```
/plugin marketplace update attentive-marketplace
```

This keeps internal details out of the public SDK repo while letting Claude Code pull them in at runtime.

### Drafting a PR description

Once the `mobile-sdk-internal` plugin is installed, use its `write-pr` skill to draft PR descriptions that meet the mobile SDK team's expectations (verification with API-level coverage, public API impact, host-app rollback):

```
/mobile-sdk-internal:write-pr           # new PR
/mobile-sdk-internal:write-pr --update  # update existing PR on this branch
```

The skill detects this repo from the git remote, confirms the base branch (this repo uses both `main` and `develop`, so confirmation matters), reads the diff, and asks targeted questions before drafting.

## Project Structure

Multi-module Android Gradle project:
- **attentive-android-sdk** — The SDK library, published to Maven Central as `com.attentive:attentive-android-sdk`
- **bonni** — Internal demo/test app distributed via Firebase App Distribution
- **attentive-lint** — Custom lint rules
- **scripts/** — Gradle publish scripts (`publish-root.gradle`, `publish-module.gradle`)

## Build & Test

```bash
# Unit tests
./gradlew :attentive-android-sdk:testDebugUnitTest

# Lint
./gradlew lintDebug
./gradlew ktlintCheck

# Build SDK
./gradlew :attentive-android-sdk:assembleRelease

# Snapshot tests (bonni)
./gradlew :bonni:verifyRoborazziDebug
```

SDK targets Java 11. Bonni requires Java 21. Kotlin 2.0.0 with Compose.

## Versioning

SDK version lives in `attentive-android-sdk/gradle.properties` as `VERSION_NAME=x.y.z`. Beta versions use format `x.y.z.beta.N`.

## Publishing

Published to Maven Central via Sonatype:
```bash
./gradlew clean publishToSonatype closeAndReleaseSonatypeStagingRepository
```
- **Local**: Uses credentials from `local.properties`
- **CI**: GitHub Action triggers CircleCI pipeline with signing and Sonatype credentials provided as env vars.

## CI

- **GitHub Actions** (`ci.yml`) — Runs unit tests and checkstyle on push/PR to main and feature branches
- **CircleCI** (`.circleci/config.yml`) — Lint, unit tests, instrumented tests, snapshot tests, build, bonni distribution, and release pipeline
- **Release workflow** — GitHub Action (`release.yml`) triggers CircleCI which pushes a `release/x.y.z` branch, opens a PR into `main`, then publishes to Maven Central and creates a GitHub Release tagged off the release branch. The PR is merged into `main` manually after the release is verified. Beta versions skip the GitHub Release.

## Git Push Policy

- **Feature/bugfix branches**: You may push freely without asking.
- **main and develop**: Never push without explicit user confirmation. If the user's original message says "push it" or similar, that counts. Otherwise, stop and ask before pushing. Do not push in the same turn as asking the question — wait for the user's reply.

## Branching

- `main` — stable release branch
- `develop` — integration branch (cutting-edge changes)
- Feature branches: `feature/MSDK-###-description`
- Bugfix branches: `bugfix/MSDK-###-description`
- Release branches: `release/x.y.z`
- Merge branches (reconciling `develop` → `main` or similar): `merge/<from>-to-<to>-MMDDYYYY`, e.g. `merge/develop-to-main-05112026`. The date is the date the merge branch is created.
- **Do not create branches with new prefix directories** (e.g. `docs/`, `chore/`) without asking first. Stick to the established prefixes above.

### Keeping main and develop in sync

- **Features** go into `develop` first. When ready, merge `develop` into `main` using a **regular merge commit** (not squash merge). Squash merges create new commit hashes that prevent git from recognizing shared history, causing conflicts on future merges.
- **Hotfixes** land on `main` first (for urgent shipping), then get **cherry-picked into `develop` immediately** — same day, before more work lands on either branch.
- When merging `develop` → `main`, always use `git merge` (not squash) so both branches maintain shared commit history.

## Key Conventions

- **Singleton + object pattern** for SDK entry points (`AttentiveSdk`, `AttentivePush`)
- **Builder pattern** for all public-facing models (config, events, identifiers)
- **Coroutine-first async design** with callback wrappers for Java interop
- **Public API** lives in top-level packages; internal implementation details stay in `internal` subpackages
- **Persistence** goes through `PersistentStorage` (SharedPreferences wrapper) — never access SharedPreferences directly
- Use the SDK logger — never `println()` or `Log.d()` directly
- Domain validation rejects URLs containing scheme/path separators — pass bare hostnames

## Architecture

### Initialization
- `AttentiveSdk` (Kotlin object) is the main public API — singleton facade
- `AttentiveConfig.Builder` constructs config with validation, wires up all dependencies
- `ClassFactory` builds internal components: `PersistentStorage`, `VisitorService`, `SettingsService`, `OkHttpClient`, `AttentiveApi`

### Event Pipeline
- Public event types: `PurchaseEvent`, `AddToCartEvent`, `ProductViewEvent`, `CustomEvent`
- One event can produce multiple API requests (e.g., Purchase with 3 items = 3 Purchase + 1 OrderConfirmed)
- Flow: `AttentiveSdk.recordEvent()` → `AttentiveEventTracker` → mapper → `AttentiveApi` → network
- Events enriched with user identifiers (email/phone Base64-encoded)
- Kotlin serialization with polymorphic metadata (`className` discriminator)

### Networking
- OkHttp with interceptor chain: `UserAgentInterceptor` → `HttpLoggingInterceptor`
- Domain passed directly by the developer — no geo-domain resolution

### Creatives
- WebView-based display system
- Communication via `WebMessageListener` JSON messages (OPEN/CLOSE/RESIZE_FRAME)
- Touch filtering: only touches within creative bounds pass to WebView
- Lifecycle-aware: auto-destroys via `CreativeActivityCallbacks` on API 30+

### User Identification
- `UserIdentifiers` with builder pattern — holds visitorId, clientUserId, email, phone, shopifyId, klaviyoId, customIdentifiers
- Visitor ID: auto-generated UUID, persisted in SharedPreferences, regenerated on `clearUser()`
- `identify()` merges new identifiers with existing (second overwrites first)
- `clearUser()` resets all identifiers and generates new visitor ID

### Push Notifications
- `AttentivePush` singleton handles token management and notification display
- `TokenProvider` wraps Firebase token retrieval with caching
- `AppLaunchTracker` observes `ProcessLifecycleOwner` to detect notification-driven launches

### Inbox
- Compose-based UI (`AttentiveInbox`) driven by `StateFlow<InboxState>`
- Swipe gestures: right to delete, left to mark unread
- Offset-based pagination with debouncing and Mutex for thread safety
- Image loading via Coil3

## Testing

- **Framework**: JUnit 4 with Mockito
- **Test doubles**: Use the `FactoryMocks` helper for mocking SDK internals
- **Pattern**: Constructor-inject dependencies, then pass mocks in tests
- **Snapshot testing**: Roborazzi for Compose UI (`bonni` module)

## Do Not

- **Use `!!` (force unwrap)** without clear justification — prefer safe-call (`?.`) or `requireNotNull` with a message
- **Access SharedPreferences directly** — go through `PersistentStorage`
- **Use `Log.d`/`println`** — use the SDK logger
- **Add dependencies** without team discussion — the SDK must stay lightweight
- **Expose internal types** in the public API surface
- **Hardcode URLs or domains** — accept them via config
