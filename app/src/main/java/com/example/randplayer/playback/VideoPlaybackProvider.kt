package com.example.randplayer.playback

import android.net.Uri
import com.example.randplayer.data.local.entity.VideoEntity

sealed interface PlaybackSource {
    data class Local(val uri: Uri) : PlaybackSource
    data class Stream(val url: String) : PlaybackSource
}

interface VideoPlaybackProvider {
    suspend fun prepare(video: VideoEntity): PlaybackSource
}
