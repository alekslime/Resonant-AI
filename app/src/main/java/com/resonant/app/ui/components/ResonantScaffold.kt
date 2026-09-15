package com.resonant.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.resonant.app.ui.theme.ResonantBlack
import com.resonant.app.ui.theme.ResonantYellow

@Composable
fun ResonantScaffold(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize().background(ResonantYellow)) {
        // Decorative left strip
        Box(
            Modifier
                .fillMaxHeight()
                .width(10.dp)
                .background(ResonantBlack)
        )

        Column(Modifier.fillMaxSize()) {
            // Black header, yellow text
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(ResonantBlack)
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = ResonantYellow,
                        modifier = Modifier.semantics { heading() }
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ResonantYellow.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

val ScreenPadding = PaddingValues(24.dp)
