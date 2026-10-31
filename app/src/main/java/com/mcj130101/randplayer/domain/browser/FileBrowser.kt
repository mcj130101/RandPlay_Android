package com.mcj130101.randplayer.domain.browser

import kotlinx.coroutines.flow.Flow

data class BrowserFileItem(
    val id: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val path: String,
    val mimeType: String? = null
)

interface FileBrowser {
    suspend fun listFiles(path: String): Result<List<BrowserFileItem>>
    suspend fun delete(path: String): Result<Unit>
    suspend fun rename(path: String, newName: String): Result<Unit>
    suspend fun copy(sourcePath: String, targetDirectoryPath: String): Result<Unit>
    suspend fun move(sourcePath: String, targetDirectoryPath: String): Result<Unit>
    suspend fun createDirectory(parentPath: String, name: String): Result<Unit>
}
