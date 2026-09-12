package com.example.randplayer.domain.random

import com.example.randplayer.data.local.entity.VideoEntity

interface RandomVideoSelector {
    suspend fun selectVideo(): VideoEntity?
}
