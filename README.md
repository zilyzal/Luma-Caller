# Luma

A liquid-glass phone and messages app for Android. One app for calls and texts: dual-SIM keypad, recents, contacts, SMS conversations, swipe-to-answer incoming calls, and full-screen contact photos during calls.

## Install

Download `Luma.apk` from the [latest release](../../releases/latest), open it on your phone and allow installs from your browser or files app. On first launch Luma asks for permissions and to become your default phone and messages app.

## Build

The APK is built by GitHub Actions on every push to `main` (see `.github/workflows/build.yml`). To build locally you need JDK 17, the Android SDK (platform 35) and Gradle 8.9:

```
gradle :app:assembleRelease
```

## Notes

- Kotlin + Jetpack Compose, minSdk 26, no third-party UI libraries.
- The signing key is created by the CI workflow and kept in the Actions cache, never in the repository. If the cache is ever cleared a new key is generated, and Android will ask you to uninstall the old build before installing the new one.
- MMS is not supported yet (plain SMS only).
