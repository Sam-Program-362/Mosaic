# Progress Tracker

Update this file after every meaningful implementation or planning change. Separate documented plans from code that has actually been built and verified.

## Current Phase

- **Android project scaffolded; the first debug build is verified.** The repository builds an installable debug APK.
- **No product features are implemented.** There is no folder selection, media scanning, feed, community, immersive viewer, or Saved functionality yet.

## Current Goal

- Provide a minimal, verified-buildable Kotlin + Jetpack Compose Android project so feature work can proceed incrementally against the confirmed product and architecture specification.

## Completed

### Specification (context package)

- Defined the app as a local-only Android phone media browser using Reddit's Android app as the navigation, feed, post-card, Saved, and theme reference.
- Defined source-folder selection through Android's native folder picker, persisted access where supported, direct-child-only scanning, and per-folder communities.
- Defined custom communities that combine selected folders.
- Defined supported initial media types as static photos, animated GIFs, and MP4 videos.
- Defined Random and Newest-first feed modes, decorative randomized vote counts, and the absence of comments or real voting.
- Defined immersive media viewing with vertical swipes across the same feed sequence; GIF/video autoplay while visible; MP4 playback muted by default with a sound toggle.
- Defined Save copy as an independent app-managed copy, allowing repeated copies and preserving Saved content after source deletion.
- Defined selected-folder additions/deletions refresh behavior and unavailable-folder retention/recovery.
- Defined switchable light and dark themes.
- Created all six context files and cross-checked the major feature, storage, UI, and workflow invariants for consistency.

### Scaffold (implementation)

- Confirmed the stack and app identity (see "Architecture Decisions"); `architecture.md` and `project-overview.md` were updated from "recommended/not set" to confirmed.
- Created the standard single-module Android project at the repository root: `settings.gradle.kts`, root `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties`, and the `app/` module source set.
- Configured Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`) as the single source of dependency and plugin versions.
- Added the Gradle wrapper (9.6.0) including `gradle/wrapper/gradle-wrapper.jar`, with `gradlew` marked executable.
- Added `MainActivity` (single `ComponentActivity` using `setContent`) and one Compose screen that displays the app name, "Mosaic".
- Added a minimal `ui/theme` layer (`MosaicTheme`) mapping the semantic color tokens from `ui-context.md` onto Material 3 light/dark color schemes.
- Added the manifest (one launcher `Activity`, **no permissions declared**), string/theme/color resources, a vector four-tile adaptive launcher icon (no binary image assets), `proguard-rules.pro`, and an Android/Gradle `.gitignore`.
- Updated `README.md` with project information, requirements, build commands, and configuration.

## In Progress

- No product implementation is in progress.

## Verification Results

Environment used: JDK 17.0.20.1, Android SDK with `platforms/android-37.0` and `build-tools/37.0.0` (plus `36.0.0`, which the Android Gradle Plugin 9.4.1 requested and installed itself), Gradle 9.6.0 via the wrapper.

| Command | Result |
| --- | --- |
| `./gradlew --version` | Succeeded — Gradle 9.6.0, launcher JVM 17.0.20.1. Confirms the wrapper is present, executable, and functional. |
| `./gradlew assembleDebug` | **Succeeded** — `BUILD SUCCESSFUL`, 36 tasks. Produced `app/build/outputs/apk/debug/app-debug.apk` (11,615,210 bytes). |
| `./gradlew lintDebug` | **Succeeded** — `BUILD SUCCESSFUL`, 0 errors, 3 warnings (see below). |
| `./gradlew testDebugUnitTest` | **No test sources exist** — task reported `NO-SOURCE` and `BUILD SUCCESSFUL`. No tests were written and no coverage is claimed. |
| APK inspection (`aapt2 dump badging`) | Confirmed `package name='com.samprogram.mosaic'`, `versionCode='1'`, `versionName='0.1.0'`, `minSdkVersion:'26'`, `targetSdkVersion:'37'`, `compileSdkVersion='37'`, `application-label:'Mosaic'`. |

### Known lint warnings (0 errors)

1. `AndroidGradlePluginVersion` — "A newer version of Gradle than 9.6.0 is available: 9.8.1". Left as-is: 9.6.0 is the documented default/minimum pairing for AGP 9.4.1; the build is verified green on it.
2. `NewerVersionAvailable` — "A newer version of `org.jetbrains.kotlin.plugin.compose` than 2.2.10 is available: 2.4.21". **Intentional:** the Compose compiler plugin version must match the Kotlin version AGP 9.4.1 bundles (its POM depends on `kotlin-gradle-plugin` 2.2.10), so 2.2.10 is the correct value, not the newest.
3. `ObsoleteSdkInt` — lint suggests merging `res/mipmap-anydpi-v26` into `res/mipmap-anydpi` since `minSdkVersion` is 26. **Verified not to work:** with the folder renamed to `mipmap-anydpi`, `processDebugResources` fails with `AAPT: error: resource mipmap/ic_launcher not found` — AAPT2 requires the `-v26` qualifier for `<adaptive-icon>`. The standard version-qualified layout is retained deliberately.

## Next Up

1. Implement the storage/access foundation first: select a folder via the Storage Access Framework, persist and re-check the grant, enumerate direct children, and represent unavailable folders. Add no broad storage permission.
2. Add the Room metadata model and successful-scan reconciliation; test source additions/deletions and recovery after access is restored.
3. Implement folder communities and custom community membership, then feed sorting and post cards.
4. Implement image/GIF/MP4 playback and the immersive vertical viewer with lifecycle-safe autoplay and mute controls.
5. Implement independent Saved copies, light/dark theme selection, folder management, and Settings.
6. Run the relevant unit/UI tests, lint, and debug build; record only real outcomes here.

## Open Questions

- Whether the initial Random behavior (shuffle on app start/refresh, no repeat until the list is exhausted) and extension-free display titles are acceptable defaults.
- `versionCode` 1 / `versionName` "0.1.0" are scaffold defaults; confirm before a release.
- Minimum supported Android version is fixed at API 26 for now; confirm before a production release.

## Architecture Decisions

- **Offline/local-only runtime:** no server, account, cloud synchronization, analytics, or ads.
- **Explicit selected-folder scope:** native Android folder picker and persisted URI permission where supported; no broad storage permission.
- **Direct children only:** nested folders are not traversed unless separately selected.
- **One community per selected folder:** automatically created using the folder name; custom communities combine chosen folders.
- **Source files are read-only:** source files are never modified, moved, renamed, overwritten, or deleted.
- **Saved copies are independent:** each save creates a unique app-managed copy; duplicates are intentional and survive source deletion.
- **Unavailable folders remain represented:** preserve the community and try again after access becomes available.
- **Fake vote counters:** regenerate on app start/feed refresh and never mutate real data.
- **Confirmed stack (user-confirmed 2026-10-11):** Kotlin, Jetpack Compose + Material 3, Room, DataStore, Android Storage Access Framework, compatible image/GIF loading, and AndroidX Media3. Navigation Compose to be added when the first multi-destination flow needs it.
- **Confirmed app identity (user-confirmed 2026-10-11):** public name "Mosaic"; namespace and application ID `com.samprogram.mosaic`; `minSdk` 26; `compileSdk` and `targetSdk` 37.
- **Confirmed build toolchain (2026-10-11):** Gradle Kotlin DSL with a version catalog; Android Gradle Plugin 9.4.1; Gradle 9.6.0; AGP's built-in Kotlin (so the Kotlin Android plugin is deliberately not applied) with the Compose compiler plugin pinned to the bundled Kotlin version 2.2.10; Java 17 source/target.
- **Reddit reference behavior:** use Reddit-style community drawer, feed sorting and post-card composition, Saved/menu access, and light/dark presentation. Product-specific vertical swipe navigation and removal of comments remain authoritative.

## Session Notes

- Task 3 scaffolded the Android project. Only the scaffold was implemented; no product features were started, per the task scope.
- The scaffold deliberately adds no navigation, persistence, media, or networking dependencies, because nothing currently needs them. Room, DataStore, Coil, Media3, and Navigation Compose will be added by the feature that first requires each one.
- No test dependencies and no tests were added; the task prohibited creating artificial tests to claim coverage.
- The launcher icon is drawn with vector drawables and an `<adaptive-icon>` rather than binary PNG mipmaps, so the repository contains no binary image assets.
- The build was run in a sandbox with no preinstalled JDK or Android SDK; JDK 17, the Android SDK platform/build-tools, and Gradle were installed locally to produce the verified results above. No SDK path is committed to the repository (the SDK is resolved via `ANDROID_HOME` or an untracked `local.properties`).
