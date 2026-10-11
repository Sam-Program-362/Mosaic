# Architecture Context

## Stack

The project is Android-native. The stack below was **confirmed by the user on 2026-10-11** for the scaffold, together with the app identity `com.samprogram.mosaic`, `minSdk` 26, and `compileSdk`/`targetSdk` 37. Record any change in this file.

| Layer | Technology | Role |
| --- | --- | --- |
| Language | Kotlin | Primary application language, coroutines, sealed result/state types |
| UI | Jetpack Compose + Material 3 | Native declarative Android UI; reproduce Reddit-like feed and navigation patterns using themed components |
| Navigation | AndroidX Navigation Compose or a small typed Compose navigation layer | Home feed, communities, immersive viewer, Saved, folder management, and Settings |
| Folder access | Android Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`) | User-mediated folder selection and persistable URI grants; no broad storage permission |
| Local metadata | Room | Persist selected-folder records, community definitions, folder-community memberships, cached media metadata, and Saved-copy records |
| Preferences | Preferences DataStore | Persist theme and simple user preferences such as default feed sort and video sound preference if selected |
| Image/GIF rendering | Coil with an appropriate GIF decoder, or a compatible Android image loader selected during implementation | Load local content URIs and animate GIFs efficiently |
| Video playback | AndroidX Media3 ExoPlayer | Inline MP4 playback and immersive viewer playback, mute/unmute controls, lifecycle/resource management |
| Background/async work | Kotlin coroutines and Flow; WorkManager only if a later requirement truly needs deferrable background work | Scan folders off the main thread and expose data changes to UI; v1 does not require continuous background scanning |
| Tests | Kotlin/JUnit, AndroidX test tooling, Compose UI testing | Unit, integration, and UI verification |
| Version management | Gradle Kotlin DSL and version catalog | Centralize dependency/plugin versions; choose compatible stable versions at implementation time rather than hardcoding stale version numbers here |
| Authentication | None | Personal single-device local application; no accounts or identity provider |
| Network/backend | None | Core runtime must work offline; no API server or cloud database |

## System Boundaries

Recommended package boundaries under `app/src/main/java/<package>/`:

- `app/` — application entry point, top-level navigation, and activity setup. Do not place repository or media-indexing logic here.
- `core/model/` — domain models and typed identifiers for source folders, media items, communities, and Saved copies.
- `core/result/` — shared error/result representations if required; keep errors typed and user-actionable.
- `data/local/` — Room database, entities, DAOs, and database migrations.
- `data/source/` — Storage Access Framework access, persisted-grant checks, direct-child enumeration, MIME/extension detection, and source-file copying.
- `data/repository/` — repositories that combine Room metadata and current document-provider state; owns refresh/reconciliation logic.
- `feature/feed/` — Home feed, Random/Newest sort control, post cards, simulated counts, and feed state.
- `feature/communities/` — community drawer, per-folder communities, custom-community creation, and folder-community membership UI.
- `feature/viewer/` — immersive viewer, cross-media vertical paging, video/GIF playback lifecycle, and sound toggle.
- `feature/saved/` — Saved-copy browser and copy records; source-folder availability must not determine Saved-copy availability.
- `feature/folders/` — adding folders, access status, and folder management.
- `feature/settings/` — theme and other local preferences.
- `ui/theme/` — semantic color tokens, typography, shapes, and light/dark Material theme.
- `ui/components/` — shared UI primitives only; feature-specific state and business rules stay within feature/data layers.
- `*/test/` or matching test source sets — tests near the module/package they verify.

The project is now scaffolded as a single `:app` Gradle module at the repository root containing only the application entry point. The remaining package boundaries above are the agreed organization for the feature work that follows and have not all been created yet.

## Storage Model

- **Selected-folder access:** persist each chosen tree URI and a display label in local metadata. Call the Android API to retain the URI permission where supported; verify grants at refresh time. A URI that cannot be read must transition to an unavailable state rather than being silently deleted.
- **Source media:** source image/GIF/MP4 files remain in their original locations. Store the content URI/document ID, source folder ID, display name, MIME/media kind, modification time when available, and other lightweight metadata needed for feed rendering. Do not import/copy every source file into the app's storage.
- **Media index:** use Room for durable folder/community relationships and media metadata. Refresh by enumerating only direct children of each selected tree. Never recurse automatically.
- **Communities:** store one automatically created community per selected folder, plus custom communities and their many-to-many folder memberships. If duplicate folder names occur, preserve unique internal IDs; a collision-safe display label may be added without changing the source folder name.
- **Saved copies:** copy the bytes to app-managed persistent files storage using a unique collision-safe destination name/ID. Store a Saved-copy row with destination path, original display name, media kind, save timestamp, and optional source reference for provenance. Saved playback/rendering must use the copied file and must not depend on source access. Saving twice creates two records and two file copies.
- **Preferences:** use DataStore for theme (`light`/`dark`) and small display preferences. Persist any selected default sort only if implemented; Random and Newest must always remain user-accessible.
- **Simulated vote counts:** ephemeral UI state only. Do not persist counts as user voting data. Regenerate on app start and feed refresh.
- **Saved media cleanup:** no automatic deletion or deduplication is permitted in v1. If manual removal of Saved copies is later added, it must only delete the app-managed copy, never the source.

## Auth and Access Model

- No sign-in, account, identity provider, or remote authorization model exists.
- The app may read only folders explicitly selected through Android's native document picker and only while the OS grants access.
- Request persistable URI permission from the picker result when available and store the URI reference. Do not request `MANAGE_EXTERNAL_STORAGE` or broad shared-storage access for the core feature.
- Treat document-provider URIs and metadata as external input: validate null/unsupported MIME data and handle provider failures, revoked permissions, missing documents, and I/O exceptions.
- Keep Saved copies inside app-managed persistent storage. The app needs no source-folder access to render them.
- Add no network permission, analytics SDK, ads SDK, crash upload service, or cloud dependency unless the user explicitly changes the offline/privacy requirements.

## Invariants

1. **Offline runtime:** browsing, feed sorting, communities, viewer playback for local files, Saved copies, and settings must not require a network connection.
2. **User-selected scope only:** never scan arbitrary device storage; use only explicitly selected folders and their direct children.
3. **No recursive scan:** files inside nested folders are ignored unless that nested folder is separately selected.
4. **Originals are immutable:** never rename, move, overwrite, or delete source media. Save always writes a new copy.
5. **Saved independence:** Saved items remain usable if their source file is deleted or its folder access is lost.
6. **Unavailable is not deleted:** losing access to a selected folder marks its community/source unavailable; preserve metadata and attempt recovery on later refresh when access returns.
7. **Community isolation:** each folder community contains files from that folder only; each custom community contains files from its configured folder set only.
8. **Fake votes only:** up/down counts are presentation-only and never change media, storage records, or any remote resource.
9. **No comments:** do not add comments, comment routes, or comment buttons.
10. **Consistent viewer sequence:** vertical swipes traverse the same media collection and sort order from which the viewer was opened; the sequence may change only after a deliberate refresh/reload or navigation to another feed.
11. **Playback lifecycle:** pause/release off-screen media and release player resources when the owning screen is disposed or backgrounded as appropriate.
12. **Document synchronization:** a scan must not delete a whole community because a provider errors. Reconcile source item deletions only when a successful scan establishes that a previously listed file is gone.
