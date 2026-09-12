# Implementation Plan - Layout Refinements

This plan addresses layout issues in the Sources and Settings screens, specifically focusing on vertical header alignment and redundant side padding.

## Proposed Changes

### Home Screen

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/home/HomeScreen.kt)
- Fix side padding: Change `padding(24.dp)` to `padding(horizontal = 16.dp)` to match other screens.
- Fix vertical alignment: Ensure the top padding isn't excessive.

### Sources Screen

#### [MODIFY] [SourcesScreen.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/sources/SourcesScreen.kt)
- Fix vertical alignment: Remove `statusBarsPadding()` from the `Column`. Rely on `padding(innerPadding)` from the internal `Scaffold`, which should correctly handle the top inset in an edge-to-edge environment.
- Fix side padding: Remove `horizontal = 16.dp` from the `SourceItem` card modifier to avoid double-padding (as the parent `Column` already has 16dp horizontal padding).

### Settings Screen

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/settings/SettingsScreen.kt)
- Fix vertical alignment: Remove `statusBarsPadding()` from the `Column`.
- Adjust padding for consistency: Use `horizontal = 16.dp` instead of `20.dp` to match other screens.

### History & Liked Screens

#### [MODIFY] [HistoryScreen.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/history/HistoryScreen.kt)
- Verify if `statusBarsPadding()` is redundant when combined with the parent `Scaffold`'s `innerPadding`.
- *Correction*: Since these screens don't have their own `Scaffold`, `statusBarsPadding()` is correct here as the parent `MainActivity` doesn't apply top padding.

## Verification Plan

### Manual Verification
- **Header Position**: Check if "Video Sources" and "Settings" headers are correctly aligned with the status bar (not too low).
- **Card Width**: Verify that the cards in the Video Sources list now align perfectly with the header, without extra side padding.
- **Consistency**: Ensure all screens (Home, Sources, History, Liked, Settings) have consistent horizontal margins for their content.
