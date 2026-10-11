# Local Media Feed — Project Overview

> **Working description, not a finalized app name.** The product is a private, offline Android media browser with Reddit-style feeds and communities. It is not a networked social network.

## Overview

Build an Android phone application that turns media in folders explicitly selected by the user into a Reddit-like browsing experience. Supported post content for the first release is static photos, animated GIFs, and MP4 videos. Each post uses the local file name as its title, shows the media inline, and opens in an immersive viewer. The user can browse all selected media in a randomized or newest-first feed, browse per-folder or custom communities, and save independent copies inside the app. All library content and settings remain local; the app does not need an account, server, or internet connection at runtime.

The reference for layout and interaction is Reddit's Android app, including its community drawer, feed sorting, post-card hierarchy, saved-content access, light/dark themes, and immersive media handling. Adapt only the parts that conflict with the local-media product requirements: there are no real users, comments, network posts, or interactive votes.

## Goals

1. Let a user select one or more folders through Android's native folder picker and browse supported media found directly inside them, without requesting broad access to all device storage.
2. Make the core feed, community browsing, immersive viewing, and saving workflows usable without network access or an account.
3. Reflect additions and deletions in selected folders during app use while preserving community records when a folder is temporarily unavailable.
4. Provide a Reddit-like Android browsing experience with randomized and newest-first sorting, autoplay for visible GIFs/videos, and switchable dark/light themes.
5. Keep source media safe: browsing and saving must never modify or delete originals; each Save action creates a separate app-managed copy.

## Core User Flow

1. On first launch, show an empty state that explains the app needs a folder selection to populate the feed.
2. The user selects a folder using Android's native system folder picker and grants access. The app records the permitted folder URI and creates a community named after that folder.
3. The app scans direct child entries only and indexes supported photos, animated GIFs, and MP4 videos. It excludes subfolder contents unless a subfolder is separately selected.
4. The user browses the Home feed, chooses Random or Newest sorting, or opens a folder community from the Reddit-style community drawer.
5. The user taps a post's media to open the immersive viewer and swipes vertically to move through the same feed's media in its current order.
6. The user invokes Save copy to copy the original file into the app-managed Saved collection. The original remains untouched; repeat saves create separate copies.
7. The user can add more folders, create a custom community from multiple selected folders, review Saved copies, and change the theme in Settings.
8. On app launch/resume and refresh, the library is rescanned. New files appear, deleted source files disappear from source feeds, and temporarily unavailable folders remain listed with an unavailable state.

## Features

### Local Library and Folder Access

- Select folders through Android's native system folder picker and retain access only where Android permits a persisted grant.
- Index supported media from direct children of selected folders; do not crawl nested folders.
- Support static photos, animated GIFs, and MP4 videos for the initial release.
- Use each file's local name as the post title. Recommended display default: omit a recognized file extension from the visible title while retaining the full file name in media metadata.
- Refresh the local catalog on app start, app foreground/resume, and explicit feed refresh. Do not depend on a background network service or continuous background process.

### Feed and Sorting

- Home feed combines supported media from all selected folders.
- Support Random and Newest-first sorting.
- Working default for Random: shuffle the current eligible list on each app start or feed refresh; do not repeat an item within a single pass unless the list is refreshed or exhausted.
- Each post is rendered as a Reddit-style card with community identity, title, inline media, and a compact action area.
- Show simulated random upvote/downvote counts. Regenerate these display-only values on app start or feed refresh. Voting arrows and counts do nothing when tapped.
- Do not provide a comments system or comment button.

### Communities

- Automatically create one community per selected folder, named after the folder.
- Allow custom communities that combine multiple selected folders. A custom community contains media from its associated folders only.
- Use a deterministic first-image rule for community artwork: choose the first supported static image after sorting direct-child file names case-insensitively. Use it for the community avatar and header artwork. If no static image exists, use a neutral placeholder with the community name/initials.
- Present communities in a Reddit-style community drawer. Keep communities visible when their source folder is unavailable.

### Media Viewer and Playback

- Tapping a post's media opens an immersive, edge-to-edge viewer.
- Vertical swipes move to the previous/next media item in the same feed and current sort order, across photos, GIFs, and MP4 videos.
- GIFs and videos autoplay when visible. Videos are muted by default and expose an in-view sound toggle.
- Stop or pause media that is no longer visible and release playback resources when the viewer/feed is no longer active.

### Saved Media

- Save copy writes a new, independent copy into app-managed persistent storage.
- Saving the same source repeatedly creates multiple independent copies; do not deduplicate, overwrite, or treat Save as a toggle.
- Source deletion or loss of source-folder access does not delete Saved copies.
- Provide a dedicated Saved view within the app, using the same media browsing patterns where practical.

### Settings and Appearance

- Provide light and dark themes selectable in Settings.
- Provide folder management and feed/media settings through Reddit-familiar menu/navigation patterns.
- The app is designed to operate entirely offline. Do not add accounts, comments, real voting, social interactions, cloud sync, ads, analytics, or server dependencies.

## Scope

### In Scope

- Android phone application and Reddit-like navigation/feed presentation.
- Native system folder selection and persisted per-folder access where Android supports it.
- Direct-child-only media discovery for selected folders.
- Per-folder communities and user-created multi-folder communities.
- Home feed, Random and Newest-first sorting, media post cards, fake vote counters, and immersive vertical-swiping viewer.
- GIF/MP4 autoplay when visible; MP4 muted by default with a sound toggle.
- App-managed Saved copies that survive removal of source files.
- Folder refresh, deleted-file reconciliation, unavailable-folder state, and access-restoration recovery.
- Local settings and theme switching.

### Out of Scope

- Accounts, authentication, profiles for real users, remote posts, online content discovery, or server APIs.
- Real voting, comments, chat, moderation, followers, notifications, or any other social-network backend.
- Internet-based media streaming, cloud storage, cloud sync, or sharing data with a service.
- Scanning all device media without the user selecting each relevant folder.
- Recursive subfolder indexing by default.
- Modifying, moving, renaming, or deleting source media.
- Formats beyond static photos, animated GIFs, and MP4 videos in the initial release unless separately approved.

## Success Criteria

1. A user can select a folder through Android's native picker and see supported direct-child media as posts without granting broad all-storage access.
2. A nested folder's media is excluded until that nested folder is separately selected.
3. Every selected folder has a corresponding community; a custom community shows only files from its associated folders.
4. Random and Newest-first sorts work, and the immersive viewer moves vertically through the same ordered feed across all supported media types.
5. A post title comes from the file name; media cards and the viewer render supported photos, animated GIFs, and MP4 videos correctly.
6. Visible GIFs/videos autoplay, MP4 audio starts muted, and the user can enable sound.
7. Voting UI displays changing simulated counts but performs no data mutation; no comment action is present.
8. Save copy creates a separate persistent file for every invocation, leaves the original unchanged, and saved files remain available after source deletion.
9. New source files and source deletions are reconciled on app foreground/resume or refresh; inaccessible folders remain listed and recover when access returns.
10. The user can switch between light and dark themes and continue using the core library/viewer workflows with no network connection.

## Implementation Defaults and Unresolved Decisions

- Recommended implementation baseline: Kotlin, Jetpack Compose, AndroidX libraries, Room for local metadata, DataStore for preferences, Android's Storage Access Framework for folder grants, and Media3 for MP4 playback. This is a recommendation, not a user-confirmed stack decision.
- The app's final public name, package/application ID, minimum Android version, and release target SDK are not set.
- Supported still-image formats should follow Android-recognized image MIME types available from the selected document provider; GIF and MP4 remain explicit requirements.
- If a selected folder contains duplicate file names across different paths, use a collision-safe internal identity based on the content URI/path rather than the visible title.
