package com.example.randplayer.domain.model

enum class RandomMode {
    UNIFORM,
    WEIGHTED,
    SMART_SHUFFLE,
    RANDOM_FOLDER
}

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

enum class PlayerSelectionMode {
    ASK_EVERY_TIME,
    SYSTEM_DEFAULT,
    SPECIFIC_APP
}

data class AppSettings(
    val randomMode: RandomMode = RandomMode.UNIFORM,
    val smartShuffleHistoryLimit: Int = 50,
    val playOnShakeEnabled: Boolean = true,
    val autoSyncEnabled: Boolean = true,
    val syncIntervalHours: Int = 6,
    val syncWifiOnly: Boolean = true,
    val syncOnMobile: Boolean = false,
    val syncWhileCharging: Boolean = false,
    val syncBeforePlayback: Boolean = false,
    val supportedExtensions: List<String> = listOf(
        "mp4", "mkv", "avi", "mov", "webm", "m4v", "mpeg", "mpg", "ts", "m2ts", "3gp", "flv", "wmv"
    ),
    val preferredPlayerPackage: String? = null,
    val playerSelectionMode: PlayerSelectionMode = PlayerSelectionMode.SYSTEM_DEFAULT,
    val theme: AppTheme = AppTheme.SYSTEM,
    val accentColorHex: String = "#3B82F6" // Default: Blue 500
)
