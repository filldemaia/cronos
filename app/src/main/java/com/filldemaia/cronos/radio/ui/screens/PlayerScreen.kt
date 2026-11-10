package com.filldemaia.cronos.radio.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filldemaia.cronos.radio.data.model.Station
import com.filldemaia.cronos.radio.ui.RadioViewModel
import com.filldemaia.cronos.radio.ui.components.RadioCard
import com.filldemaia.cronos.radio.ui.components.ScreenTitle
import com.filldemaia.cronos.radio.ui.components.StationLogoBanner
import com.filldemaia.cronos.radio.ui.components.StatusPill
import com.filldemaia.cronos.radio.ui.theme.RadioBlue
import com.filldemaia.cronos.radio.ui.theme.RadioGreen
import com.filldemaia.cronos.radio.ui.theme.RadioMuted
import com.filldemaia.cronos.radio.ui.theme.RadioOrange
import com.filldemaia.cronos.radio.ui.theme.RadioSurfaceHigh
import com.filldemaia.cronos.radio.ui.theme.RadioSurfaceHighest
import com.filldemaia.cronos.radio.ui.theme.RadioText
import com.filldemaia.cronos.radio.ui.theme.RadioYellow

@Composable
fun PlayerScreen(
    viewModel: RadioViewModel,
    onOpenPlayer: () -> Unit
) {
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                ScreenTitle(
                    kicker = "En directe",
                    title = "Ràdio",
                    subtitle = "En directe, sense soroll."
                )
            }
            LayoutToggle(
                grid = settings.stationsGrid,
                onSelect = { grid ->
                    if (grid != settings.stationsGrid) {
                        viewModel.updateSettings { it.copy(stationsGrid = grid) }
                    }
                }
            )
        }

        if (stations.isEmpty()) {
            RadioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Radio, contentDescription = null, tint = RadioMuted)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Carregant el catàleg d'emissores…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RadioMuted
                    )
                }
            }
        } else if (settings.stationsGrid) {
            StationGrid(
                stations = stations,
                currentId = playback.station?.id,
                isPlaying = playback.isPlaying,
                onClick = { station ->
                    if (playback.station?.id == station.id) {
                        onOpenPlayer()
                    } else {
                        viewModel.playStation(station)
                    }
                }
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stations.forEach { station ->
                    StationCard(
                        station = station,
                        isCurrent = playback.station?.id == station.id,
                        isPlaying = playback.station?.id == station.id && playback.isPlaying,
                        onClick = {
                            if (playback.station?.id == station.id) {
                                onOpenPlayer()
                            } else {
                                viewModel.playStation(station)
                            }
                        }
                    )
                }
            }
        }

        Text(
            "El directe es grava al disc fins a ${bufferLabel(settings.bufferDurationMinutes)} " +
                "i continua gravant mentre està en pausa. Obre el reproductor per " +
                "retrocedir, avançar o programar un temporitzador.",
            style = MaterialTheme.typography.bodySmall,
            color = RadioMuted,
            modifier = Modifier.padding(bottom = 10.dp)
        )
    }
}

@Composable
private fun LayoutToggle(grid: Boolean, onSelect: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = RadioSurfaceHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(2.dp)) {
            LayoutToggleOption(
                icon = Icons.AutoMirrored.Filled.ViewList,
                description = "Mostra les emissores en llista",
                selected = !grid,
                onClick = { onSelect(false) }
            )
            LayoutToggleOption(
                icon = Icons.Filled.GridView,
                description = "Mostra les emissores en mosaic",
                selected = grid,
                onClick = { onSelect(true) }
            )
        }
    }
}

@Composable
private fun LayoutToggleOption(
    icon: ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) RadioSurfaceHighest else RadioSurfaceHigh
    ) {
        Box(
            modifier = Modifier.size(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = description,
                tint = if (selected) RadioOrange else RadioMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StationCard(
    station: Station,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isCurrent) RadioSurfaceHigh else MaterialTheme.colorScheme.surface,
        border = if (isCurrent) {
            BorderStroke(1.dp, RadioBlue.copy(alpha = 0.45f))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StationLogoBanner(
                station = station,
                height = 50.dp,
                modifier = Modifier.width(82.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    station.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = RadioText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    station.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            when {
                isCurrent && isPlaying -> StatusPill("SONANT", RadioGreen)
                isCurrent -> StatusPill("PAUSA", RadioYellow)
                station.isCustom -> StatusPill("MEVA", RadioBlue)
                else -> Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Escolta ${station.name}",
                    tint = RadioOrange,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun StationGrid(
    stations: List<Station>,
    currentId: String?,
    isPlaying: Boolean,
    onClick: (Station) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stations.chunked(3).forEach { rowStations ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowStations.forEach { station ->
                    Box(Modifier.weight(1f)) {
                        StationGridCard(
                            station = station,
                            isCurrent = station.id == currentId,
                            isPlaying = station.id == currentId && isPlaying,
                            onClick = { onClick(station) }
                        )
                    }
                }
                repeat(3 - rowStations.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StationGridCard(
    station: Station,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val statusColor = when {
        !isCurrent -> if (station.isCustom) RadioBlue else RadioMuted
        isPlaying -> RadioGreen
        else -> RadioYellow
    }
    val statusText = when {
        isCurrent -> if (isPlaying) "SONANT" else "PAUSA"
        station.isCustom -> "MEVA"
        else -> null
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isCurrent) RadioSurfaceHigh else MaterialTheme.colorScheme.surface,
        border = if (isCurrent) {
            BorderStroke(1.dp, RadioBlue.copy(alpha = 0.45f))
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StationLogoBanner(
                station = station,
                height = 54.dp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(7.dp))
            Text(
                station.name,
                style = MaterialTheme.typography.titleSmall,
                color = RadioText,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (statusText != null) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private fun bufferLabel(minutes: Int): String =
    if (minutes % 60 == 0) "${minutes / 60} h" else "$minutes min"
