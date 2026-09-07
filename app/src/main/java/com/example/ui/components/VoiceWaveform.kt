package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SiriCyan
import com.example.ui.theme.SiriMagenta
import com.example.ui.theme.SiriViolet
import kotlin.math.sin

@Composable
fun VoiceWaveform(
    isActive: Boolean,
    audioRms: Float = 0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val barCount = 28
        val spacing = size.width / barCount
        val centerY = size.height / 2f

        val gradient = Brush.horizontalGradient(
            colors = listOf(
                SiriCyan.copy(alpha = 0.4f),
                SiriCyan,
                SiriMagenta,
                SiriViolet,
                SiriCyan.copy(alpha = 0.4f)
            )
        )

        for (i in 0 until barCount) {
            val x = i * spacing + (spacing / 2f)
            val normalizedIdx = (i.toFloat() / barCount) * 3.1415f
            val envelope = sin(normalizedIdx)

            val baseWave = sin(phase + (i * 0.4f))
            val waveHeight = if (isActive) {
                val dynamicRms = if (audioRms > 0.05f) audioRms else 0.25f
                (size.height * 0.45f * envelope * (0.35f + (baseWave * 0.35f) + dynamicRms)).coerceAtLeast(4f)
            } else {
                3f
            }

            drawLine(
                brush = gradient,
                start = Offset(x, centerY - waveHeight),
                end = Offset(x, centerY + waveHeight),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )
        }
    }
}
