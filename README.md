# CarNoiseAI

CarNoiseAI is an Android companion app for Bluetooth-connected driving profiles.

## Current features

- Select a paired Bluetooth device as the vehicle.
- Detect connect/disconnect broadcasts for the selected device.
- Automatically activate Android Do Not Disturb while the selected vehicle is connected.
- Configure whether calls, messages, media audio and alarms are allowed.
- Restore the phone's previous interruption filter after disconnecting (optional).
- Open Android Auto's app settings from the app.
- Dark midnight-blue interface with Home, Cars, Profile and Settings sections.

## Permissions and Android behavior

- Android 12+: Nearby devices / Bluetooth permission is required to read paired device names and addresses.
- The user must grant Notification Policy / Do Not Disturb access in Android settings before automatic DND can work.
- Android Auto does not expose a public API for third-party apps to edit its internal settings. This app provides a shortcut to its Android settings page.
- DND behavior can vary by Android version and device manufacturer. The app configures Android's priority policy to silence calls and messages by default while allowing media and alarms.

## Build

The GitHub Actions workflow builds a debug APK on pushes to main and uploads it as the CarNoiseAI-debug artifact.

To build locally with Android SDK 35 and Gradle 8.9, run: gradle --no-daemon assembleDebug

APK output: app/build/outputs/apk/debug/app-debug.apk
