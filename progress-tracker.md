# Progress Tracker

Update this file after every meaningful implementation or planning change. Separate documented plans from code that has actually been built and verified.

## Current Phase

- **Planning/context specification prepared. Application implementation has not started.** No Android project, application code, build, or test run is claimed by this document package.

## Current Goal

- Provide a consistent six-file context package for a Reddit-style, offline local-media Android application so future implementation can proceed incrementally against an explicit product and architecture specification.

## Completed

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

## In Progress

- No application implementation is in progress. The context package is the current deliverable.

## Next Up

1. Confirm the recommended Kotlin + Jetpack Compose Android baseline before scaffolding, or revise `architecture.md` if a different stack is selected.
2. Choose the public app name, package/application ID, minimum Android version, and target SDK.
3. Scaffold the native Android project and verify that the first debug build succeeds.
4. Implement the storage/access foundation first: select a folder, persist/check the grant, enumerate direct children, and represent unavailable folders.
5. Add the Room metadata model and successful-scan reconciliation; test source additions/deletions and recovery after access is restored.
6. Implement folder communities and custom community membership, then feed sorting and post cards.
7. Implement image/GIF/MP4 playback and the immersive vertical viewer with lifecycle-safe autoplay and mute controls.
8. Implement independent Saved copies, light/dark themes, folder management, and Settings.
9. Run the relevant unit/UI tests, lint, and debug build; record only real outcomes here.

## Open Questions

The first implementation slice can proceed using the documented defaults below. Confirm or revise these before a production release:

- Final app name and Android application/package ID.
- Whether to accept the recommended Kotlin + Jetpack Compose baseline.
- Minimum supported Android version and target SDK.
- Whether the initial Random behavior (shuffle on app start/refresh, no repeat until the list is exhausted) and extension-free display titles are acceptable defaults.

Non-blocking implementation defaults already documented: deterministic community artwork selection; neutral fallback when no static image exists; filename-based ordering for artwork; Room for metadata; DataStore for preferences; source-safe copy behavior; and a community drawer matching Reddit's Android navigation pattern.

## Architecture Decisions

- **Offline/local-only runtime:** no server, account, cloud synchronization, analytics, or ads.
- **Explicit selected-folder scope:** native Android folder picker and persisted URI permission where supported; no broad storage permission.
- **Direct children only:** nested folders are not traversed unless separately selected.
- **One community per selected folder:** automatically created using the folder name; custom communities combine chosen folders.
- **Source files are read-only:** source files are never modified, moved, renamed, overwritten, or deleted.
- **Saved copies are independent:** each save creates a unique app-managed copy; duplicates are intentional and survive source deletion.
- **Unavailable folders remain represented:** preserve the community and try again after access becomes available.
- **Fake vote counters:** regenerate on app start/feed refresh and never mutate real data.
- **Recommended stack, not yet user-confirmed:** Kotlin, Jetpack Compose + Material 3, Room, DataStore, Android Storage Access Framework, compatible image/GIF loading, and AndroidX Media3.
- **Reddit reference behavior:** use Reddit-style community drawer, feed sorting and post-card composition, Saved/menu access, and light/dark presentation. Product-specific vertical swipe navigation and removal of comments remain authoritative.

## Session Notes

- This is a planning and context-documentation session, not an implementation session.
- The uploaded template set supplied five files (`project-overview.md`, `architecture.md`, `code-standards.md`, `ai-workflow-rules.md`, and `progress-tracker.md`). No `ui-context.md` template was available in the supplied files, so a matching UI context file was authored to complete the required six-file set.
- Template placeholders that assumed web/TypeScript (API routes, TypeScript, npm scripts) were replaced with Android/Kotlin-appropriate standards rather than copied literally.
- Reddit UI documentation reviewed for this package: [Android app FAQ](https://support.reddithelp.com/hc/en-us/articles/17099257885844-Official-Reddit-App-FAQ-for-Android), [community drawer help](https://support.reddithelp.com/hc/en-us/articles/5590560923156-How-do-I-access-and-manage-my-communities-on-the-Reddit-app), and [mobile feed/media-player design article](https://redditinc.com/news/a-faster-conversation-first-approach-to-feeds-in-reddits-mobile-apps).
