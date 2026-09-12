package com.example.randplayer.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import com.example.randplayer.MainActivity
import com.example.randplayer.R

class DiceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DiceWidget()
}

class DiceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = "com.example.randplayer.ACTION_PLAY_RANDOM"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clickable(actionStartActivity(intent)),
                contentAlignment = Alignment.Center
            ) {
                // We use a simple image for the widget since Glance doesn't support complex Compose Canvas
                Box(
                    modifier = GlanceModifier
                        .size(100.dp)
                        .background(Color(0xFF3B82F6)) // Blue500
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_launcher_foreground), // Using app icon as fallback for now
                        contentDescription = "Roll Dice",
                        modifier = GlanceModifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
