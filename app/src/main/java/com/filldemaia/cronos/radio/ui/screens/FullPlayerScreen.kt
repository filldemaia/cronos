package com.filldemaia.cronos.radio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filldemaia.cronos.radio.data.model.AppSettings
import com.filldemaia.cronos.radio.playback.PlaybackMode
import com.filldemaia.cronos.radio.playback.PlaybackSnapshot
import com.filldemaia.cronos.radio.ui.components.CronosSeekBar
import com.filldemaia.cronos.radio.ui.components.MiniLabel
import com.filldemaia.cronos.radio.ui.components.StationLogo
import com.filldemaia.cronos.radio.ui.components.StatusPill
import com.filldemaia.cronos.radio.ui.components.formatClock
import com.filldemaia.cronos.radio.ui.theme.RadioBlue
import com.filldemaia.cronos.radio.ui.theme.RadioMuted
import com.filldemaia.cronos.radio.ui.theme.RadioNight
import com.filldemaia.cronos.radio.ui.theme.RadioOnAccent
import com.filldemaia.cronos.radio.ui.theme.RadioOrange
import com.filldemaia.cronos.radio.ui.theme.RadioRed
import com.filldemaia.cronos.radio.ui.theme.RadioSurface
import com.filldemaia.cronos.radio.ui.theme.RadioSurfaceHigh
import com.filldemaia.cronos.radio.ui.theme.RadioText
import com.filldemaia.cronos.radio.ui.theme.RadioYellow
import kotlin.math.abs
import kotlinx.coroutines.delay

@Composable
fun FullPlayerScreen(
    playback: PlaybackSnapshot,
    settings: AppSettings,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onStopLocalAlarm: () -> Unit,
    onSeekRelative: (Int) -> Unit,
    onGoLive: () -> Unit,
    onSeekToUs: (Long) -> Unit,
    onSetSleepTimer: (Int?) -> Unit
) {
    BackHandler(onBack = onClose)
    var menuOpen by remember { mutableStateOf(false) }
    var sleepDialogOpen by remember { mutableStateOf(false) }

    Surface(
        color = RadioNight,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(RadioSurfaceHigh, RadioNight, RadioNight)
                    )
                )
                .systemBarsPadding()
                .padding(horizontal = 22.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Tanca el reproductor",
                        tint = RadioText,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "ARA SONA",
                    style = MaterialTheme.typography.labelSmall,
                    color = RadioMuted,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Opcions",
                            tint = RadioText
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Temporitzador") },
                            leadingIcon = {
                                Icon(Icons.Filled.Alarm, contentDescription = null)
                            },
                            onClick = {
                                menuOpen = false
                                sleepDialogOpen = true
                            }
                        )
                        if (playback.mode != PlaybackMode.IDLE && !playback.isAtLive) {
                            DropdownMenuItem(
                                text = { Text("Torna al directe") },
                                onClick = {
                                    menuOpen = false
                                    onGoLive()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Atura la reproducció") },
                            leadingIcon = {
                                Icon(Icons.Filled.Stop, contentDescription = null)
                            },
                            onClick = {
                                menuOpen = false
                                onStop()
                                onClose()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(0.5f))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(188.dp),
                contentAlignment = Alignment.Center
            ) {
                val station = playback.station
                if (station != null) {
                    StationLogo(station, 188.dp)
                } else {
                    Box(
                        modifier = Modifier
                            .size(188.dp)
                            .background(RadioSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "È",
                            style = MaterialTheme.typography.displayLarge,
                            color = RadioMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                playback.station?.name ?: "Cronos",
                style = MaterialTheme.typography.headlineMedium,
                color = RadioText,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(3.dp))
            Text(
                playback.station?.tagline ?: "Tria una emissora per començar",
                style = MaterialTheme.typography.bodyMedium,
                color = RadioMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (playback.isScheduled) {
                    StatusPill(
                        if (playback.isTest) "PROVA" else "ALARMA",
                        if (playback.isTest) RadioBlue else RadioOrange
                    )
                    Spacer(Modifier.width(8.dp))
                }
                if (playback.mode != PlaybackMode.IDLE) {
                    StatusPill(
                        if (playback.isAtLive) {
                            "DIRECTE"
                        } else {
                            "RETARD ${playback.delayLabel.removePrefix("-")}"
                        },
                        if (playback.isAtLive) MaterialTheme.colorScheme.tertiary else RadioYellow
                    )
                }
            }

            Spacer(Modifier.weight(0.5f))

            if (playback.errorMessage != null && !playback.isLocalAlarm) {
                Text(
                    playback.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
            }

            if (playback.hasTimeshift) {
                val window = playback.window
                val ratio = window.progress(playback.positionUs)
                var dragRatio by remember { mutableStateOf<Float?>(null) }
                var pendingRatio by remember { mutableStateOf<Float?>(null) }
                val displayRatio = dragRatio ?: pendingRatio ?: ratio
                val displayPositionUs = window.startUs +
                    ((window.liveEdgeUs - window.startUs) * displayRatio).toLong()
                val displayLive = window.liveEdgeUs - displayPositionUs <= 2_000_000L

                // Quan l'usuari deixa anar el polze, el servei triga un moment a
                // publicar la nova posició. Mantenim el polze al lloc triat fins
                // que arriba, perquè no faci un salt enrere.
                LaunchedEffect(ratio) {
                    val target = pendingRatio ?: return@LaunchedEffect
                    if (dragRatio == null && abs(ratio - target) <= 0.01f) {
                        pendingRatio = null
                    }
                }
                LaunchedEffect(pendingRatio) {
                    val target = pendingRatio ?: return@LaunchedEffect
                    delay(2_500)
                    if (pendingRatio == target) pendingRatio = null
                }
                val animatedRatio by animateFloatAsState(
                    targetValue = displayRatio,
                    animationSpec = when {
                        dragRatio != null -> snap()
                        pendingRatio != null -> tween(durationMillis = 220)
                        else -> tween(durationMillis = 900, easing = LinearEasing)
                    },
                    label = "seekProgress"
                )

                // Espai reservat sempre: la barra no es mou quan apareix el botó.
                GoLiveSlot(visible = !playback.isAtLive, onClick = onGoLive)

                CronosSeekBar(
                    value = animatedRatio,
                    onValueChange = { dragRatio = it },
                    onValueChangeFinished = {
                        dragRatio?.let { target ->
                            val span = window.liveEdgeUs - window.startUs
                            pendingRatio = target
                            onSeekToUs(window.startUs + (span * target).toLong())
                        }
                        dragRatio = null
                    },
                    isLive = displayLive,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth()) {
                    // Hora real de l'àudio que s'està escoltant.
                    MiniLabel(
                        formatClock(
                            System.currentTimeMillis() -
                                window.delayUs(displayPositionUs) / 1_000L
                        ),
                        tabular = true
                    )
                    Spacer(Modifier.weight(1f))
                    MiniLabel(
                        if (displayLive) {
                            "Directe"
                        } else {
                            "Retard ${window.delayLabel(displayPositionUs).removePrefix("-")}"
                        },
                        tabular = true
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (playback.isLocalAlarm) {
                Button(
                    onClick = onStopLocalAlarm,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RadioRed)
                ) {
                    Icon(Icons.Filled.Alarm, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Atura el to d'alarma", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkipButton(
                        seconds = settings.skipBackSeconds,
                        forward = false,
                        onClick = { onSeekRelative(-settings.skipBackSeconds) }
                    )
                    Spacer(Modifier.width(26.dp))
                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.size(78.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = RadioOrange,
                            contentColor = RadioOnAccent
                        )
                    ) {
                        Icon(
                            if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playback.isPlaying) "Pausa" else "Reprodueix",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.width(26.dp))
                    SkipButton(
                        seconds = settings.skipForwardSeconds,
                        forward = true,
                        onClick = { onSeekRelative(settings.skipForwardSeconds) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            playback.sleepTimerAtMillis?.let { at ->
                Text(
                    "S'atura a les ${formatClock(at)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = RadioYellow,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.weight(0.35f))
        }
    }

    if (sleepDialogOpen) {
        AlertDialog(
            onDismissRequest = { sleepDialogOpen = false },
            title = { Text("Temporitzador") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Atura la reproducció automàticament.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RadioMuted
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 60, 90).forEach { minutes ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    onSetSleepTimer(minutes)
                                    sleepDialogOpen = false
                                },
                                label = { Text("$minutes min") }
                            )
                        }
                    }
                    playback.sleepTimerAtMillis?.let { at ->
                        Text(
                            "Actiu fins a les ${formatClock(at)}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = RadioYellow
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSetSleepTimer(null)
                        sleepDialogOpen = false
                    }
                ) {
                    Text("Desactiva")
                }
            },
            dismissButton = {
                TextButton(onClick = { sleepDialogOpen = false }) { Text("Tanca") }
            }
        )
    }
}

@Composable
private fun GoLiveSlot(visible: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            GoLiveButton(onClick = onClick)
        }
    }
}

/**
 * Enllaç discret i centrat per tornar al directe: sense fons ni contorn, només
 * un punt daurat que respira i el text. Es manté pla per no competir amb la
 * barra de progrés.
 */
@Composable
private fun GoLiveButton(onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "liveDot")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotAlpha"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(RadioYellow.copy(alpha = dotAlpha), CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Torna al directe",
            style = MaterialTheme.typography.labelLarge,
            color = RadioYellow
        )
        // Compensa el punt i el seu espai perquè el text quedi òpticament centrat.
        Spacer(Modifier.width(14.dp))
    }
}

@Composable
private fun SkipButton(
    seconds: Int,
    forward: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
            Icon(
                if (forward) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                contentDescription = if (forward) "Avança $seconds segons" else "Retrocedeix $seconds segons",
                tint = RadioText,
                modifier = Modifier.size(34.dp)
            )
        }
        Text(
            "$seconds s",
            style = MaterialTheme.typography.labelSmall,
            color = RadioMuted
        )
    }
}
