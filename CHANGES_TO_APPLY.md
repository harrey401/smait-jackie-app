# Exact Changes to Apply

## 1. build.gradle.kts (app level)

Add after the existing `dependencies {` block entries:

```kotlin
// CAE SDK
implementation(files("libs/cae.jar"))
implementation(files("libs/AlsaRecorder.jar"))
```

Add inside `android {` block:

```kotlin
sourceSets {
    getByName("main") {
        jniLibs.srcDirs("libs")
    }
}
```

## 2. AndroidManifest.xml

Add these permissions (before the `<application>` tag):

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
    tools:ignore="ScopedStorage" />
```

## 3. MainActivity.kt Changes

### Remove these imports:
```kotlin
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
```

### Remove these fields:
```kotlin
// ─── Audio ───
private var audioRecord: AudioRecord? = null
private var audioThread: Thread? = null
private var aec: AcousticEchoCanceler? = null
private var noiseSuppressor: NoiseSuppressor? = null
```

### Replace with:
```kotlin
// ─── Audio (CAE Beamforming) ───
private lateinit var caeAudio: CaeAudioManager
```

### Remove these constants from companion object:
```kotlin
private const val SAMPLE_RATE = 16000
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
```

### In onCreate(), add after `prefs = ...`:
```kotlin
caeAudio = CaeAudioManager(this)
caeAudio.copyAssetsIfNeeded()
```

### Replace the entire startAudioCapture() method with:
```kotlin
private fun startAudioCapture() {
    webSocket?.let { ws ->
        caeAudio.start(ws)
        Log.i(TAG, "CAE beamformed audio capture started")
    }
}
```

### Replace the entire stopAudioCapture() method with:
```kotlin
private fun stopAudioCapture() {
    caeAudio.stop()
}
```

### In checkPermissions(), add storage permission:
```kotlin
val permissions = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.WRITE_EXTERNAL_STORAGE  // For CAE asset files
)
```

### In onDestroy(), the existing audio cleanup stays — stopStreaming() calls stopAudioCapture() which now calls caeAudio.stop()

## 4. Copy the Java source files

From the CAE demo into your app's java source folder:

```
com/voice/osCaeHelper/CaeCoreHelper.java
com/voice/caePk/OnCaeOperatorlistener.java
com/voice/caePk/util/FileUtil.java
```

### Edit CaeCoreHelper.java:
Change the ALSA device constants are NOT in CaeCoreHelper — they're in CaeAudioManager.kt.
CaeCoreHelper only handles the CAE engine init. No changes needed IF the auth token works.

If auth fails, you may need to get a valid token from your lab's iFlytek account.

## 5. That's it!

Build → install → test. Stop run_jackie.py on lab PC first so the USB device is free.

The audio pipeline is now:
  USB Mic Array (8ch 16-bit) → ALSA → Channel Adapter → CAE Engine → Beamformed Mono → WebSocket → PC
