package com.example.randplayer.data.local

import androidx.room.TypeConverter
import com.example.randplayer.data.local.entity.SourceType
import com.example.randplayer.data.local.entity.SyncStatus

class Converters {
    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = enumValueOf(value)

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = enumValueOf(value)
}
