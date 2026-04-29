# smAIT Office App (Jackie)

Android app for the **Jackie** humanoid robot — the productized office-deployment build.
Package: `com.smait.jackie`. Target device: Jackie (armeabi-v7a, 32-bit ARM).

## Pull & Deploy to Jackie

```bash
git clone -b smait-office-app https://github.com/harrey401/smait-jackie-app.git
cd smait-jackie-app
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The output APK lands at `app/build/outputs/apk/debug/app-debug.apk` (~54 MB).

## ABI

Production builds ship **armeabi-v7a only**. Reasons:
- Jackie is a 32-bit ARM device.
- iFLYTEK CAE native libs (`libcae-jni.so`, `libhlw.so`) only exist for armeabi-v7a.
- Rive native runtime (`librive-android.so`) is bundled for armeabi-v7a.

To preview UI in an x86_64 Android emulator, temporarily edit `app/build.gradle.kts`:

```kotlin
ndk {
    abiFilters += listOf("armeabi-v7a", "x86_64")  // do not commit
}
```

## Features

- **Home** — 4 action cards (Ask Me Anything, Office Tour, Photo, Website). Long-press the SMAIT logo to open Settings.
- **Ask Me Anything** — voice conversation with LLM via WebSocket; transcript + animated Rive bear with lip-sync (PCM RMS amplitude → `Talk` boolean).
- **Office Tour** — map-driven walkthrough.
- **Photo** — 4-style portrait booth (Ghibli, Pixar 3D, Cyberpunk, Claymation) + normal camera.
- **Website** — embedded WebView for smait.ai.

## Robot Character

Rive-animated talking bear at `app/src/main/res/raw/jackie_character.riv`.
State machine `State Machine 1` exposes:

| Input    | Type    | Used for                          |
|----------|---------|-----------------------------------|
| `Talk`   | Boolean | Lip-sync, driven by TTS amplitude |
| `Hear`   | Boolean | Listening state                   |
| `Check`  | Boolean | Thinking state                    |
| `Hide`   | Boolean | Hide character                    |
| `Look`   | Number  | Gaze direction (0–100)            |
| `success`| Trigger | Celebrate                         |
| `fail`   | Trigger | Frown                             |

`RobotAvatar.kt` enumerates inputs at runtime via `InputCatalog`, so swapping the `.riv` file with a different state machine won't crash — missing inputs are silently skipped.

## LLM-Driven Robot Actions

Server can push actions over the existing WebSocket as JSON:

```json
{"type": "robot_action", "action": "celebrate"}
{"type": "robot_action", "action": "frown"}
{"type": "robot_action", "action": "wave", "duration_ms": 2000}
{"type": "robot_action", "action": "look", "value": 75}
```

Routed by `RobotActionController` → `RobotAvatar` Rive inputs.

## Dependencies of Note

- `compileSdk = 35`, `minSdk = 24`, `targetSdk = 35`
- Kotlin + Compose (BOM-managed)
- `app.rive:rive-android:9.5.3` (9.5.3 is the last version compatible with compileSdk 35; 11.x requires compileSdk 36)
- MediaPipe `tasks-vision:0.10.14` for Follow Mode face tracking
- CameraX 1.3.1
- iFLYTEK CAE (jar at `app/libs/cae.jar` + `app/libs/armeabi-v7a/`)

## Permissions Requested

CAMERA, RECORD_AUDIO, INTERNET, WAKE_LOCK, READ/WRITE/MANAGE_EXTERNAL_STORAGE.
The activity is locked to landscape with `keepScreenOn=true`.
