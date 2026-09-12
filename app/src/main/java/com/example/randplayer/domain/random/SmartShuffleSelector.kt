package com.example.randplayer.domain.random

import com.example.randplayer.data.local.entity.VideoEntity
import com.example.randplayer.data.repository.PlaybackRepository
import com.example.randplayer.data.repository.SettingsRepository
import com.example.randplayer.data.repository.VideoRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

class SmartShuffleSelector @Inject constructor(
    private val videoRepository: VideoRepository,
    private val playbackRepository: PlaybackRepository,
    private val settingsRepository: SettingsRepository
) : RandomVideoSelector {
    override suspend fun selectVideo(): VideoEntity? {
        val settings = settingsRepository.settingsFlow.first()
        val limit = settings.smartShuffleHistoryLimit
        
        val recentVideoIds = playbackRepository.getRecentHistory(limit).first().map { it.videoId }.toSet()
        
        // This could be slow if there are millions of videos.
        // A better SQL query would be: SELECT * FROM videos WHERE id NOT IN (SELECT videoId FROM playback_history ORDER BY playedAt DESC LIMIT :limit)
        // For now, let's do a simple filter if the total count is reasonable, or just fallback to random if we can't find one after X tries.
        
        // Let's use a count-based approach for performance.
        val eligibleVideos = videoRepository.getRandomEligibleVideo() ?: return null
        
        if (eligibleVideos.id !in recentVideoIds) {
            return eligibleVideos
        }
        
        // Try a few more times or just return any if we can't find a "new" one
        for (i in 0 until 5) {
            val candidate = videoRepository.getRandomEligibleVideo() ?: continue
            if (candidate.id !in recentVideoIds) return candidate
        }
        
        return eligibleVideos // Fallback
    }
}
