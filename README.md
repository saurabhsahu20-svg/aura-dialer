# Aura Dialer (Android 14 beta)

An experimental native Java Android dialer with:
- Dark dial pad, contacts, and recent calls
- Optional Android default phone app role
- Full-screen contact photo when a saved contact has one
- Incoming call controls (answer/decline)
- Active call controls (end, mute, speaker, hold when supported, DTMF keypad)

## Build an APK on GitHub (no computer needed)

1. Open the repository's **Actions** tab.
2. Choose **Build Aura Dialer APK**.
3. Open a successful run, scroll to **Artifacts**, and tap **Aura-Dialer-debug-apk**.
4. Extract the ZIP file and install `app-debug.apk`.

The workflow runs automatically on pushes to `main`. If no workflow appears, check the repository Actions settings and ensure workflows are permitted.

## Safety and feature limitations

**This is an untested beta, not a dependable replacement for your phone app.** Build success means the APK compiled, not that calls work reliably. Keep your existing phone app installed; do not rely on Aura Dialer for emergency calls. Test incoming and outgoing calls with another phone before considering it your default dialer.

Android 14 can limit full-screen incoming-call notifications. The caller photo only appears if Android grants contacts access and the saved contact has a photo. Dual-SIM selection, conference calling, Bluetooth routing, and recording are not implemented. The notification and in-call screen behavior require on-device testing.

## Build requirements

GitHub Actions, JDK 17, Gradle 8.2.1, Android Gradle Plugin 8.2.2, Android SDK 34.
