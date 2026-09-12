package com.example.randplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SourceType {
    LOCAL,
    SMB
}

enum class SyncStatus {
    IDLE,
    SYNCING,
    ERROR,
    OFFLINE
}

@Entity(tableName = "video_sources")
data class VideoSourceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: SourceType,
    val location: String, // URI for local, SMB path/share for SMB
    val credentialId: String?, // Null if no auth required
    val recursive: Boolean,
    val enabled: Boolean,
    val createdAt: Long,
    val lastSyncAt: Long?,
    val lastSyncStatus: SyncStatus,
    val lastSyncError: String?
)
