package com.example.randplayer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "videos",
    indices = [
        Index("sourceId"),
        Index("path"),
        Index("isExcluded"),
        Index("lastPlayedAt")
    ],
    foreignKeys = [
        ForeignKey(
            entity = VideoSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class VideoEntity(
    @PrimaryKey
    val id: String, // Stable ID: URI or normalized SMB path hash
    val sourceId: String,
    val path: String, // Full path or URI
    val fileName: String,
    val folderPath: String,
    val extension: String,
    val sizeBytes: Long?,
    val modifiedTimestamp: Long?,
    val durationMs: Long?,
    val isFavorite: Boolean,
    val isExcluded: Boolean,
    val playCount: Int,
    val lastPlayedAt: Long?,
    val indexedAt: Long
)
