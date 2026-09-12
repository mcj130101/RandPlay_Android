package com.example.randplayer.data.scanner

import com.hierynomus.smbj.auth.AuthenticationContext
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SmbFileItem(
    val name: String,
    val isDirectory: Boolean,
    val path: String // relative to share root or absolute smb url
)

@Singleton
class SmbBrowser @Inject constructor() {

    private fun getJcifsContext(authContext: AuthenticationContext) = 
        SingletonContext.getInstance().withCredentials(
            if (authContext.isAnonymous) {
                NtlmPasswordAuthenticator()
            } else {
                NtlmPasswordAuthenticator(
                    authContext.domain ?: "",
                    authContext.username,
                    String(authContext.password)
                )
            }
        )

    suspend fun listShares(server: String, authContext: AuthenticationContext): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val context = getJcifsContext(authContext)
            val url = "smb://$server/"
            
            SmbFile(url, context).use { serverFile ->
                val shares = serverFile.list()
                    .map { it.removeSuffix("/") }
                    .filter { !it.endsWith("$") }
                    .sorted()
                Result.success(shares)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listFolders(server: String, share: String, path: String, authContext: AuthenticationContext): Result<List<SmbFileItem>> = withContext(Dispatchers.IO) {
        try {
            val context = getJcifsContext(authContext)
            // Ensure path starts with / and ends with / if not empty
            val cleanPath = path.trim('/').let { if (it.isEmpty()) "" else "$it/" }
            val url = "smb://$server/$share/$cleanPath"
            
            SmbFile(url, context).use { folder ->
                val items = folder.listFiles()
                    .filter { it.isDirectory }
                    .map { file ->
                        SmbFileItem(
                            name = file.name.removeSuffix("/"),
                            isDirectory = true,
                            path = cleanPath + file.name.removeSuffix("/")
                        )
                    }
                    .sortedBy { it.name }
                Result.success(items)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
