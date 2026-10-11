# Mosaic

An offline Android media browser that turns folders you choose into a Reddit-style
feed. All content and settings stay on the device: no account, no server, and no
network access at runtime.

This repository currently contains the **project scaffold only**. Product features
(local media scanning, feeds, communities, immersive viewer, Saved) are not
implemented yet.

## Documentation

The authoritative project specification documents live at the repository root:

- [project-overview.md](project-overview.md) — product scope, features, and success criteria
- [architecture.md](architecture.md) — stack, package boundaries, storage model, and invariants
- [ui-context.md](ui-context.md) — navigation, component anatomy, themes, and design tokens
- [code-standards.md](code-standards.md) — Kotlin/Compose conventions, error handling, and testing rules
- [ai-workflow-rules.md](ai-workflow-rules.md) — workflow, verification gates, and protected boundaries
- [progress-tracker.md](progress-tracker.md) — current phase, completed work, next steps, and open decisions

## Requirements

- JDK 17
- Android SDK Platform 37 (Android 17), plus Build-Tools (installed automatically by
  the Android Gradle Plugin once SDK licenses are accepted)

Point Gradle at your SDK with the `ANDROID_HOME` environment variable, or create an
untracked `local.properties` file containing `sdk.dir=/path/to/android-sdk`.

## Build

```bash
./gradlew assembleDebug      # debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew lintDebug          # Android lint
./gradlew testDebugUnitTest  # unit tests (none exist yet)
```

## Configuration

| Setting | Value |
| --- | --- |
| Application ID / namespace | `com.samprogram.mosaic` |
| minSdk | 26 (Android 8.0) |
| compileSdk / targetSdk | 37 (Android 17) |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |

## Permissions

The app declares no permissions. Local media access will use Android's Storage
Access Framework when that feature is implemented, so the app never requests broad
storage access.
