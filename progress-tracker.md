# Progress Tracker

Update this file after every meaningful implementation or planning change. Separate documented plans from code that has actually been built and verified.

## Current Phase

- **Task 4 — Local Folder Library — implemented and verified.** The app selects folders through the native picker, persists grants and metadata in Room, scans direct children only, reconciles on refresh, and shows folders with availability states and per-folder media lists.
- **Not yet implemented:** Home feed, community drawer and custom communities, media rendering (Coil/Media3), immersive viewer, Saved copies, DataStore theme preferences, Settings.

## Current Goal

- Build the Reddit-style feed on top of the verified folder library: expose per-folder communities, add Random/Newest sorting and post cards, then render media.

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

### Scaffold (implementation — Task 3)

- Confirmed the stack and app identity (see "Architecture Decisions"); `architecture.md` and `project-overview.md` were updated from "recommended/not set" to confirmed.
- Created the standard single-module Android project at the repository root: `settings.gradle.kts`, root `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties`, and the `app/` module source set.
- Configured Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`) as the single source of dependency and plugin versions.
- Added the Gradle wrapper (9.6.0) including `gradle/wrapper/gradle-wrapper.jar`, with `gradlew` marked executable.
- Added `MainActivity` (single `ComponentActivity` using `setContent`) and one Compose screen that displays the app name, "Mosaic".
- Added a minimal `ui/theme` layer (`MosaicTheme`) mapping the semantic color tokens from `ui-context.md` onto Material 3 light/dark color schemes.
- Added the manifest (one launcher `Activity`, **no permissions declared**), string/theme/color resources, a vector four-tile adaptive launcher icon (no binary image assets), `proguard-rules.pro`, and an Android/Gradle `.gitignore`.
- Updated `README.md` with project information, requirements, build commands, and configuration.

### Local folder library (implementation — Task 4)

- **Dependencies** (all versions in the catalog): Room 2.8.5 with KSP 2.2.10-2.0.2 (processor pinned to the bundled Kotlin 2.2.10), `lifecycle-viewmodel-compose` and `lifecycle-runtime-compose` 2.11.0 (via `lifecycle`), and `kotlinx-coroutines-test` 1.11.0 (test only). No Hilt/Dagger: a small manual container in `MosaicApplication` builds Room plus the repository.
- `core/model/` — domain models: `MediaKind` (`IMAGE`, `GIF`, `VIDEO`), `SourceFolder` (with `FolderAccessStatus` and last scan failure), `MediaItem`, `FolderCommunity`, `ScanFailure`.
- `data/local/` — Room `MosaicDatabase` v1 (`mosaic.db`) with `SourceFolderEntity` (unique `treeUri` — the duplicate-selection guard), `CommunityEntity` (unique `folderId` — exactly one community per folder), `MediaItemEntity` (primary key = document URI, so files with identical display names stay distinct; `folderId` FK with `CASCADE`), the `FolderDao`/`MediaDao`/`CommunityDao` trio, and entity↔domain mappers.
- `data/source/` — `MediaTreeSource`, the document-provider seam; `AndroidMediaTreeSource` (SAF `DocumentsContract` queries of direct children only, persistable grant take/release, explicit handling of null cursors, revoked grants, missing documents, provider errors, and I/O failures); `MediaTypeClassifier` (provider MIME type first, conservative extension fallback); `MediaIndex` (pure indexing: directory/unsupported exclusion, ordering, stable identity); `DocumentChild` model.
- `data/repository/` — `FolderLibraryRepository`: `addFolder` (dedupe by normalized tree URI, persistable grant, community auto-creation, initial index), `removeFolder` (deletes only Mosaic rows and releases the persisted grant; never touches source files), and `refreshAll` (per-folder scan; stale media rows are removed only after that folder's scan succeeds; a failed scan marks the folder unavailable and preserves its last-known media and community for retry on a later refresh). `FolderLibraryResults` holds the typed results/messages.
- `feature/folders/` — `FolderLibraryViewModel` (StateFlow UI state: loading/refreshing, folder summaries with status and media count, typed user messages) with `FolderLibraryScreen` (Mosaic title, Add folder, Refresh, folder list with availability and counts, empty/loading/error/unavailable states, remove-with-confirmation dialog) and `FolderMediaScreen` (a folder's indexed direct children with filename and media kind).
- `MainActivity` stays thin: theme surface hosting `FolderLibraryRoute`. The `OpenDocumentTree` launcher and persistable-grant handling live in the feature route; no provider or database work happens in composables or the activity.
- Refresh triggers: `ON_RESUME` via `LifecycleEventEffect`, the manual Refresh action (which additionally reports unavailable folders), and immediately after adding a folder.
- **Manifest permissions remain empty**: no `INTERNET`, `MANAGE_EXTERNAL_STORAGE`, or storage/media permissions; verified in the merged manifest and the built APK.
- `gradle.properties` — memory settings for the 2 GB build sandbox (in-process Kotlin compilation, capped heap, single worker) plus `android.disallowKotlinSourceSets=false`, the exact suppression AGP 9 names when KSP registers generated sources through `kotlin.sourceSets`; required for Room codegen with AGP's built-in Kotlin.

## In Progress

- None.

## Verification Results

Environment used for both rounds: JDK 17.0.20.1, Android SDK with `platforms/android-37.0` and `build-tools/37.0.0` (plus `36.0.0`, which AGP 9.4.1 requested and installed itself), Gradle 9.6.0 via the wrapper, on a 2 GB / 1 CPU sandbox.

### Task 4 — Local Folder Library (2026-10-11, run after the last source edit)

| Command | Result |
| --- | --- |
| `./gradlew testDebugUnitTest` | **Succeeded** — 33 tests, 0 failures, 0 errors, 0 skipped, across `MediaTypeClassifierTest` (6), `MediaIndexTest` (9), `FolderLibraryRepositoryTest` (18). Covers media-type detection, nested-directory exclusion, empty-scan-as-valid-state, adding new media, reconciling disappeared media, preserving media after a failed scan, duplicate-selection handling, stable identity for duplicate display names, provider failure never reported as empty, and unavailable→restored access recovery. |
| `./gradlew lintDebug` | **Succeeded** — `BUILD SUCCESSFUL`, 0 errors, 3 warnings. The same three pre-existing advisories listed below; the task introduced no new warnings. |
| `./gradlew assembleDebug` | **Succeeded** — `BUILD SUCCESSFUL`, producing `app/build/outputs/apk/debug/app-debug.apk` (12,512,746 bytes). |
| Manifest/APK permission audit | No `INTERNET`, `MANAGE_EXTERNAL_STORAGE`, `READ_/WRITE_EXTERNAL_STORAGE`, or any other permission in the merged manifest or the APK's `badging` output. |
| Device or emulator run | **Not performed.** `adb devices` reports no attached device and no emulator is installed in this environment, so the picker workflow has not been exercised on a device. Only build, lint, and unit-test verification is claimed. |

### Task 3 — Scaffold (2026-10-11)

| Command | Result |
| --- | --- |
| `./gradlew --version` | Succeeded — Gradle 9.6.0, launcher JVM 17.0.20.1. Confirms the wrapper is present, executable, and functional. |
| `./gradlew assembleDebug` | **Succeeded** — `BUILD SUCCESSFUL`, 36 tasks. Produced `app/build/outputs/apk/debug/app-debug.apk` (11,615,210 bytes). |
| `./gradlew lintDebug` | **Succeeded** — `BUILD SUCCESSFUL`, 0 errors, 3 warnings (see below). |
| `./gradlew testDebugUnitTest` | **No test sources exist** — task reported `NO-SOURCE` and `BUILD SUCCESSFUL`. No tests were written and no coverage is claimed. |
| APK inspection (`aapt2 dump badging`) | Confirmed `package name='com.samprogram.mosaic'`, `versionCode='1'`, `versionName='0.1.0'`, `minSdkVersion:'26'`, `targetSdkVersion:'37'`, `compileSdkVersion='37'`, `application-label:'Mosaic'`. |

### Known lint warnings (0 errors, unchanged by Task 4)

1. `AndroidGradlePluginVersion` — "A newer version of Gradle than 9.6.0 is available: 9.8.1". Left as-is: 9.6.0 is the documented default/minimum pairing for AGP 9.4.1; the build is verified green on it.
2. `NewerVersionAvailable` — "A newer version of `org.jetbrains.kotlin.plugin.compose` than 2.2.10 is available: 2.4.21". **Intentional:** the Compose compiler plugin version must match the Kotlin version AGP 9.4.1 bundles (its POM depends on `kotlin-gradle-plugin` 2.2.10), so 2.2.10 is the correct value, not the newest.
3. `ObsoleteSdkInt` — lint suggests merging `res/mipmap-anydpi-v26` into `res/mipmap-anydpi` since `minSdkVersion` is 26. **Verified not to work:** with the folder renamed to `mipmap-anydpi`, `processDebugResources` fails with `AAPT: error: resource mipmap/ic_launcher not found` — AAPT2 requires the `-v26` qualifier for `<adaptive-icon>`. The standard version-qualified layout is retained deliberately.

## Next Up

1. Communities + feed: surface the auto-created per-folder communities in a Reddit-style drawer, then the Home feed with Random/Newest sorting and post cards over the existing media index.
2. Media rendering: Coil for image/GIF display in cards, then Media3 for muted MP4 playback.
3. Immersive viewer with vertical paging over the exact feed sequence and lifecycle-safe playback.
4. Saved copies (independent app-managed copies), then DataStore-backed light/dark theme selection and Settings.
5. Exercise the folder-picker workflow on a real device or emulator once one is available, and record the outcome here.

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
- **Room needs KSP under AGP's built-in Kotlin (2026-10-11):** KSP 2 registers generated sources through `kotlin.sourceSets`, which AGP 9 disallows by default; `android.disallowKotlinSourceSets=false` in `gradle.properties` is the suppression AGP itself names and is required for Room codegen.
- **Manual dependency injection (2026-10-11):** a single container class in `MosaicApplication` provides Room and the repository; no Hilt/Dagger until a feature demonstrably needs them.
- **Document-provider seam (2026-10-11):** scanning goes through the `MediaTreeSource` interface so indexing and reconciliation logic is unit-testable with fakes, keeping device/provider concerns out of repository tests.
- **Reddit reference behavior:** use Reddit-style community drawer, feed sorting and post-card composition, Saved/menu access, and light/dark presentation. Product-specific vertical swipe navigation and removal of comments remain authoritative.

## Session Notes

- Task 4 implemented the folder library end to end within its scope: picker → persistable grant → Room records → direct-child scan → safe reconciliation → Material 3 UI, plus unit tests over fakes for every device-dependent seam.
- All provider/database work is reachable only through the repository; composables and `MainActivity` hold no I/O logic.
- `FolderLibraryRepositoryTest` uses a fake `MediaTreeSource` and in-memory fake DAOs (no Robolectric), so scan, failure, and recovery paths run as plain JVM tests.
- The build sandbox has 2 GB RAM and 1 CPU; `gradle.properties` caps the Gradle heap, runs Kotlin compilation in-process, and limits workers to 1 to keep the OS OOM killer from terminating builds. These settings are safe on larger machines but trade away build parallelism.
- The first attempt at the KSP/AGP combination failed until `android.disallowKotlinSourceSets=false` was added; this is a documented AGP flag, not a workaround, and should be dropped when KSP supports built-in Kotlin natively.
- The scaffold's original "no test dependencies" stance was replaced for this task: `kotlinx-coroutines-test` was added so repository behavior can be verified, and 33 real tests now exist.
- No emulator or device exists in this environment, so the picker flow was verified only through unit tests and code review; on-device verification remains outstanding.
