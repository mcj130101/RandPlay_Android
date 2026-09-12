package com.example.randplayer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_logs",
    indices = [
        Index("sourceId"),
        Index("timestamp")
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
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceId: String,
    val timestamp: Long,
    val isError: Boolean,
    val message: String,
    val itemsAdded: Int = 0,
    val itemsUpdated: Int = 0,
    val itemsDeleted: Int = 0
)
