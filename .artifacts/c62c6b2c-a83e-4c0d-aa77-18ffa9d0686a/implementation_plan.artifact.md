# Implementation Plan - Fix Missing Features & Restore Animation

This plan addresses the missing features (Shake to Roll, Widget) that were overlooked in the previous run, and reverts the Lottie animation back to the original Compose animation as requested.

## Proposed Changes

### 1. Restore Old Dice Animation
- **[MODIFY] [DiceButton.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/components/DiceButton.kt)**: Revert to the original `Animatable`-based rotation and scale animation.
- **[MODIFY] [build.gradle.kts](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/build.gradle.kts)**: Remove `lottie-compose` dependency.

### 2. Implement Shake to Play
- **[NEW] [ShakeDetector.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/components/ShakeDetector.kt)**: Create a reusable Compose effect that registers a `SensorEventListener` for the accelerometer and detects shake gestures.
- **[MODIFY] [HomeScreen.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/home/HomeScreen.kt)**: Apply the `ShakeDetector` to trigger `viewModel.rollDice()`.

### 3. Implement Home Screen Widget
- **[NEW] [DiceWidget.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/widget/DiceWidget.kt)**: Create `DiceWidget` (extending `GlanceAppWidget`) and `DiceWidgetReceiver` (extending `GlanceAppWidgetReceiver`).
- **[NEW] [dice_widget_info.xml](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/res/xml/dice_widget_info.xml)**: Define the AppWidgetProviderInfo for the widget.
- **[MODIFY] [AndroidManifest.xml](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/AndroidManifest.xml)**: Register the `DiceWidgetReceiver` so it appears in the Android widget list.
- **[MODIFY] [MainActivity.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/MainActivity.kt)**: Add intent handling to roll the dice immediately if launched from the widget.

## Verification Plan

### Automated Tests
- Run `gradle_build app:assembleDebug` to verify no compilation errors.

### Manual Verification
- **Dice Animation**: Open Home screen, click dice. Verify it uses the old rotating casino icon instead of Lottie.
- **Shake to Play**: Physically shake the device while on the Home screen. Verify it triggers a roll.
- **Widget**: Long-press the Android launcher home screen, open widgets, and verify "RandPlayer" widget is listed. Add it to the home screen.
