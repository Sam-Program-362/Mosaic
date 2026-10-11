# UI Context

## Reference and Design Intent

Use Reddit's Android app as the primary reference for the visual hierarchy and interaction model: feed header and sort control, community drawer, community identity, compact media post cards, media-first browsing, Saved access, Settings access, and light/dark appearance. Match these patterns closely rather than inventing a separate navigation metaphor. Use the project's own name and app icon; do not depend on Reddit's logo or proprietary asset files.

This app is a local-media adaptation. Preserve these product-specific rules even where they differ from Reddit: no comments or comment button; up/down counts are decorative and non-interactive; posts come only from selected local folders; media is browsed by vertical swipes in the immersive viewer; Saved creates local copies.

Reference sources checked on 2026-10-09:

- [Official Reddit App FAQ for Android](https://support.reddithelp.com/hc/en-us/articles/17099257885844-Official-Reddit-App-FAQ-for-Android) — dark mode, Saved access, autoplay, and mute behavior.
- [How do I access and manage my communities on the Reddit app?](https://support.reddithelp.com/hc/en-us/articles/5590560923156-How-do-I-access-and-manage-my-communities-on-the-Reddit-app) — community drawer and community grouping.
- [A Faster, Conversation-First Approach to Feeds in Reddit's Mobile Apps](https://redditinc.com/news/a-faster-conversation-first-approach-to-feeds-in-reddits-mobile-apps) — unified media player and immersive media navigation patterns.

Reddit may run interface experiments, so treat the standard community-drawer and profile/menu patterns as the baseline rather than copying a temporary experiment. The user's explicit local-media interaction rules override Reddit-specific gestures that conflict with them.

## Navigation and Information Architecture

- **Home feed:** the main landing screen. It combines media from all selected folders and has a clearly visible sort control with Random and Newest options.
- **Community drawer:** a hamburger/menu control at the top left opens a Reddit-style drawer. Support opening the drawer with the standard Android navigation gesture where feasible. Show Home, folder communities, and custom communities in clear groups; allow a user to create a custom community and manage selected folders from an obvious action.
- **Community screen:** uses the same feed/post-card components as Home, filtered to the selected folder or the folder set associated with a custom community.
- **App/profile-style menu:** because this app has no account profile, provide a simple app menu in the position/pattern familiar from Reddit. It exposes Saved, Manage folders, and Settings; do not invent profile/account features.
- **Saved:** a dedicated collection of app-managed copies. Each save action is an independent entry, including repeated saves of the same source.
- **Settings:** theme and applicable local playback/feed preferences. Avoid account, notification, or social settings that have no meaning in an offline app.
- **Immersive viewer:** opened by tapping a post's media. Vertical swipe navigates through the same feed and sort order. Keep the source context recoverable through a back gesture/button.

Use the drawer as the primary community-navigation pattern; do not replace it with a novel navigation system or ask the user to choose between common navigation variants.

## Screen and Component Rules

### Feed Header

- Show the screen/community title and a recognizable menu affordance.
- Place Random/Newest sorting close to the feed content using a compact selector or segmented/dropdown control consistent with Reddit's mobile feed controls.
- Make the active sort mode obvious and persistent while scrolling where the reference pattern supports it.
- Provide an explicit refresh gesture/action; a successful refresh reconciles the library and regenerates fake vote counts.

### Media Post Card

Render posts in a compact Reddit-style card hierarchy:

1. **Community row:** community avatar, community name, and local-file metadata such as relative modification time when available.
2. **Title:** use the file name as the post title. Default display omits a recognized extension for readability; preserve the full name in metadata.
3. **Media:** show the image/GIF/video inline, within the card's full available width. Preserve the source aspect ratio where possible; avoid destructive cropping. Use a stable placeholder/loading state while local media is being prepared.
4. **Action row:** decorative upvote/downvote arrows and randomized counts, plus a clearly labeled or recognizable Save copy action. Upvote/downvote taps do nothing and must not imply real voting. There is no comment icon, comment count, or comment button.
5. **Open behavior:** tapping the media opens the immersive viewer. Avoid making the entire card an ambiguous action target if it prevents clear access to Save copy.

Keep card spacing, dividers, typography, metadata hierarchy, and touch targets close to Reddit's standard Android feel. Do not turn each post into a rounded promotional tile or a dashboard widget.

### Community Identity

- Automatically created folder communities use the folder name.
- Custom communities use the name the user enters and list the folders assigned to them in the management UI.
- Community artwork uses the first supported static image found directly inside the folder, ordered by case-insensitive filename. Use it as both avatar and header artwork to keep identity consistent.
- If no static image is available, use a neutral generated placeholder with initials/name. Artwork selection must not require a network call.
- Keep unavailable communities in the drawer and show a clear unavailable state on their screen. Do not silently erase them.

### Immersive Media Viewer

- Use an edge-to-edge media-first layout with minimal chrome and legible overlays.
- Vertical swipe moves to previous/next media within the feed snapshot that opened the viewer. Support photos, GIFs, and MP4 videos in the same sequence.
- Photos should fit the viewport without distortion; a zoom gesture is a suitable platform-standard enhancement if implemented accessibly.
- GIFs and videos autoplay only while visible. Pause/offload media when it leaves view or the app becomes inactive.
- Videos start muted and include a clear mute/unmute control. Any playback controls must not intercept vertical paging unnecessarily.
- The viewer uses the current feed order; it must not silently switch to a global collection or a newly shuffled list mid-session.

### Saved View

- Show the saved file's name and media preview using the same visual language as the feed.
- Repeated saves are separate copies and separate list entries, not a saved/unsaved toggle.
- Keep Saved items accessible when source folders become unavailable or source files are deleted.
- Do not imply that tapping Save deletes or moves the original.

### Empty, Loading, Error, and Unavailable States

- **No folders selected:** explain that a folder must be selected and provide a direct Add folder action.
- **Selected folder has no supported media:** distinguish this from a folder access error; offer refresh and folder management.
- **Folder unavailable:** retain the community and explain that access is unavailable; provide an action to reselect/restore access where Android allows.
- **Media unavailable/unsupported:** preserve navigation and the rest of the feed; show a contained placeholder instead of crashing.
- **Save failure:** state that the copy failed and the original was not changed.
- **Loading:** avoid blank screens for slow document providers; use placeholders that do not shift the whole feed unnecessarily.

## Visual Language

The app should look and behave like a familiar Reddit-style mobile feed, not an unrelated gallery or social-media redesign. Keep the palette and compact information hierarchy close to Reddit's recognizable light/dark presentation while using no Reddit logo files or official brand assets. Since the exact production palette/typography was not provided as a separate design source, treat the token values below as **initial implementation tokens to tune against the installed Reddit Android app**, not pixel-verified brand specifications.

### Semantic Color Tokens (initial values)

| Token | Light | Dark | Use |
| --- | --- | --- | --- |
| `colorBackground` | `#DAE0E6` | `#0E1113` | App/feed background behind content |
| `colorSurface` | `#FFFFFF` | `#181C1F` | Post cards, menus, dialogs, sheets |
| `colorSurfaceRaised` | `#FFFFFF` | `#24292D` | Elevated surfaces and viewer controls |
| `colorTextPrimary` | `#1A1A1B` | `#F2F4F5` | Titles and primary content |
| `colorTextSecondary` | `#576F76` | `#A6ADB2` | Metadata, timestamps, supporting labels |
| `colorDivider` | `#E5E7E8` | `#343A3F` | Card separators and subtle borders |
| `colorAccent` | `#FF4500` | `#FF4500` | Primary actions and active states |
| `colorLink` | `#0079D3` | `#4FBCFF` | Text links where links are relevant |
| `colorError` | `#C62828` | `#FF6B6B` | Errors and unavailable-state emphasis |
| `colorMediaBackdrop` | `#F3F5F6` | `#050606` | Media loading and immersive viewer backdrop |

- Define tokens centrally in the theme layer; use semantic names in components.
- Keep text/background and control/focus contrast accessible in both themes.
- Use accent color for important actions, not large decorative blocks.
- Fake vote colors/graphics must remain decorative; they must not suggest active vote state.

## Typography

- Use the Android system sans-serif / Material 3 type scale initially; do not add a custom font without a need.
- Screen/community title: approximately 20–24sp, semibold.
- Post title: approximately 16–18sp, medium/semibold.
- Body and controls: approximately 14–16sp.
- Metadata and compact action labels: approximately 12–14sp while preserving readability.
- Respect Android font scaling and avoid truncating filenames in a way that hides the only identifying content; allow wrapping or use a tooltip/secondary full-name surface when needed.

## Spacing, Shapes, and Layout

- Use an 4dp base spacing grid; common spacing values are 4, 8, 12, 16, 20, 24, and 32dp.
- Use compact, familiar mobile feed spacing. Start with 0–4dp outer card rounding, 8dp for controls/sheets, and 12–16dp only for larger overlays; tune to match the Reddit Android reference.
- Feed cards should span the available phone width with separators/backgrounds rather than excessive gutters.
- Keep touch targets consistent with Android accessibility guidance even when icon art is visually small.
- Render inline media at the card's available width and preserve aspect ratio. Avoid layout jumps after image metadata is loaded.
- The target is Android phones. Support common portrait use and reasonable landscape/large-font behavior, but do not invent a tablet-specific multi-pane design for v1.

## Icons, Motion, and Feedback

- Use a consistent Android/Material icon family with recognizable menu, bookmark/save, sort, folder, settings, mute/unmute, play/pause, up/down, and refresh affordances.
- Icon-only controls need meaningful accessibility descriptions. Decorative vote controls should be communicated as decorative/non-interactive to accessibility services, not exposed as actionable buttons.
- Keep transitions quick and spatially consistent with the Android app reference. Use an immersive transition into media viewing; avoid elaborate custom animations that delay browsing.
- Provide visible feedback for Save copy success/failure and folder selection/access changes.
- Autoplay only while media is visible. Do not replay or restart all visible media because an unrelated state field changed.

## Dialogs, Sheets, and Forms

- Use native-feeling Material 3 dialogs/sheets for creating a custom community, confirming destructive operations if those are later added, and communicating recoverable errors.
- Folder selection must launch Android's native system picker, not a custom full-storage browser.
- Custom-community creation needs a name field and a selectable list of already selected folders. Prevent empty community names and make the folder membership clear before saving.
- Theme controls should be direct and understandable: Light and Dark. Do not add an Auto option unless requested or deliberately included as a documented future enhancement.

## Responsive and Accessibility Rules

- Prioritize Android phone layout and portrait media browsing.
- Honor system font scaling and display size, content descriptions, focus order, keyboard/remote focus where relevant, and sufficient touch target sizes.
- Do not encode state by color alone: identify unavailable folders, active sort, and playback state with text/icons as well as color.
- Keep the media viewer usable with TalkBack and playback controls reachable without requiring precise gestures alone.
- Verify both themes for feed cards, empty states, drawer, viewer overlays, media placeholders, and dialogs.
