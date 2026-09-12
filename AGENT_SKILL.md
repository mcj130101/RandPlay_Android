---
name: randplayer-development-guide
description: Expert guidelines for maintaining and extending the RandPlayer Android application. Covers architecture, playback logic, theming, and coding conventions.
---

# RandPlayer Agent Skill

This document defines the architectural patterns, best practices, and technical details for the RandPlayer project. Use this as a reference when implementing new features or refactoring existing ones.

## 🏗️ Architecture

RandPlayer follows the **MVVM (Model-View-ViewModel)** pattern combined with a Repository-based data layer.

### Layers
- **UI**: Jetpack Compose using Material 3. Components are kept stateless where possible.
- **ViewModel**: Manages UI state using `StateFlow`. Uses `viewModelScope` for side effects.
- **Repository**: Single source of truth. Orchestrates data from local DB (Room), preferences (DataStore), and external sources (SMB/SAF).
- **Domain**: Pure logic for video selection (`RandomVideoSelector`) and playback orchestration.

## 💉 Dependency Injection
- **Framework**: Hilt.
- **Scopes**: Most core managers (`PlaybackManager`, `SyncCoordinator`) and repositories are `@Singleton`.
- **Qualifiers**: Use `@IoDispatcher`, `@DefaultDispatcher`, and `@MainDispatcher` defined in `AppModule.kt` for thread management.

## 🎬 Playback System
- **Central Authority**: `PlaybackManager` is the **only** class that should initiate playback.
- **Tracking**: `PlaybackManager.playVideo(videoId, recordHistory = true)` automatically:
    1. Increments `playCount` in the `videos` table.
    2. Updates `lastPlayedAt`.
    3. Adds a new entry to `playback_history`.
- **Skip Tracking**: When replaying from the Home screen's "Continue Watching" card, `recordHistory` is set to `false` to avoid inflating play counts.
- **Providers**: 
    - `LocalPlaybackProvider`: For local device storage.
    - `SmbPlaybackProvider`: For network shares (uses a local proxy server `NanoHTTPD` to stream to external players).
- **External Player**: Launches videos via `Intent.ACTION_VIEW`.
- **Selection Modes**: Supports `SYSTEM_DEFAULT`, `ASK_EVERY_TIME` (chooser), and `SPECIFIC_APP` (package-locked).

## 📊 Data Layer
- **Room Database**: 
    - `VideoEntity`: Core video metadata.
    - `VideoSourceEntity`: Definitions for Local/SMB sources.
    - `PlaybackHistoryEntity`: Flat log of every playback event.
- **DataStore**: Used for `AppSettings` (Theme, Random Mode, Player Package).
- **Sync**: `SyncCoordinator` handles scanning. Uses `Scanner` implementations for different `SourceType`s.

## 🎨 UI & Theming
- **Material 3**: All components should use `MaterialTheme.colorScheme` and `MaterialTheme.typography`.
- **Theme**: `RandPlayerTheme` handles dynamic background blurs and scrim overlays.
- **Headers**: Page headers should use `headlineMedium` with `FontWeight.Bold` and be placed inside the screen's main `Column` (not a `TopAppBar`) for consistency.
- **Margins**: Standard horizontal padding is **16.dp**.
- **Navigation**: Uses a `HorizontalPager` (DashboardPager) in `MainActivity`. Screens are pre-loaded (`beyondViewportPageCount = 4`) for zero-stutter jumps.

## ✅ Best Practices
1. **Surgical Edits**: When modifying files, prefer `replace_file_content` or `multi_replace_file_content` to preserve surrounding context.
2. **UI State**: Expose state via `StateFlow` and consume in Compose using `collectAsStateWithLifecycle()`.
3. **Strings**: Move hardcoded strings to `strings.xml` for localization (though current dev may use literals for speed).
4. **Error Handling**: Use `Result` wrappers or specific `ScanEvent.Error` flows for long-running tasks like syncing.
5. **Edge-to-Edge**: Always use `statusBarsPadding()` or `innerPadding` from `Scaffold` to avoid overlap with system bars.
6. **Compose Performance**: Avoid heavy logic inside `@Composable` functions; move it to `derivedStateOf` or the `ViewModel`.

## 🚫 What NOT to do
- **Don't** update `playCount` manually in ViewModels; call `playbackManager.playVideo()`.
- **Don't** use `animateScrollToPage` for bottom nav navigation (causes stutter); use `scrollToPage`.
- **Don't** hardcode colors; use the semantic palette in `Color.kt`.
- **Don't** block the Main thread; always use the appropriate `Dispatcher`.
