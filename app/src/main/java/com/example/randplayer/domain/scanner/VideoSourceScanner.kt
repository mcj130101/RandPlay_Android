package com.example.randplayer.domain.scanner

import com.example.randplayer.data.local.entity.VideoEntity
import com.example.randplayer.data.local.entity.VideoSourceEntity
import kotlinx.coroutines.flow.Flow

sealed interface ScanEvent {
    data object Started : ScanEvent
    data class FileFound(val video: VideoEntity) : ScanEvent
    data class Progress(val scanned: Int, val discovered: Int) : ScanEvent
    data class Completed(val added: Int, val updated: Int, val deleted: Int) : ScanEvent
    data class Error(val message: String) : ScanEvent
}

interface VideoSourceScanner {
    fun scan(source: VideoSourceEntity): Flow<ScanEvent>
}
