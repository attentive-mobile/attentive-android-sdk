# Bonni App Signing and Distribution

This document explains how code signing works for the Bonni demo app and how it integrates with CircleCI for automated distribution.

## Overview

The Bonni app uses Android's standard APK signing for release builds. The signing configuration supports two modes:
1. **Local development** - Uses a local `keystore.properties` file
2. **CI/CD (CircleCI)** - Uses environment variables

## Environment Variables

### Code Signing Variables

| Variable | Purpose | How to Generate |
|----------|---------|-----------------|
| `BONNI_KEYSTORE_BASE64` | The keystore file, base64 encoded | `base64 -i bonni/bonni-release.keystore` |
| `BONNI_STORE_PASSWORD` | Password to access the keystore file | From `keystore.properties` |
| `BONNI_KEY_ALIAS` | Alias of the signing key within the keystore | From `keystore.properties` |
| `BONNI_KEY_PASSWORD` | Password for the specific key | From `keystore.properties` |

### Firebase Distribution Variables

| Variable | Purpose | How to Generate |
|----------|---------|-----------------|
| `FIREBASE_APP_ID` | Identifies the app in Firebase (e.g., `1:123456789:android:abc123`) | Firebase Console > Project Settings > Your Apps |
| `FIREBASE_TOKEN` | Authentication token for Firebase CLI | Run `firebase login:ci` locally |

## How Code Signing Works

### Decision Flow

```
┌─────────────────────────────────────────────────────────┐
│                    Gradle Build Starts                   │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │ BONNI_STORE_PASSWORD  │
              │   env var exists?     │
              └───────────┬───────────┘
                          │
            ┌─────────────┴─────────────┐
            │                           │
           YES                          NO
            │                           │
            ▼                           ▼
┌───────────────────────┐   ┌───────────────────────┐
│ Use environment       │   │ keystore.properties   │
│ variables for signing │   │ file exists?          │
└───────────────────────┘   └───────────┬───────────┘
                                        │
                          ┌─────────────┴─────────────┐
                          │                           │
                         YES                          NO
                          │                           │
                          ▼                           ▼
              ┌───────────────────────┐   ┌───────────────────────┐
              │ Use local properties  │   │ Signing not           │
              │ file for signing      │   │ configured (debug     │
              └───────────────────────┘   │ builds only)          │
                                          └───────────────────────┘
```

### Local Development

For local development, create a `bonni/keystore.properties` file:

```properties
storeFile=bonni-release.keystore
storePassword=<password>
keyAlias=<alias>
keyPassword=<password>
```

**Note:** This file is gitignored and should never be committed.

### CI/CD (CircleCI)

In CircleCI, the signing process works as follows:

1. **Decode Keystore**: The base64-encoded keystore is decoded and written to `bonni/bonni-release.keystore`
2. **Environment Detection**: Gradle detects `BONNI_STORE_PASSWORD` is set and uses environment variables
3. **Build**: The release APK is built and signed
4. **Distribute**: The signed APK is uploaded to Firebase App Distribution

## CircleCI Workflow

### Jobs

| Job | Purpose | When it Runs |
|-----|---------|--------------|
| `lint` | Runs Android Lint and ktlint | Every push |
| `unit-test` | Runs unit tests with coverage | Every push |
| `instrumented-test` | Runs emulator-based tests | Every push |
| `build` | Builds the SDK AAR | After lint and unit-test pass |
| `distribute-bonni` | Builds and distributes Bonni app | Only on `main` branch, only when versionCode changes |

### Distribution Trigger

The `distribute-bonni` job includes a version check:
- Compares the current `versionCode` in `bonni/build.gradle` with the previous commit
- If unchanged, the job halts early (no build or distribution)
- If changed, proceeds with build and Firebase upload

This prevents unnecessary distributions when only SDK code changes.

## File Locations

| File | Purpose | Committed to Git? |
|------|---------|-------------------|
| `bonni/bonni-release.keystore` | The actual keystore file | No (gitignored) |
| `bonni/keystore.properties` | Local signing credentials | No (gitignored) |
| `bonni/build.gradle` | Signing configuration logic | Yes |
| `.circleci/config.yml` | CI/CD pipeline definition | Yes |

## Setting Up a New Environment

### For Local Development

1. Obtain the keystore file (`bonni-release.keystore`) from a team member
2. Place it in the `bonni/` directory
3. Create `bonni/keystore.properties` with the credentials
4. Build normally with `./gradlew :bonni:assembleRelease`

### For CircleCI

1. Go to CircleCI Project Settings > Environment Variables
2. Add all six environment variables listed above
3. For `BONNI_KEYSTORE_BASE64`, run locally:
   ```bash
   base64 -i bonni/bonni-release.keystore | pbcopy
   ```
   Then paste the value in CircleCI

## Security Considerations

- The keystore file and credentials are **never** committed to the repository
- CircleCI masks all secret values in build logs
- Environment variables are stored encrypted in CircleCI
- Only the `main` branch triggers distribution (not feature branches)

## Troubleshooting

### "Keystore file not found" Error

- **In CI**: Verify `BONNI_KEYSTORE_BASE64` is set correctly and the base64 encoding is valid
- **Locally**: Ensure `bonni/bonni-release.keystore` exists and `keystore.properties` points to it

### Distribution Not Triggering

- Check that you're on the `main` branch
- Verify the `versionCode` in `bonni/build.gradle` was incremented
- Check CircleCI for the job output

### Firebase Upload Fails

- Verify `FIREBASE_APP_ID` matches your app in Firebase Console
- Regenerate `FIREBASE_TOKEN` with `firebase login:ci` if expired
- Ensure the `--groups` parameter matches an existing tester group
