package com.mcj130101.randplayer.data.scanner

import com.mcj130101.randplayer.domain.browser.BrowserFileItem
import com.hierynomus.smbj.auth.AuthenticationContext
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

    private fun getJcifsContext(authContext: AuthenticationContext) = run {
        val prop = java.util.Properties()
        prop.setProperty("jcifs.smb.client.enableSMB2", "true")
        prop.setProperty("jcifs.smb.client.useSMB2Negotiation", "true")
        prop.setProperty("jcifs.resolveOrder", "DNS,BCAST")
        prop.setProperty("jcifs.smb.client.connTimeout", "5000")
        prop.setProperty("jcifs.smb.client.responseTimeout", "5000")
        prop.setProperty("jcifs.smb.client.soTimeout", "5000")
        val config = jcifs.config.PropertyConfiguration(prop)
        val baseContext = jcifs.context.BaseContext(config)
        baseContext.withCredentials(
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
    }

    suspend fun listShares(server: String, authContext: AuthenticationContext): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            kotlinx.coroutines.withTimeout(6000L) {
                val context = getJcifsContext(authContext)
                val url = "smb://$server/"
                
                SmbFile(url, context).use { serverFile ->
                    val shares = serverFile.list()
                        .map { it.removeSuffix("/") }
                        .filter { !it.endsWith("$") }
                        .sorted()
                    Result.success(shares)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listFolders(server: String, share: String, path: String, authContext: AuthenticationContext): Result<List<SmbFileItem>> = withContext(Dispatchers.IO) {
        try {
            kotlinx.coroutines.withTimeout(8000L) {
                val context = getJcifsContext(authContext)
                val baseUrl = "smb://$server/${share.trim('/')}/"
                val fullUrl = if (path.isEmpty()) baseUrl else "$baseUrl${path.trim('/')}/"
                
                SmbFile(fullUrl, context).use { folder ->
                    val folders = folder.listFiles()
                        .filter { it.isDirectory }
                        .map { file ->
                            val name = file.name.removeSuffix("/")
                            val relativePath = if (path.isEmpty()) name else "${path.trim('/')}/$name"
                            SmbFileItem(
                                name = name,
                                isDirectory = true,
                                path = relativePath
                            )
                        }
                        .sortedBy { it.name.lowercase() }
                    Result.success(folders)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listFiles(url: String, authContext: AuthenticationContext): Result<List<BrowserFileItem>> = withContext(Dispatchers.IO) {
        try {
            val context = getJcifsContext(authContext)
            SmbFile(url, context).use { folder ->
                val items = folder.listFiles()
                    .map { file ->
                        BrowserFileItem(
                            id = file.url.toString(),
                            name = file.name.removeSuffix("/"),
                            isDirectory = file.isDirectory,
                            size = if (file.isDirectory) 0 else file.length(),
                            lastModified = file.lastModified(),
                            path = file.url.toString(),
                            mimeType = null // JCIFS doesn't give mime type easily
                        )
                    }
                    .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                Result.success(items)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun delete(url: String, authContext: AuthenticationContext): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val context = getJcifsContext(authContext)
            SmbFile(url, context).use { file ->
                file.delete()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rename(url: String, newName: String, authContext: AuthenticationContext): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val context = getJcifsContext(authContext)
            SmbFile(url, context).use { file ->
                val parent = file.parent
                val newFile = SmbFile(parent + newName, context)
                file.renameTo(newFile)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDirectory(url: String, authContext: AuthenticationContext): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val context = getJcifsContext(authContext)
            SmbFile(url, context).use { file ->
                file.mkdir()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
