package com.example.randplayer.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import com.example.randplayer.R
import com.example.randplayer.data.repository.SettingsRepository
import com.example.randplayer.domain.model.PlayerSelectionMode
import com.example.randplayer.domain.model.RandomMode
import com.example.randplayer.domain.random.SmartShuffleSelector
import com.example.randplayer.domain.random.UniformRandomSelector
import com.example.randplayer.playback.PlaybackManager
import com.example.randplayer.playback.PlaybackSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import androidx.core.graphics.toColorInt
import androidx.glance.layout.size

class DiceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DiceWidget()
}

class DiceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            PlayRandomVideoAction.WidgetEntryPoint::class.java
        )
        val settings = entryPoint.settingsRepository().settingsFlow.first()
        val accentColor = try {
            Color(settings.accentColorHex.toColorInt())
        } catch (_: Exception) {
            Color(0xFF3B82F6) // Fallback Blue 500
        }

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(accentColor)
                        .cornerRadius(16.dp)
                        .clickable(actionRunCallback<PlayRandomVideoAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_launcher_foreground),
                        contentDescription = "Roll Dice",
                        modifier = GlanceModifier.size(110.dp), // Slightly adjusted scale
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
        }
    }
}

class PlayRandomVideoAction : ActionCallback {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun settingsRepository(): SettingsRepository
        fun uniformSelector(): UniformRandomSelector
        fun smartSelector(): SmartShuffleSelector
        fun playbackManager(): PlaybackManager
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )

        val settingsRepository = entryPoint.settingsRepository()
        val uniformSelector = entryPoint.uniformSelector()
        val smartSelector = entryPoint.smartSelector()
        val playbackManager = entryPoint.playbackManager()

        val settings = settingsRepository.settingsFlow.first()
        val selector = when (settings.randomMode) {
            RandomMode.SMART_SHUFFLE -> smartSelector
            else -> uniformSelector
        }

        val video = selector.selectVideo()
        if (video != null) {
            val playbackSource = playbackManager.preparePlayback(video.id)
            if (playbackSource != null) {
                val intent = when (playbackSource) {
                    is PlaybackSource.Local -> {
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(playbackSource.uri, "video/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                    is PlaybackSource.Stream -> {
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(playbackSource.url.toUri(), "video/*")
                        }
                    }
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                try {
                    when (settings.playerSelectionMode) {
                        PlayerSelectionMode.SPECIFIC_APP -> {
                            val preferredPlayer = settings.preferredPlayerPackage
                            if (!preferredPlayer.isNullOrEmpty()) {
                                intent.setPackage(preferredPlayer)
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    intent.setPackage(null)
                                    context.startActivity(intent)
                                }
                            } else {
                                context.startActivity(intent)
                            }
                        }
                        PlayerSelectionMode.ASK_EVERY_TIME -> {
                            val chooser = Intent.createChooser(intent, "Open video with...")
                            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(chooser)
                        }
                        PlayerSelectionMode.SYSTEM_DEFAULT -> {
                            context.startActivity(intent)
                        }
                    }
                } catch (_: Exception) { }
            }
        }
    }
}
