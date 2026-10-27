# Mudeer Video Transformer (MVT) — starter project

This is an Android Studio source project for an early MVT prototype.

## Included
- Kotlin + Jetpack Compose interface
- Android system video picker
- Random clip on every export: random start point, random length 16-30 seconds
- Live export progress percentage
- Media3 Transformer export to a 1080×1920 (9:16) canvas
- Fill/crop or fit-whole-picture framing
- Source audio retained by the Media3 transformation pipeline
- Attempts to copy completed exports to `Movies/MudeerVideoTransformer`

## Not included yet
- Best-moment detection (start point is random for now)
- Face/subject tracking
- Subtitle transcription or burned-in captions
- Watermark masking
- In-app video preview/timeline

## Open/build
1. Extract this ZIP.
2. Open the `MudeerVideoTransformer` folder in Android Studio.
3. Allow Gradle sync and install the Android SDK platform requested by the project (API 35).
4. Build and run on an Android device.

## Important verification note
This project has not been built or tested in an Android SDK environment here. Treat it as a starter source project, not a verified APK. If Android Studio reports an API or Gradle error, share the first error block so it can be corrected.

## Build the APK on GitHub (no Android Studio needed)
Push to `main`, open the repo's **Actions** tab, run **Build APK**, then download `MVT-debug-apk` from the run's Artifacts and install it on your phone.
