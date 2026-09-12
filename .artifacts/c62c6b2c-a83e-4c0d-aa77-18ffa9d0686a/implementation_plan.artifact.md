# Implementation Plan - Fix Player Selection Issues

The user reported that player selection is not working as expected: MX Player always starts, and the specific app list in Settings is empty. This is primarily caused by Android's package visibility restrictions (introduced in API 30) and missing manifest declarations.

## Proposed Changes

### Android Manifest

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/AndroidManifest.xml)
- Add `<queries>` section to allow the app to see other installed video players. This is required for `queryIntentActivities` to work on Android 11+.

### Settings Screen

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/settings/SettingsScreen.kt)
- Update `PlayerSelector` to use a more robust `Intent` for querying (e.g., using `content://` or just the MIME type).
- Ensure `PackageManager.queryIntentActivities` is called with appropriate flags.
- Add a check to show a placeholder if no players are found (though the `<queries>` fix should resolve this).

### Main Activity (Playback Launch)

#### [MODIFY] [MainActivity.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/MainActivity.kt)
- Ensure the `Intent` used for `createChooser` is fresh and hasn't had any package set previously.
- Explicitly reset the package to `null` before starting in "Ask Every Time" or "System Default" modes to prevent any accidental "stickiness".

## Verification Plan

### Manual Verification
- **Manifest**: After adding `<queries>`, go to Settings -> Playback -> Choose Specific App. Verify that a list of installed video players (like VLC, MX Player, etc.) now appears in the dropdown.
- **Ask Every Time**: Select "Ask Every Time" in Settings. Play a video. Verify that the Android system chooser appears, even if a default was previously set.
- **Specific App**: Select a specific app (e.g., VLC). Play a video. Verify it launches VLC directly.
- **System Default**: Select "System Default". Verify it uses the system's standard resolution.
