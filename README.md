# Dynamic Island for Android

A native Kotlin Android overlay that recreates the interaction model and visual language of a Dynamic-Island-style pill. It uses a foreground `TYPE_APPLICATION_OVERLAY` window, custom Canvas rendering, Android DynamicAnimation springs, notification listener access, media sessions, battery/ringer/Bluetooth broadcasts, calibration controls, haptics, and a GitHub Actions APK build.

## Build

Requires JDK 17. The included `gradlew` bootstraps Gradle 9.6 and runs the Android Gradle Plugin 9.4 build.

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Permissions

The first-run screen links to overlay permission, notification listener access, and battery optimization settings. Notification access is needed for notification interception and media-session discovery.

## Important Android platform limits

Android does not expose every iOS-only system event or hardware API. In particular, a third-party app cannot universally intercept another app's private call UI, manipulate the status bar/camera hardware, or toggle the torch from an arbitrary overlay without the appropriate platform API. The project therefore uses public Android APIs and explicit test events for those states.
