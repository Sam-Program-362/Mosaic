# Code Standards

## General

- Use idiomatic Kotlin and keep modules small, cohesive, and single-purpose.
- Separate UI rendering, UI state, domain decisions, document-provider I/O, and persistence. A composable must not implement folder enumeration or file-copy logic.
- Fix root causes rather than layering retries or UI workarounds around incorrect state.
- Treat the user requirements in `project-overview.md` and the invariants in `architecture.md` as authoritative.
- Distinguish confirmed product requirements from implementation defaults. Update the context files when a default becomes a confirmed decision or changes.
- Do not add a network service, account system, analytics, ads, broad storage permissions, or internet permission to solve a local feature.

## Kotlin and Android

- Use Kotlin's null safety, data classes, sealed interfaces/classes for finite states and failures, and immutable data structures where practical.
- Avoid `!!`; when a value may be absent, model the absent/error state and handle it explicitly.
- Use coroutines for blocking or suspendable work and `Flow`/`StateFlow` for observable repository and UI state. Never perform folder scans, image metadata extraction, or file copying on the main thread.
- Use lifecycle-aware collection in Compose (`collectAsStateWithLifecycle` or the current equivalent supported by the chosen dependency version).
- Keep UI state explicit: `Loading`, `Content`, `Empty`, `Unavailable`, and actionable error states where relevant. Do not represent provider errors as empty folders.
- Use stable IDs for media and community identity. Do not use a file's visible name as its unique identity.
- Do not retain an Activity, Context, player, or open document stream in a long-lived ViewModel or singleton beyond its proper lifecycle.
- Follow Android platform guidance and the existing Gradle/Kotlin conventions. Centralize dependency versions in the version catalog and use compatible stable versions.

## Jetpack Compose and UI Architecture

- Keep composables focused on rendering, events, and accessibility. Move business logic into ViewModels/use cases/repositories as appropriate.
- Prefer stateless reusable composables with explicit parameters and event callbacks; expose UI state as immutable values.
- Use `remember` only for short-lived UI state. Persist durable preferences in DataStore and durable application records in Room.
- Use stable keys in lazy lists and pagers, based on media/community IDs rather than list positions or file names.
- Keep navigation destinations small. Route to domain operations through ViewModels/repositories rather than passing file streams or large media byte arrays between screens.
- Respect lifecycle and visibility when autoplaying GIFs or videos. Do not keep multiple invisible video players actively decoding media.
- Use Material 3 components where appropriate, styled through the semantic tokens in `ui-context.md`. Avoid scattering hardcoded color, spacing, and shape values through feature code.
- Provide accessibility labels for icon-only actions, keep text legible under Android's font scaling, and expose a usable focus order.

## Error Handling

- Handle permission loss, missing documents, provider unavailability, empty folders, malformed metadata, unsupported types, decode errors, insufficient storage, and copy failures as expected conditions.
- Never catch and silently discard an exception that could make the feed or Saved collection incorrect. Log diagnostic details locally only when useful, and show a concise recovery action to the user.
- Do not log full local file paths or content URIs unless necessary for debugging and explicitly appropriate; do not send diagnostics off-device.
- On copy failure, clean up any incomplete destination file/record and preserve the original. Report that the copy failed without claiming it succeeded.
- During refresh, distinguish a successfully enumerated folder with zero supported files from a folder that could not be enumerated.

## Data and Storage

- Keep folder grants, community memberships, media metadata, and Saved-copy metadata in Room or the specific storage described in `architecture.md`.
- Never store whole media file bytes in Room. Keep media in its source folder or app-managed Saved files and persist references/metadata.
- Treat source content URIs as revocable and external. Revalidate access before opening/copying a source document.
- Use collision-safe destination IDs/names for Saved copies. Repeated Save actions must not overwrite or deduplicate prior copies.
- Reconcile deletions only after a successful scan. If scanning fails, keep last-known media entries and show the folder as unavailable or needing attention.
- Do not mutate source content or recursively traverse child directories.

## Media and Playback

- Determine media kind from the document provider's MIME type where reliable, with conservative extension fallback for providers that omit MIME data.
- Support static images, animated GIFs, and MP4 video in the initial release. For unknown or unsupported formats, show a graceful unsupported-media state rather than crashing the feed.
- Autoplay GIFs and videos only while visible, and release/pause playback on visibility or lifecycle changes.
- Start videos muted and provide an explicit sound control. Keep player ownership/lifecycle scoped to the relevant feed item or viewer session.
- Preserve the viewer's feed source, ordering, and current index when opening a post. Vertical swipes advance through that exact collection.

## Naming and File Organization

- Use `UpperCamelCase` for classes, interfaces, composables, and Kotlin type names.
- Use `lowerCamelCase` for functions, properties, and local variables. Use `SCREAMING_SNAKE_CASE` only for true constants where idiomatic.
- Name composables by the UI role they render (for example, `MediaPostCard`, `CommunityDrawer`, `ImmersiveMediaViewer`). Avoid vague names such as `CommonThing` or `DataManager`.
- Use feature-first packages aligned with `architecture.md`: `feature/feed/`, `feature/communities/`, `feature/viewer/`, `feature/saved/`, `feature/folders/`, `feature/settings/`, `data/local/`, `data/source/`, `data/repository/`, and `ui/theme/`.
- Keep Room entities/DAOs separate from UI/domain models if their responsibilities differ. Avoid leaking persistence entities directly into composables.
- Place reusable design primitives in `ui/components/`; do not turn this package into a second location for feature-specific business logic.

## Testing and Verification

- Add unit tests for feed sorting and randomization, direct-child-only folder indexing, media-type detection, community membership filtering, stable identity, simulated-count regeneration triggers, and duplicate Save behavior.
- Test folder-source cases: valid empty folder, newly added file, deleted source file, unavailable/revoked folder access, and restored access.
- Test that Saved copies remain readable after source deletion and that repeating Save produces separate files.
- Add UI tests for the empty state, community drawer, sort switching, post-to-viewer navigation, vertical paging, mute/unmute controls, Save copy feedback, and light/dark theme switching where practical.
- Use test doubles for document providers/repositories when testing domain logic. Do not rely exclusively on manual testing with one real device folder.
- Before closing a task, run the narrow relevant tests, then the available full checks. Recommended baseline commands once the Android project exists: `./gradlew testDebugUnitTest`, `./gradlew lint`, and `./gradlew assembleDebug`.
- Record any checks that could not run and why. Never claim a build or test passed unless its output confirms success.

## Styling

- Use the semantic tokens defined in `ui-context.md`; do not scatter literal color hex values or arbitrary spacing/radius values across feature composables.
- Keep light and dark palettes complete, with accessible contrast for primary text, secondary text, dividers, actions, and media surfaces.
- Use the shared typography scale and shape tokens. Let Android's font scaling work rather than hardcoding pixel sizes.
- Preserve the Reddit-like feed hierarchy and density described in `ui-context.md`; do not independently redesign navigation or post cards while implementing unrelated features.

## Platform Permissions and Dependencies

- Folder access must flow through Android's Storage Access Framework and explicit user action.
- Do not request `MANAGE_EXTERNAL_STORAGE`, scan all device media through broad permissions, or add an `INTERNET` permission for local features.
- Add only dependencies required by the approved architecture. Explain new dependencies in the change and keep versions centralized.
- Do not edit generated build output or third-party library internals.
