package com.mcj130101.randplayer.playback

import android.net.Uri
import com.mcj130101.randplayer.data.local.SecureCredentialStore
import com.mcj130101.randplayer.data.local.entity.VideoEntity
import com.mcj130101.randplayer.data.repository.SourceRepository
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbPlaybackProvider @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val credentialStore: SecureCredentialStore
) : VideoPlaybackProvider {

    override suspend fun prepare(video: VideoEntity): PlaybackSource = withContext(Dispatchers.IO) {
        val source = sourceRepository.getSourceById(video.sourceId)
        
        val auth = if (source?.credentialId != null) {
            val cred = credentialStore.getCredential(source.credentialId)
            if (cred != null) {
                NtlmPasswordAuthenticator(cred.domain ?: "", cred.username, cred.password)
            } else {
                NtlmPasswordAuthenticator()
            }
        } else {
            NtlmPasswordAuthenticator()
        }

        val prop = java.util.Properties()
        prop.setProperty("jcifs.smb.client.enableSMB2", "true")
        prop.setProperty("jcifs.smb.client.useSMB2Negotiation", "true")
        prop.setProperty("jcifs.resolveOrder", "DNS")
        prop.setProperty("jcifs.smb.client.connTimeout", "3000")
        prop.setProperty("jcifs.smb.client.responseTimeout", "3000")
        prop.setProperty("jcifs.smb.client.soTimeout", "3000")
        val config = jcifs.config.PropertyConfiguration(prop)
        val baseContext = jcifs.context.BaseContext(config)
        val context = baseContext.withCredentials(auth)

        try {
            SmbFile(video.id, context).use { smbFile ->
                if (!smbFile.exists()) {
                    throw IOException("SMB file or share unavailable: ${video.id}")
                }
            }
        } catch (e: Exception) {
            throw IOException("SMB storage offline or unreachable: ${e.message}", e)
        }

        val uri = if (source?.credentialId != null) {
            val cred = credentialStore.getCredential(source.credentialId)
            if (cred != null) {
                val encodedUser = Uri.encode(cred.username)
                val encodedPass = Uri.encode(cred.password)
                video.id.replace("smb://", "smb://$encodedUser:$encodedPass@")
            } else {
                video.id
            }
        } else {
            video.id
        }

        PlaybackSource.Stream(uri)
    }
}
