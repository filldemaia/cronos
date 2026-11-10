package com.filldemaia.cronos.radio.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filldemaia.cronos.radio.data.model.Schedule
import com.filldemaia.cronos.radio.data.model.Station
import com.filldemaia.cronos.radio.domain.schedule.NextAlarmPlanner
import com.filldemaia.cronos.radio.domain.schedule.NextOccurrenceCalculator
import com.filldemaia.cronos.radio.ui.RadioViewModel
import com.filldemaia.cronos.radio.ui.components.RadioCard
import com.filldemaia.cronos.radio.ui.components.ScreenTitle
import com.filldemaia.cronos.radio.ui.components.StationLogo
import com.filldemaia.cronos.radio.ui.components.StatusDot
import com.filldemaia.cronos.radio.ui.components.StatusPill
import com.filldemaia.cronos.radio.ui.components.SurfaceRow
import com.filldemaia.cronos.radio.ui.components.formatDateTime
import com.filldemaia.cronos.radio.ui.components.rememberNow
import com.filldemaia.cronos.radio.ui.components.stopAfterLabel
import com.filldemaia.cronos.radio.ui.theme.RadioBlue
import com.filldemaia.cronos.radio.ui.theme.RadioGreen
import com.filldemaia.cronos.radio.ui.theme.RadioMuted
import com.filldemaia.cronos.radio.ui.theme.RadioOrange
import com.filldemaia.cronos.radio.ui.theme.RadioRed
import com.filldemaia.cronos.radio.ui.theme.RadioText

@Composable
fun SchedulesScreen(viewModel: RadioViewModel) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val now = rememberNow()

    var editorOpen by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<Schedule?>(null) }
    var pendingDelete by remember { mutableStateOf<Schedule?>(null) }
    var pendingDisable by remember { mutableStateOf<Schedule?>(null) }

    val planned = remember(schedules, now) { NextAlarmPlanner.plan(schedules, now) }
    val next = planned.firstOrNull()
    val stationsById = remember(stations) { stations.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                ScreenTitle(
                    kicker = "Programacions",
                    title = "Alarmes",
                    subtitle = "Engegades exactes amb el rellotge del sistema"
                )
            }
            FilledTonalButton(
                onClick = {
                    editingSchedule = null
                    editorOpen = true
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nova")
            }
        }

        if (next != null) {
            val station = stationsById[next.schedule.stationId]
            RadioCard(
                containerColor = RadioOrange.copy(alpha = 0.08f),
                borderColor = RadioOrange.copy(alpha = 0.35f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .padding(0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Alarm, contentDescription = null, tint = RadioOrange)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Propera engegada",
                            style = MaterialTheme.typography.labelMedium,
                            color = RadioMuted
                        )
                        Text(
                            NextOccurrenceCalculator.label(next.nextAt, now),
                            style = MaterialTheme.typography.titleLarge,
                            color = RadioText
                        )
                        Text(
                            "${next.schedule.timeLabel()} · ${station?.name ?: "Emissora no trobada"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RadioMuted
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (schedules.isEmpty()) {
                    "Cap alarma"
                } else if (schedules.size == 1) {
                    "1 alarma"
                } else {
                    "${schedules.size} alarmes"
                },
                style = MaterialTheme.typography.titleMedium,
                color = RadioText,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = viewModel::armAllNow) {
                Text("Rearma totes")
            }
        }

        if (schedules.isEmpty()) {
            RadioCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Alarm,
                        contentDescription = null,
                        tint = RadioOrange,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Encara no tens cap alarma",
                        style = MaterialTheme.typography.titleMedium,
                        color = RadioText
                    )
                    Text(
                        "Crea una engegada exacta. Si el directe falla, l'app reintenta, " +
                            "prova emissores de reserva i, si cal, activa un to local.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RadioMuted
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            editingSchedule = null
                            editorOpen = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RadioOrange)
                    ) {
                        Text("Crea la primera alarma")
                    }
                }
            }
        } else {
            schedules.forEach { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    station = stationsById[schedule.stationId],
                    stationsById = stationsById,
                    nextLabel = NextOccurrenceCalculator.next(schedule, now)?.let {
                        NextOccurrenceCalculator.label(it, now)
                    },
                    onToggle = { enabled ->
                        if (!enabled && !schedule.isOneShot) pendingDisable = schedule
                        else viewModel.setScheduleEnabled(schedule, enabled)
                    },
                    onEdit = {
                        editingSchedule = schedule
                        editorOpen = true
                    }
                )
            }
        }

        Text(
            "Les alarmes es rearmen després de reiniciar el mòbil, actualitzar l'app o " +
                "canviar l'hora o la zona horària. Les alarmes i els seus avisos només " +
                "es mostren en aquesta pantalla.",
            style = MaterialTheme.typography.bodySmall,
            color = RadioMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    if (editorOpen) {
        ScheduleEditorSheet(
            initial = editingSchedule,
            stations = stations,
            defaultFadeSeconds = settings.defaultFadeInSeconds,
            onTest = { editingSchedule?.let(viewModel::testNow) },
            onDelete = { pendingDelete = editingSchedule },
            onDismiss = { editorOpen = false },
            onSave = { schedule ->
                viewModel.saveSchedule(schedule)
                editorOpen = false
            }
        )
    }

    pendingDelete?.let { schedule ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar l'alarma?") },
            text = {
                Text(
                    "S'eliminarà l'alarma de les ${schedule.timeLabel()} " +
                        "(${schedule.daysLabel().lowercase()}) i es cancel·larà al sistema."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSchedule(schedule)
                        pendingDelete = null
                        editorOpen = false
                    }
                ) {
                    Text("Elimina", color = RadioRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel·la") }
            }
        )
    }
    pendingDisable?.let { schedule ->
        AlertDialog(
            onDismissRequest = { pendingDisable = null },
            title = { Text("Desactivar l'alarma") },
            text = { Text("Vols ometre només demà o desactivar-la fins que la tornis a activar?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.skipTomorrow(schedule)
                    pendingDisable = null
                }) { Text("Només demà") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setScheduleEnabled(schedule, false)
                    pendingDisable = null
                }) { Text("Desactiva") }
            }
        )
    }
}

@Composable
private fun ScheduleCard(
    schedule: Schedule,
    station: Station?,
    stationsById: Map<String, Station>,
    nextLabel: String?,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
    var detailsExpanded by remember { mutableStateOf(false) }
    RadioCard(
        borderColor = if (schedule.enabled) {
            MaterialTheme.colorScheme.outlineVariant
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        },
        modifier = Modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onEdit() }
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        schedule.timeLabel(),
                        style = MaterialTheme.typography.displaySmall,
                        color = if (schedule.enabled) RadioText else RadioMuted
                    )
                    Spacer(Modifier.width(10.dp))
                    if (schedule.isOneShot) {
                        StatusPill("UN SOL COP", RadioBlue)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    val dayLabels = mapOf(
                        1 to "Dl", 2 to "Dt", 3 to "Dc", 4 to "Dj", 5 to "Dv", 6 to "Ds", 7 to "Dg"
                    )
                    val orderedDays = if (schedule.isOneShot) emptyList() else listOf(1, 2, 3, 4, 5, 6, 7)
                    orderedDays.forEach { day ->
                        val active = schedule.repeatDays.contains(day)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (active) RadioOrange.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (active) RadioOrange.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Text(
                                dayLabels[day] ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) RadioOrange else RadioMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
            Switch(
                checked = schedule.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = RadioOrange,
                    checkedTrackColor = RadioOrange.copy(alpha = 0.45f)
                )
            )
        }

        if (schedule.isOneShot) {
            schedule.oneShotAtMillis?.let { at ->
                Spacer(Modifier.height(4.dp))
                Text(
                    formatDateTime(at),
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioBlue
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (station != null) {
                StationLogo(station, 36.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = RadioRed)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    station?.name ?: "Emissora no trobada",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (station != null) RadioText else RadioRed
                )
                Text(
                    if (schedule.enabled) {
                        nextLabel?.let { "Propera: $it" } ?: "Sense pròxima execució"
                    } else {
                        "Desactivada"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (nextLabel != null && schedule.enabled) RadioGreen else RadioMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (schedule.enabled && nextLabel == null) {
                StatusDot(RadioRed, 9.dp)
            }
            IconButton(onClick = { detailsExpanded = !detailsExpanded }) {
                Icon(
                    if (detailsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (detailsExpanded) "Amaga els detalls" else "Mostra els detalls"
                )
            }
        }

        if (detailsExpanded) {
            Spacer(Modifier.height(4.dp))
            SurfaceRow("Volum", "${schedule.volumePercent}%")
            SurfaceRow(
                "Fade-in",
                if (schedule.fadeInSeconds <= 0) "Desactivat" else "${schedule.fadeInSeconds} s"
            )
            SurfaceRow("Aturada automàtica", stopAfterLabel(schedule.stopAfterMinutes))
            if (schedule.fallbackStationIds.isNotEmpty()) {
                val names = schedule.fallbackStationIds
                    .mapNotNull { stationsById[it]?.name }
                    .joinToString(", ")
                SurfaceRow("Reserves", names.ifBlank { "Emissores eliminades" })
            }

            Spacer(Modifier.height(8.dp))
        }

    }
}
