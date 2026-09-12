package com.example.randplayer.domain.random

import com.example.randplayer.data.local.entity.VideoEntity
import com.example.randplayer.data.repository.VideoRepository
import javax.inject.Inject

class UniformRandomSelector @Inject constructor(
    private val videoRepository: VideoRepository
) : RandomVideoSelector {
    override suspend fun selectVideo(): VideoEntity? {
        return videoRepository.getRandomEligibleVideo()
    }
}
