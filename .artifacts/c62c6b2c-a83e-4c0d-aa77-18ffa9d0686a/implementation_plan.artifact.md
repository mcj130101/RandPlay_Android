# Implementation Plan - UI & Theming Fixes

This plan addresses the UI inconsistencies and visibility issues in dark mode reported by the user.

## User Review Required

> [!IMPORTANT]
> I will be updating the `DarkColorScheme` and `LightColorScheme` to include more Material 3 color roles (like `secondaryContainer`) to ensure better consistency across components like `IconButton`.

## Proposed Changes

### Theming

#### [MODIFY] [Theme.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/theme/Theme.kt)
- Expand `DarkColorScheme` and `LightColorScheme` to include:
    - `secondaryContainer`, `onSecondaryContainer`
    - `tertiary`, `onTertiary`
    - `errorContainer`, `onErrorContainer`
- Ensure `LocalContentColor` is correctly established in the root `Box`.

### History Screen

#### [MODIFY] [HistoryScreen.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/history/HistoryScreen.kt)
- Set explicit `color = MaterialTheme.colorScheme.onBackground` for the "Recent Playback" header.
- Set explicit `color = MaterialTheme.colorScheme.onSurface` for the video titles in the list.
- Update icon button colors to use primary/secondary roles instead of default tonal colors if they don't contrast well.

### Liked Screen

#### [MODIFY] [LikedScreen.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/liked/LikedScreen.kt)
- Set explicit `color = MaterialTheme.colorScheme.onBackground` for the "Liked Videos" header.
- Set explicit `color = MaterialTheme.colorScheme.onSurface` for the video titles in the list.
- Update the "Search" field and "Sort" button colors for better visibility.

### Sources Screen

#### [MODIFY] [SourcesScreen.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/sources/SourcesScreen.kt)
- Update `TopAppBar` colors to ensure title visibility.
- Refine `IconButton` colors (Info, Refresh, Delete) to match the theme better.
- Ensure source names and locations have proper contrast.
- **[NEW]** Add a badge or label to `SourceItem` indicating if the source is "Local" or "SMB".

## Verification Plan

### Manual Verification
- Deploy to an Android device/emulator.
- Switch between Light and Dark modes.
- Verify that headers ("Recent Playback", "Liked Videos", "Video Sources") are clearly visible (not black in dark mode).
- Verify that video titles in all lists are readable.
- Verify that buttons (Delete, Refresh, Info) have consistent styling and appropriate contrast.
