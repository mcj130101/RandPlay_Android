package com.mcj130101.randplayer.domain.random

import com.mcj130101.randplayer.data.local.entity.VideoEntity
import com.mcj130101.randplayer.data.repository.VideoRepository
import javax.inject.Inject

class UniformRandomSelector @Inject constructor(
    private val videoRepository: VideoRepository
) : RandomVideoSelector {
    override suspend fun selectVideo(): VideoEntity? {
        return videoRepository.getRandomEligibleVideo()
    }
}
