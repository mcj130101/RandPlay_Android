package com.example.randplayer.sync

import com.example.randplayer.data.local.entity.VideoSourceEntity
import com.example.randplayer.domain.model.AppSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncPolicy @Inject constructor() {

    fun shouldSync(source: VideoSourceEntity, settings: AppSettings): Boolean {
        if (!source.enabled) return false
        if (!settings.autoSyncEnabled) return false
        
        val lastSync = source.lastSyncAt ?: return true
        val intervalMs = settings.syncIntervalHours * 60 * 60 * 1000L
        
        return System.currentTimeMillis() - lastSync > intervalMs
    }
}
