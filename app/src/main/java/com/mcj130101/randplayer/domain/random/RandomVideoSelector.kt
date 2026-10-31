package com.mcj130101.randplayer.domain.random

import com.mcj130101.randplayer.data.local.entity.VideoEntity

interface RandomVideoSelector {
    suspend fun selectVideo(): VideoEntity?
}
