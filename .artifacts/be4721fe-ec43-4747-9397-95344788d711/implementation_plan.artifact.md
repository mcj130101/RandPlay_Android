# Implementation Plan - Blue Theme Reversion & Widget Fix

Restore the original look and feel for the default Blue theme and fix the "transparent square" visual issue on the Home Screen widget.

## User Review Required

> [!IMPORTANT]
> - **Blue Theme**: When the "Blue" accent color (`#3B82F6`) is selected, the app will revert to its original static theme using the blurred background image (`background.avif`).
> - **Other Themes**: All other colors will continue to use the monochromatic "Ambient Aura" animated background.
> - **Widget Fix**: The "transparent square" on the home screen widget will be addressed by refining the container hierarchy and clipping in `DiceWidget.kt`.

## Proposed Changes

### Presentation Layer

#### [MODIFY] [Theme.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/theme/Theme.kt)
- **Restore Static Color Schemes**: Re-introduce `DarkColorScheme` and `LightColorScheme` using the constants from `Color.kt`.
- **Conditional Background**:
    - If `accentColorHex == "#3B82F6"`: Show the original `AsyncImage` with blur and use the static `ColorScheme`.
    - Otherwise: Show `AmbientAuraBackground` and use `getDynamicColorScheme`.
- **Imports**: Restore `coil.compose.AsyncImage` and `androidx.compose.ui.layout.ContentScale`.

#### [MODIFY] [DiceWidget.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/widget/DiceWidget.kt)
- **Fix Transparent Square**:
    - The "transparent square" likely refers to the outer `Box` or the `clickable` area being visible as a square on some launchers.
    - I will consolidate the background and padding into a single container where possible, or ensure the outer container has no interactive highlights that extend beyond the rounded dice.
    - Specifically, I will move the `clickable` and `background` to the inner container to ensure the "active" area matches the "visual" area.

## Verification Plan

### Automated Tests
- ✅ **Gradle Build**: Ensure the project compiles.

### Manual Verification
1. **Blue Theme**: Set color to Blue. Verify the blurred background image is back and the theme colors match the original blue palette.
2. **Dynamic Themes**: Set color to Purple or Green. Verify the animated Aura background is still active.
3. **Widget**: Verify on the home screen that the square outline/shadow around the dice is gone.
