package com.resonant.app.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.ui.components.DotsState

/**
 * Debug-only strip above the Ask pill (Settings > Debug Mode > swipe left in the middle turns it
 * on). Forces any dots animation with a simulated or slider-driven level, so every state can be
 * checked without Ollama, Whisper or TTS cooperating. Not shown otherwise.
 *
 * @param forced the forced state name, or null = follow the real app state ("Auto")
 * @param level slider level 0..1, or null = simulated (the built-in fake envelope / baseline)
 */
@Composable
fun DotsDebugPanel(
    forced: String?,
    level: Float?,
    onForce: (String?) -> Unit,
    onLevel: (Float?) -> Unit
) {
    val ink = Color(0xFF0F0F0F)
    @Composable
    fun Chip(label: String, on: Boolean, onClick: () -> Unit) {
        Box(
            Modifier
                .clip(RoundedCornerShape(percent = 50))
                .then(if (on) Modifier.background(ink) else Modifier.border(BorderStroke(2.dp, ink), RoundedCornerShape(percent = 50)))
                .semantics { role = Role.Button }
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = if (on) Color(0xFFFFAE00) else ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }

    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Chip("Auto", forced == null) { onForce(null) }
            listOf(
                DotsState.Idle, DotsState.Listening, DotsState.Transcribing,
                DotsState.Thinking, DotsState.Speaking, DotsState.Offline
            ).forEach { Chip(it.name, forced == it.name) { onForce(it.name) } }
        }
        if (forced != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Chip(if (level == null) "Sim level" else "Level ${(level * 100).toInt()}%", level == null) {
                    onLevel(if (level == null) .5f else null)
                }
                Slider(
                    value = level ?: 0f,
                    onValueChange = { onLevel(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = ink, activeTrackColor = ink, inactiveTrackColor = ink.copy(alpha = .25f)
                    ),
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
            }
        }
    }
}
