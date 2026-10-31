package com.mcj130101.randplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mcj130101.randplayer.data.local.dao.PlaybackHistoryDao
import com.mcj130101.randplayer.data.local.dao.SyncLogDao
import com.mcj130101.randplayer.data.local.dao.VideoDao
import com.mcj130101.randplayer.data.local.dao.VideoSourceDao
import com.mcj130101.randplayer.data.local.entity.PlaybackHistoryEntity
import com.mcj130101.randplayer.data.local.entity.SyncLogEntity
import com.mcj130101.randplayer.data.local.entity.VideoEntity
import com.mcj130101.randplayer.data.local.entity.VideoSourceEntity

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
