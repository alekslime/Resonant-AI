package com.resonant.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.Composable
import kotlin.math.cos
import kotlin.math.sin

/**
 * PLACEHOLDER icons — hand-drawn shapes standing in for the real mic/settings
 * assets Figma exports as SVGs (Home frame, node 2001:33). This sandbox can't
 * reach Figma's asset-download URLs, so these are here only until someone
 * exports those two SVGs and drops them into res/drawable — swap the call
 * sites in HomeScreen.kt for real vector assets at that point and delete
 * this file.
 */

@Composable
fun MicPlaceholderIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val capsuleWidth = w * 0.34f
        val capsuleHeight = h * 0.46f
        val capsuleTop = h * 0.08f
        val strokeWidth = w * 0.09f

        drawRoundRect(
            color = tint,
            topLeft = Offset((w - capsuleWidth) / 2f, capsuleTop),
            size = Size(capsuleWidth, capsuleHeight),
            cornerRadius = CornerRadius(capsuleWidth / 2f, capsuleWidth / 2f)
        )
        drawArc(
            color = tint,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset(w * 0.16f, capsuleTop + capsuleHeight * 0.05f),
            size = Size(w * 0.68f, h * 0.6f)
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, capsuleTop + capsuleHeight + h * 0.18f),
            end = Offset(w * 0.5f, h * 0.88f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.3f, h * 0.88f),
            end = Offset(w * 0.7f, h * 0.88f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SettingsPlaceholderIcon(tint: Color, modifier: Modifier = Modifier) {
    // Solid cog with a hole, like the Figma icon. Offscreen layer so the hole is truly
    // transparent (shows the button behind it) rather than painted in a guessed color.
    Canvas(modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val m = size.minDimension
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val dx = cos(angle).toFloat()
            val dy = sin(angle).toFloat()
            drawLine(
                color = tint,
                start = Offset(center.x + dx * m * 0.28f, center.y + dy * m * 0.28f),
                end = Offset(center.x + dx * m * 0.46f, center.y + dy * m * 0.46f),
                strokeWidth = m * 0.2f,
                cap = StrokeCap.Butt
            )
        }
        drawCircle(color = tint, radius = m * 0.34f, center = center)
        drawCircle(color = Color.Black, radius = m * 0.14f, center = center, blendMode = BlendMode.Clear)
    }
}
