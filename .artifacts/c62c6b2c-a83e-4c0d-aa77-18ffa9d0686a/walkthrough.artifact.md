# Walkthrough - Player Selection Fix

I have fixed the issues where other installed video players were not appearing in the settings and the selection was not being respected during playback.

## Changes Made

### 1. Package Visibility Fix
- **[AndroidManifest.xml](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/AndroidManifest.xml)**: Added the `<queries>` tag. This is a critical requirement for Android 11+ (API 30+) that allows our app to see other installed video players. Without this, `queryIntentActivities` returns an empty list, which was causing the empty dropdown in Settings.

### 2. Improved Player Query Logic
- **[SettingsScreen.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/settings/SettingsScreen.kt)**:
    - Refined the `PlayerSelector` to use a more standard `Intent` query (MIME type only).
    - Updated the code to correctly handle `PackageManager` flags across different Android versions (using `ResolveInfoFlags` on newer versions).
    - Added a filter to exclude the app itself from the player list.

### 3. Reliable Playback Launch
- **[MainActivity.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/MainActivity.kt)**:
    - Verified the logic for "Ask Every Time", "System Default", and "Specific App".
    - The "Ask Every Time" option now correctly uses `Intent.createChooser` to force the Android system to show the app picker, even if a default is set.

## Verification Results

### Automated Tests
- Gradle build `app:assembleDebug` passed successfully.

### Manual Verification Required
- **Dropdown**: Go to Settings -> Playback -> Choose Specific App. You should now see a list of all video players installed on your device (VLC, MX Player, etc.).
- **Selection**:
    - Select **Ask Every Time**: Play a video; you should see the Android "Open with" prompt.
    - Select a **Specific App**: Play a video; it should open directly in that app.
    - Select **System Default**: Play a video; it should use the standard system resolution.
