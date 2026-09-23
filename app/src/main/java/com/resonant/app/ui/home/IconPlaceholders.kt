package com.resonant.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension * 0.32f
        val toothLength = size.minDimension * 0.16f
        val toothWidth = size.minDimension * 0.1f
        val ringWidth = size.minDimension * 0.09f

        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val dx = cos(angle).toFloat()
            val dy = sin(angle).toFloat()
            drawLine(
                color = tint,
                start = Offset(center.x + dx * outerRadius, center.y + dy * outerRadius),
                end = Offset(
                    center.x + dx * (outerRadius + toothLength),
                    center.y + dy * (outerRadius + toothLength)
                ),
                strokeWidth = toothWidth,
                cap = StrokeCap.Round
            )
        }
        drawCircle(color = tint, radius = outerRadius, center = center, style = Stroke(width = ringWidth))
        drawCircle(color = tint, radius = outerRadius * 0.32f, center = center)
    }
}
