package com.mcj130101.randplayer.playback

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.mcj130101.randplayer.data.local.entity.VideoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import javax.inject.Inject

class LocalPlaybackProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) : VideoPlaybackProvider {
    override suspend fun prepare(video: VideoEntity): PlaybackSource {
        val uri = Uri.parse(video.path)
        val doc = DocumentFile.fromSingleUri(context, uri) ?: DocumentFile.fromTreeUri(context, uri)
        if (doc == null || !doc.exists()) {
            throw FileNotFoundException("Local storage or file unavailable: ${video.path}")
        }
        return PlaybackSource.Local(uri)
    }
}
