# CAE SDK Beamforming Integration Guide

## Step 1: Copy Files

### Native Libraries
Copy the entire `armeabi-v7a` folder from the CAE demo into your app:

```
FROM: HRI-smait/docs/hardware-sdk/CAEDemoAIUI-4 MIC/CAEDemoAIUI/app/libs/
TO:   smait-jackie-app/app/libs/

Files to copy:
  libs/
    AlsaRecorder.jar
    cae.jar
    armeabi-v7a/
      libtinyalsa.so
      libbase.so
      libc++.so
      libunwind.so
      libhlw.so
      libalsa-jni.so
      liblzma.so
      libcae-jni.so
      libcutils.so
      libbacktrace.so
      libutils.so
```

You do NOT need: `AIUI.jar`, `libaiui.so`, `kwmusic-autosdk-v2.0.9.jar`, `kwylsdk.jar`

### Asset Files
Copy these into `smait-jackie-app/app/src/main/assets/`:

```
FROM: HRI-smait/docs/hardware-sdk/CAEDemoAIUI-4 MIC/CAEDemoAIUI/app/src/main/assets/
TO:   smait-jackie-app/app/src/main/assets/

Files:
  hlw.ini
  hlw.param
  res_cae_model.bin
  res_ivw_model.bin
```

### Java Source Files
Copy these into `smait-jackie-app/app/src/main/java/`:

```
FROM: HRI-smait/docs/hardware-sdk/CAEDemoAIUI-4 MIC/CAEDemoAIUI/app/src/main/java/
TO:   smait-jackie-app/app/src/main/java/

Folders/files needed:
  com/voice/osCaeHelper/CaeCoreHelper.java
  com/voice/caePk/OnCaeOperatorlistener.java
  com/voice/caePk/util/FileUtil.java
```

You do NOT need: CaeOperator.java (we replace it with CaeAudioManager.kt)

## Step 2: Edit CaeCoreHelper.java

Change the auth token — or better yet, check if the existing one works first.
The key change: we DON'T need wake word detection for SMAIT. We only need beamforming + noise suppression.

## Step 3: Modify hlw.ini

The CAE model resources will be copied to `/sdcard/cae/` at runtime.
No changes needed if you keep the default paths.

## Step 4: See the new files below for code changes

- `CaeAudioManager.kt` — new file, replaces AudioRecord with CAE SDK
- `MainActivity.kt` changes — swap audio capture methods
- `build.gradle.kts` changes — add jar dependencies
- `AndroidManifest.xml` changes — add storage permissions
