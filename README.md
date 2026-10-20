<p align="center">
  <img src="docs/logo.png" width="128" alt="Luma logo">
</p>

<h1 align="center">Luma</h1>

<p align="center">
  Calls and messages in one liquid-glass Android app.
</p>

<p align="center">
  <a href="https://github.com/zilyzal/Luma-Caller/releases/latest/download/Luma.apk"><b>Download APK</b></a>
  &nbsp;·&nbsp;
  <a href="../../releases">All releases</a>
</p>

---

## Features

- **Phone and messages together.** Luma is your default dialer and your default SMS app.
- **Dual-SIM aware.** Two call buttons on the keypad (green for SIM 1, blue for SIM 2) and two send buttons in chats, so you pick the SIM every time.
- **Liquid-glass design.** Translucent layered surfaces, soft drifting glow background, springy press feedback and a liquid sliding tab indicator.
- **Keypad.** Clean number display, delete icon only when a number is typed, long-press `0` for `+`, and a quick "Add to contacts" popup that asks only for a name.
- **Recents, Messages and Contacts.** Full-screen lists with a search bar and no title bars.
- **Incoming calls.** Swipe to answer, full-screen contact photo when there is one, name only when there is not.
- **Arabic and RTL.** Each message bubble follows the direction of its own text.

## Install

1. Download [`Luma.apk`](https://github.com/zilyzal/Luma-Caller/releases/latest/download/Luma.apk) on your phone.
2. Open it and allow installs from your browser or files app.
3. On first launch, allow the permissions and set Luma as the default phone and messages app.

Requires Android 8.0 (API 26) or newer.

## Build

The APK is built by GitHub Actions on every push to `main` (see `.github/workflows/build.yml`) and published as a release. To build locally you need JDK 17, the Android SDK (platform 35) and Gradle 8.9:

```
gradle :app:assembleRelease
```

## Technical notes

- Kotlin and Jetpack Compose; no third-party UI libraries.
- Telecom `InCallService` for calls, Telephony provider for SMS, ContactsContract for contacts.
- The signing key is created by the CI workflow and kept in the Actions cache, never in the repository. If the cache is ever cleared a new key is generated, and Android will ask you to uninstall the old build before installing the new one.
- MMS is not supported yet (plain SMS only).
