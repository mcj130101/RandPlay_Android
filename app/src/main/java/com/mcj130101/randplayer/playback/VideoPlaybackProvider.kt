package com.mcj130101.randplayer.playback

import android.net.Uri
import com.mcj130101.randplayer.data.local.entity.VideoEntity

sealed interface PlaybackSource {
    data class Local(val uri: Uri) : PlaybackSource
    data class Stream(val url: String) : PlaybackSource
}

interface VideoPlaybackProvider {
    suspend fun prepare(video: VideoEntity): PlaybackSource
}
