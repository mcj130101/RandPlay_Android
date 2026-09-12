package com.example.randplayer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playback_history",
    indices = [
        Index("videoId"),
        Index("playedAt")
    ],
    foreignKeys = [
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoId: String,
    val playedAt: Long,
    val completionStatus: Float? // e.g. 0.0 to 1.0 if we eventually track watch time
)
