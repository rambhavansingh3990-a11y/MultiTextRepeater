# Multi Text Repeater

A lightweight Android app built with Kotlin and Jetpack Compose.

## Features
- Repeat multiple non-empty text lines independently
- Repeat count from 1 to 10,000
- New line, space, or comma separators
- Optional prefix and suffix
- Copy, share, and save generated output as a TXT file
- Output-size safety limit (2 MB)

## Build
Requires JDK 17, Android SDK, and Gradle 8.9.

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Automatic APK build
Every push to `main` runs the GitHub Actions workflow in `.github/workflows/build-apk.yml`. Download the APK from the workflow run's Artifacts section.
