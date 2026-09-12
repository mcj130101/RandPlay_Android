package com.example.randplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.randplayer.data.local.dao.PlaybackHistoryDao
import com.example.randplayer.data.local.dao.SyncLogDao
import com.example.randplayer.data.local.dao.VideoDao
import com.example.randplayer.data.local.dao.VideoSourceDao
import com.example.randplayer.data.local.entity.PlaybackHistoryEntity
import com.example.randplayer.data.local.entity.SyncLogEntity
import com.example.randplayer.data.local.entity.VideoEntity
import com.example.randplayer.data.local.entity.VideoSourceEntity

@Database(
    entities = [
        VideoEntity::class,
        VideoSourceEntity::class,
        PlaybackHistoryEntity::class,
        SyncLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun videoSourceDao(): VideoSourceDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        const val DATABASE_NAME = "rand_player_db"
    }
}
