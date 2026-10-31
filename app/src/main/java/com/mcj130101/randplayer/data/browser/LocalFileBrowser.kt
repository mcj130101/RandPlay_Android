package com.mcj130101.randplayer.data.browser

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.mcj130101.randplayer.domain.browser.BrowserFileItem
import com.mcj130101.randplayer.domain.browser.FileBrowser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalFileBrowser @Inject constructor(
    @ApplicationContext private val context: Context
) : FileBrowser {

    override suspend fun listFiles(path: String): Result<List<BrowserFileItem>> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(path)
            val doc = DocumentFile.fromTreeUri(context, uri) ?: DocumentFile.fromSingleUri(context, uri)
            if (doc == null || !doc.exists() || !doc.isDirectory) {
                return@withContext Result.failure(Exception("Invalid directory: $path"))
            }

            val items = doc.listFiles().map { file ->
                BrowserFileItem(
                    id = file.uri.toString(),
                    name = file.name ?: "Unknown",
                    isDirectory = file.isDirectory,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    path = file.uri.toString(),
                    mimeType = file.type
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = DocumentFile.fromSingleUri(context, Uri.parse(path))
            if (doc != null && doc.exists()) {
                if (doc.delete()) Result.success(Unit)
                else Result.failure(Exception("Failed to delete $path"))
            } else {
                Result.failure(Exception("File not found: $path"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rename(path: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = DocumentFile.fromSingleUri(context, Uri.parse(path))
            if (doc != null && doc.exists()) {
                if (doc.renameTo(newName)) Result.success(Unit)
                else Result.failure(Exception("Failed to rename to $newName"))
            } else {
                Result.failure(Exception("File not found: $path"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun copy(sourcePath: String, targetDirectoryPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        // SAF doesn't have a direct copy method for DocumentFile. 
        // We would need to open streams. For now, let's mark it as Not Supported or implement via streams.
        // Implementing via streams is safer for a "complete" explorer.
        Result.failure(Exception("Copy not supported yet for local SAF"))
    }

    override suspend fun move(sourcePath: String, targetDirectoryPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        // SAF supports move if both are within the same provider, using DocumentsContract.moveDocument
        // But for simplicity and broad compatibility, we can skip for now or use rename if it's just a rename.
        Result.failure(Exception("Move not supported yet for local SAF"))
    }

    override suspend fun createDirectory(parentPath: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val parent = DocumentFile.fromTreeUri(context, Uri.parse(parentPath))
            if (parent != null && parent.isDirectory) {
                val newDir = parent.createDirectory(name)
                if (newDir != null) Result.success(Unit)
                else Result.failure(Exception("Failed to create directory $name"))
            } else {
                Result.failure(Exception("Invalid parent directory: $parentPath"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
