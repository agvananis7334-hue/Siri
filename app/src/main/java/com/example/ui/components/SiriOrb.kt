package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SiriBlue
import com.example.ui.theme.SiriCyan
import com.example.ui.theme.SiriMagenta
import com.example.ui.theme.SiriViolet
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SiriOrb(
    isListening: Boolean,
    isSpeaking: Boolean,
    isThinking: Boolean,
    audioRms: Float = 0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "siri_orb_anim")

    // Rotation animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 2500 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Fluid wave shift
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    val scale = when {
        isListening -> 1.0f + (audioRms * 0.45f)
        isSpeaking -> breathingPulse * 1.05f
        isThinking -> breathingPulse
        else -> breathingPulse * 0.98f
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .testTag("siri_orb_button")
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.65f * scale

            // 1. Ambient outer aura glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SiriCyan.copy(alpha = if (isListening) 0.35f else 0.15f),
                        SiriMagenta.copy(alpha = if (isSpeaking) 0.30f else 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.6f
                ),
                radius = baseRadius * 1.6f,
                center = center
            )

            // 2. Multi-layered dynamic fluid wave rings
            drawFluidRings(
                center = center,
                radius = baseRadius,
                rotationDeg = rotation,
                waveOffset = waveOffset,
                isListening = isListening,
                isSpeaking = isSpeaking
            )

            // 3. Dense iridescent core sphere
            val coreGradient = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isListening) 0.95f else 0.85f),
                    SiriCyan.copy(alpha = 0.90f),
                    SiriMagenta.copy(alpha = 0.85f),
                    SiriViolet.copy(alpha = 0.70f),
                    SiriBlue.copy(alpha = 0.50f)
                ),
                center = Offset(
                    center.x - baseRadius * 0.2f * cos(Math.toRadians(rotation.toDouble())).toFloat(),
                    center.y - baseRadius * 0.2f * sin(Math.toRadians(rotation.toDouble())).toFloat()
                ),
                radius = baseRadius * 0.95f
            )

            drawCircle(
                brush = coreGradient,
                radius = baseRadius * 0.75f,
                center = center
            )

            // 4. Center soundwave / energy pulse
            if (isListening || isSpeaking) {
                drawActiveEnergy(center, baseRadius * 0.45f, audioRms, isListening)
            }
        }
    }
}

private fun DrawScope.drawFluidRings(
    center: Offset,
    radius: Float,
    rotationDeg: Float,
    waveOffset: Float,
    isListening: Boolean,
    isSpeaking: Boolean
) {
    val ringColors = listOf(
        SiriCyan to SiriViolet,
        SiriMagenta to SiriCyan,
        SiriBlue to SiriMagenta,
        SiriViolet to SiriCyan
    )

    ringColors.forEachIndexed { index, (c1, c2) ->
        val phase = waveOffset + (index * 1.57f)
        val wobble = sin(phase) * (if (isListening) 12f else 6f)
        val ringRadius = radius * (0.85f + (index * 0.08f)) + wobble
        val ringRotation = rotationDeg * (if (index % 2 == 0) 1f else -1f)

        val brush = Brush.sweepGradient(
            colors = listOf(
                c1.copy(alpha = 0.85f),
                c2.copy(alpha = 0.75f),
                c1.copy(alpha = 0.15f),
                c2.copy(alpha = 0.85f)
            ),
            center = center
        )

        drawCircle(
            brush = brush,
            radius = ringRadius,
            center = center,
            style = Stroke(width = if (isSpeaking) 5f else 3.5f)
        )
    }
}

private fun DrawScope.drawActiveEnergy(
    center: Offset,
    maxRadius: Float,
    audioRms: Float,
    isListening: Boolean
) {
    val barCount = 5
    val barWidth = 4f
    val spacing = 8f
    val totalWidth = (barCount * barWidth) + ((barCount - 1) * spacing)
    val startX = center.x - (totalWidth / 2f)

    for (i in 0 until barCount) {
        val x = startX + (i * (barWidth + spacing))
        val factor = when (i) {
            0, 4 -> 0.35f
            1, 3 -> 0.7f
            else -> 1.0f
        }
        val amplitude = if (isListening) {
            (maxRadius * 0.3f) + (audioRms * maxRadius * 0.7f * factor)
        } else {
            maxRadius * 0.6f * factor
        }

        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = Offset(x, center.y - amplitude),
            end = Offset(x, center.y + amplitude),
            strokeWidth = barWidth
        )
    }
}
