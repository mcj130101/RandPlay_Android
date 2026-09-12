package com.example.randplayer.playback

import android.net.Uri
import com.example.randplayer.data.local.SecureCredentialStore
import com.example.randplayer.data.local.entity.VideoEntity
import com.example.randplayer.data.repository.SourceRepository
import com.example.randplayer.data.repository.VideoRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbPlaybackProvider @Inject constructor(
    private val videoRepository: VideoRepository,
    private val sourceRepository: SourceRepository,
    private val credentialStore: SecureCredentialStore
) : VideoPlaybackProvider {

    override suspend fun prepare(video: VideoEntity): PlaybackSource {
        val source = sourceRepository.getSourceById(video.sourceId)
        
        val uri = if (source?.credentialId != null) {
            val cred = credentialStore.getCredential(source.credentialId)
            if (cred != null) {
                // Construct smb://user:pass@host/share/path
                val encodedUser = Uri.encode(cred.username)
                val encodedPass = Uri.encode(cred.password)
                
                // video.id should be the full smb:// url from jcifs-ng
                // We need to inject the credentials into it
                video.id.replace("smb://", "smb://$encodedUser:$encodedPass@")
            } else {
                video.id
            }
        } else {
            video.id
        }
        
        return PlaybackSource.Stream(uri)
    }
}
