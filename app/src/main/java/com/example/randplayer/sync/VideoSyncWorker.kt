package com.example.randplayer.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.randplayer.data.repository.SettingsRepository
import com.example.randplayer.data.repository.SourceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class VideoSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sourceRepository: SourceRepository,
    private val settingsRepository: SettingsRepository,
    private val syncCoordinator: SyncCoordinator,
    private val syncPolicy: SyncPolicy
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.settingsFlow.first()
        val sources = sourceRepository.getAllSources().first()

        for (source in sources) {
            if (syncPolicy.shouldSync(source, settings)) {
                syncCoordinator.syncSource(source.id)
            }
        }

        return Result.success()
    }
}
