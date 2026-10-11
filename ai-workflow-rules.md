# AI Workflow Rules

## Approach

Build this project incrementally using a spec-driven workflow. The six context files define the product, architecture, conventions, UI behavior, and current state. Read the relevant sections before making a change and implement the specified behavior rather than inventing a different product.

The user is the final decision-maker. Respect explicit requirements, including the Reddit-like Android interface, local-only storage scope, offline operation, and the rule that this project is a planning/context package until implementation is explicitly requested. Do not silently turn a recommended implementation default into a user-confirmed decision.

## Scoping Rules

- Work on one end-to-end feature unit at a time.
- Prefer small, verifiable increments over broad speculative changes.
- Do not combine unrelated UI, persistence, media playback, and folder-access changes in one implementation step unless the unit cannot work without that combination.
- Make the smallest change that satisfies the stated requirement; avoid refactors unrelated to the current task.
- Preserve the user-facing Reddit-like navigation and post-card structure when implementing feature logic.
- Do not add networking, accounts, comments, real voting, cloud sync, analytics, advertisements, or broad device-storage access.

## When to Split Work

Split an implementation step if it combines:

- Native folder selection and media indexing with unrelated visual redesign.
- Room schema changes, source-file lifecycle handling, and immersive media playback beyond what the feature requires.
- Multiple feature areas such as communities, Saved, and Settings without a shared end-to-end goal.
- Changes that cannot be verified without real document-provider access, playback behavior, or a UI test; build and test small seams independently first.
- A confirmed requirement with a genuinely unresolved behavior whose outcome changes persisted data or source-file safety.

If a change cannot be verified end to end within its intended scope, split it further.

## Handling Missing Requirements

- Do not invent product behavior that contradicts these files or explicit user statements.
- Use defaults already documented as implementation defaults (for example, Kotlin/Compose baseline and a deterministic artwork fallback) unless the user changes them.
- If a missing decision is implementation-blocking and no documented default exists, add it to `progress-tracker.md` as an open question and stop only the affected decision-dependent work. Continue with independent work.
- Distinguish confirmed requirements, externally researched platform behavior, and recommendations in documentation and summaries.
- When a newer explicit user decision conflicts with an earlier one, apply the newer decision and update every affected file.
- Do not repeatedly ask the user to choose details that can be researched from the stated reference product or reasonably inferred from an explicit “like Reddit” instruction. Ask one focused question only when a material product decision cannot be inferred and genuinely blocks progress.

## Protected Files and Safety Boundaries

- Do not modify generated build output or files owned by third-party libraries.
- Do not add `INTERNET`, `MANAGE_EXTERNAL_STORAGE`, or broad media/storage permissions without explicit user approval and a documented reason.
- Do not introduce cloud endpoints, telemetry, ads, or remote data transfer.
- Treat source media as read-only. Never delete, rename, move, overwrite, or modify selected-folder media.
- Saving must create a new app-managed copy every time. Never deduplicate or overwrite prior Saved copies.
- Never remove a community merely because its folder is temporarily unavailable or a provider scan failed.
- Do not migrate or delete persisted user data without a clear migration path and explicit task scope.

## Keeping Docs in Sync

Update the relevant context files whenever implementation changes:

- Product features, in-scope/out-of-scope boundaries, or success criteria: `project-overview.md`.
- Stack, package boundaries, storage model, permissions, lifecycle, or invariants: `architecture.md`.
- Kotlin/Compose conventions, testing, error handling, or package rules: `code-standards.md`.
- Agent process, verification gates, or prohibited changes: `ai-workflow-rules.md`.
- Navigation, component anatomy, themes, tokens, and interaction behavior: `ui-context.md`.
- Phase, completed units, in-progress work, next steps, open decisions, and actual verification results: `progress-tracker.md`.

When a confirmed decision changes, search all six files for the old rule, update every affected section, and perform a contradiction check.

## Verification Before Completion

1. Confirm the implementation meets the feature's stated acceptance criteria.
2. Confirm no invariant in `architecture.md` is violated, especially offline operation, selected-folder-only access, non-recursive scanning, and original-file safety.
3. Add/update tests for changed behavior, including failure and unavailable-folder cases when applicable.
4. Run relevant tests, then the available full checks: `./gradlew testDebugUnitTest`, `./gradlew lint`, and `./gradlew assembleDebug` when those Gradle tasks exist.
5. Review accessibility, light/dark theme behavior, loading/empty/error/unavailable states, and playback lifecycle for UI changes.
6. Update `progress-tracker.md` with what was actually completed and the real check results. Do not claim success for any unrun check.
7. If a full check cannot run, state the blocker explicitly and do not represent the work as fully verified.

## Before Moving to the Next Unit

- The current unit works end to end within its defined scope or has a clearly documented blocker.
- Relevant tests/checks have been run and the outcomes are known.
- No architecture invariant was violated.
- Context files reflect any durable decisions or changes.
- `progress-tracker.md` records actual progress, not plans described as completed implementation.
