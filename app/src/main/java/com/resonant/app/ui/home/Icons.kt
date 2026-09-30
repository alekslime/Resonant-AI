package com.resonant.app.ui.home

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.resonant.app.R

/**
 * The Figma mic and settings icons, exported as vector drawables (res/drawable/ic_mic.xml,
 * ic_settings.xml). [tint] recolors them, so one asset serves every background.
 * Decorative: the buttons that hold them carry their own spoken labels.
 */
@Composable
fun MicIcon(tint: Color, modifier: Modifier = Modifier) {
    Icon(painterResource(R.drawable.ic_mic), contentDescription = null, tint = tint, modifier = modifier)
}

@Composable
fun SettingsIcon(tint: Color, modifier: Modifier = Modifier) {
    Icon(painterResource(R.drawable.ic_settings), contentDescription = null, tint = tint, modifier = modifier)
}
