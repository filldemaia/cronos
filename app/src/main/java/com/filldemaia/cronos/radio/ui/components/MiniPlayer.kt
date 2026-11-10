package com.filldemaia.cronos.radio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filldemaia.cronos.radio.playback.PlaybackSnapshot
import com.filldemaia.cronos.radio.ui.theme.RadioMuted
import com.filldemaia.cronos.radio.ui.theme.RadioOnAccent
import com.filldemaia.cronos.radio.ui.theme.RadioOrange
import com.filldemaia.cronos.radio.ui.theme.RadioSurfaceHigh
import com.filldemaia.cronos.radio.ui.theme.RadioSurfaceHighest
import com.filldemaia.cronos.radio.ui.theme.RadioText

@Composable
fun MiniPlayer(
    playback: PlaybackSnapshot,
    onOpen: () -> Unit,
    onTogglePlayPause: () -> Unit
) {
    val station = playback.station
    Surface(
        onClick = onOpen,
        color = RadioSurfaceHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            if (playback.hasTimeshift) {
                val ratio = playback.window.progress(playback.positionUs)
                    .coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(ratio)
                            .height(2.dp)
                            .background(RadioOrange)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (station != null) {
                    StationLogo(station, 40.dp)
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(RadioSurfaceHighest, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Radio, contentDescription = null, tint = RadioMuted)
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        station?.name ?: "Cronos",
                        style = MaterialTheme.typography.titleSmall,
                        color = RadioText,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        playback.statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (playback.errorMessage != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            RadioMuted
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onTogglePlayPause,
                    enabled = !playback.isLocalAlarm,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = RadioOrange,
                        contentColor = RadioOnAccent
                    )
                ) {
                    Icon(
                        if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playback.isPlaying) "Pausa" else "Reprodueix",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
