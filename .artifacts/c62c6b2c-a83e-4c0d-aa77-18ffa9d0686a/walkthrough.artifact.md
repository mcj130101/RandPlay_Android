# Walkthrough - Dice Animation & Missing Features

I have reverted the dice animation back to the original rotating casino icon, added the pulsing background ring effect, and implemented the Shake to Play and Home Screen Widget features.

## Changes Made

### 1. Dice Animation Reversion & Enhancement
- **Reverted Lottie**: Removed the `lottie-compose` dependency and the placeholder JSON file.
- **Enhanced Original**: Restored the `Animatable` and `Rotate` modifiers for the `Icons.Rounded.Casino`.
- **Pulse Effect**: Added an `infiniteTransition` to create a continuous pulsing glow ring behind the dice button when it is idle.

### 2. Shake to Play
- **[ShakeDetector.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/components/ShakeDetector.kt)**: Created a new Compose effect that uses the Android `SensorManager` to listen to the Accelerometer.
- **Integration**: Placed the `ShakeDetector` inside the `HomeScreen`. Shaking the phone now triggers `viewModel.rollDice()` exactly as if the button was clicked.

### 3. Home Screen Widget
- **Glance Framework**: Added Google's Jetpack Glance framework for building modern app widgets.
- **[DiceWidget.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/widget/DiceWidget.kt)**: Designed a simple 2x2 widget containing the RandPlayer icon.
- **Widget Receiver**: Created `DiceWidgetReceiver` and registered it with `dice_widget_info.xml` metadata in the `AndroidManifest.xml` so it appears in the Android OS widget picker.
- **Instant Play**: Tapping the widget launches `MainActivity` with a specific Intent action (`ACTION_PLAY_RANDOM`). The `DashboardPager` intercepts this on boot and immediately triggers the dice roll animation and playback on the Home screen.

## Verification Results

### Automated Tests
- Gradle build `app:assembleDebug` passed successfully.

### Manual Verification Required
- **Dice**: Go to the Home screen and verify the dice button rotates when clicked, and has a pulsing glow when idle.
- **Shake**: Physically shake the device while on the Home screen.
- **Widget**: Long-press your Android home screen -> Widgets -> find RandPlayer. Add the widget, tap it, and verify it launches the app directly into a dice roll.
