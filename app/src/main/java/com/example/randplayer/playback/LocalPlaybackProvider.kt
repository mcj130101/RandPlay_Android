package com.example.randplayer.playback

import android.net.Uri
import com.example.randplayer.data.local.entity.VideoEntity
import javax.inject.Inject

class LocalPlaybackProvider @Inject constructor() : VideoPlaybackProvider {
    override suspend fun prepare(video: VideoEntity): PlaybackSource {
        return PlaybackSource.Local(Uri.parse(video.path))
    }
}
