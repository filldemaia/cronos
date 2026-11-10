@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.filldemaia.cronos.radio.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filldemaia.cronos.radio.data.model.AppSettings
import com.filldemaia.cronos.radio.data.model.Schedule
import com.filldemaia.cronos.radio.data.model.Station
import com.filldemaia.cronos.radio.ui.components.MiniLabel
import com.filldemaia.cronos.radio.ui.theme.RadioMuted
import com.filldemaia.cronos.radio.ui.theme.RadioOrange
import com.filldemaia.cronos.radio.ui.theme.RadioRed
import com.filldemaia.cronos.radio.ui.theme.RadioSurface
import com.filldemaia.cronos.radio.ui.theme.RadioText
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

private val MONTH_NAMES_CA = listOf(
    "gener", "febrer", "març", "abril", "maig", "juny",
    "juliol", "agost", "setembre", "octubre", "novembre", "desembre"
)

private val DAY_NAMES_CA = listOf(
    1 to "Dl", 2 to "Dt", 3 to "Dc", 4 to "Dj", 5 to "Dv", 6 to "Ds", 7 to "Dg"
)

@Composable
fun ScheduleEditorSheet(
    initial: Schedule?,
    stations: List<Station>,
    defaultFadeSeconds: Int,
    onDismiss: () -> Unit,
    onTest: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Schedule) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var hour by remember(initial?.id) { mutableStateOf(initial?.hour ?: 7) }
    var minute by remember(initial?.id) { mutableStateOf(initial?.minute ?: 0) }
    var oneShot by remember(initial?.id) { mutableStateOf(initial?.isOneShot ?: false) }
    var repeatDays by remember(initial?.id) {
        mutableStateOf(initial?.repeatDays ?: listOf(1, 2, 3, 4, 5))
    }
    var selectedDate by remember(initial?.id) {
        mutableStateOf(
            initial?.oneShotAtMillis
                ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                ?: LocalDate.now().plusDays(1)
        )
    }
    var stationId by remember(initial?.id) {
        mutableStateOf(initial?.stationId ?: stations.firstOrNull()?.id.orEmpty())
    }
    var volumePercent by remember(initial?.id) {
        mutableStateOf(initial?.volumePercent ?: 80)
    }
    var fadeSeconds by remember(initial?.id) {
        mutableStateOf(initial?.fadeInSeconds ?: defaultFadeSeconds)
    }
    var stopAfterMinutes by remember(initial?.id) {
        mutableStateOf(initial?.stopAfterMinutes)
    }
    var fallbackIds by remember(initial?.id) {
        mutableStateOf(initial?.fallbackStationIds ?: emptyList())
    }
    var enabled by remember(initial?.id) { mutableStateOf(initial?.enabled ?: true) }
    var stationMenuOpen by remember { mutableStateOf(false) }

    if (stations.isNotEmpty() && stations.none { it.id == stationId }) {
        LaunchedEffect(stations, stationId) {
            if (stations.isNotEmpty() && stations.none { it.id == stationId }) {
                stationId = stations.first().id
            }
        }
    }
    val selectedStation = stations.firstOrNull { it.id == stationId }

    val oneShotInstant = selectedDate
        .atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        .atZone(ZoneId.systemDefault())
        .toInstant()
    val validation = when {
        stations.isEmpty() -> "No hi ha cap emissora disponible."
        selectedStation == null -> "Tria una emissora."
        !oneShot && repeatDays.isEmpty() -> "Tria almenys un dia de la setmana."
        oneShot && !oneShotInstant.isAfter(Instant.now()) -> "La data i hora ja han passat."
        else -> null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RadioSurface,
        contentColor = RadioText
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (initial == null) "Nova alarma" else "Edita l'alarma",
                    style = MaterialTheme.typography.headlineSmall,
                    color = RadioText,
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = {
                    if (validation == null) {
                        val base = initial ?: Schedule(
                            hour = hour,
                            minute = minute,
                            stationId = stationId
                        )
                        onSave(
                            base.copy(
                                hour = hour,
                                minute = minute,
                                repeatDays = if (oneShot) emptyList() else repeatDays.sorted(),
                                oneShotAtMillis = if (oneShot) oneShotInstant.toEpochMilli() else null,
                                stationId = stationId,
                                enabled = enabled,
                                volumePercent = volumePercent,
                                fadeInSeconds = fadeSeconds,
                                stopAfterMinutes = stopAfterMinutes,
                                fallbackStationIds = fallbackIds.filter { it != stationId }
                            )
                        )
                    }
                },
                enabled = validation == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RadioOrange)
            ) {
                Text(
                    if (initial == null) "Crea l'alarma" else "Desa els canvis",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                "L'hora es programa amb AlarmManager.setAlarmClock(): el sistema " +
                    "engegarà el receptor exactament a l'hora indicada.",
                style = MaterialTheme.typography.bodySmall,
                color = RadioMuted
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Hora d'engegada", style = MaterialTheme.typography.titleMedium, color = RadioText)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ValueDropdown(
                    label = "HORA",
                    value = hour,
                    values = (0..23).toList(),
                    display = { "%02d".format(it) },
                    onSelect = { hour = it },
                    modifier = Modifier.weight(1f)
                )
                ValueDropdown(
                    label = "MINUT",
                    value = minute,
                    values = (0..59).toList(),
                    display = { "%02d".format(it) },
                    onSelect = { minute = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Repetició", style = MaterialTheme.typography.titleMedium, color = RadioText)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !oneShot,
                    onClick = {
                        oneShot = false
                        if (repeatDays.isEmpty()) repeatDays = listOf(1, 2, 3, 4, 5)
                    },
                    label = { Text("Cada setmana") }
                )
                FilterChip(
                    selected = oneShot,
                    onClick = { oneShot = true },
                    label = { Text("Un sol cop") }
                )
            }

            if (oneShot) {
                Text(
                    "Data de l'única engegada",
                    style = MaterialTheme.typography.titleSmall,
                    color = RadioText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ValueDropdown(
                        label = "DIA",
                        value = selectedDate.dayOfMonth,
                        values = (1..YearMonth.of(selectedDate.year, selectedDate.monthValue).lengthOfMonth()).toList(),
                        onSelect = { day ->
                            selectedDate = selectedDate.withDayOfMonth(
                                day.coerceAtMost(selectedDate.lengthOfMonth())
                            )
                        },
                        modifier = Modifier.weight(0.8f)
                    )
                    ValueDropdown(
                        label = "MES",
                        value = selectedDate.monthValue,
                        values = (1..12).toList(),
                        display = { MONTH_NAMES_CA[it - 1] },
                        onSelect = { month ->
                            val safeDay = selectedDate.dayOfMonth.coerceAtMost(
                                YearMonth.of(selectedDate.year, month).lengthOfMonth()
                            )
                            selectedDate = LocalDate.of(selectedDate.year, month, safeDay)
                        },
                        modifier = Modifier.weight(1.5f)
                    )
                    ValueDropdown(
                        label = "ANY",
                        value = selectedDate.year,
                        values = (LocalDate.now().year..LocalDate.now().year + 5).toList(),
                        onSelect = { year ->
                            val safeDay = selectedDate.dayOfMonth.coerceAtMost(
                                YearMonth.of(year, selectedDate.monthValue).lengthOfMonth()
                            )
                            selectedDate = LocalDate.of(year, selectedDate.monthValue, safeDay)
                        },
                        modifier = Modifier.weight(0.9f)
                    )
                }
                Text(
                    "S'executarà un sol cop: ${selectedDate.dayOfMonth} " +
                        "${MONTH_NAMES_CA[selectedDate.monthValue - 1]} de " +
                        "${selectedDate.year} a les ${"%02d:%02d".format(hour, minute)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioMuted
                )
            } else {
                Text(
                    "Dies de la setmana",
                    style = MaterialTheme.typography.titleSmall,
                    color = RadioText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    DAY_NAMES_CA.forEach { (value, label) ->
                        FilterChip(
                            selected = repeatDays.contains(value),
                            onClick = {
                                repeatDays = if (repeatDays.contains(value)) {
                                    repeatDays - value
                                } else {
                                    repeatDays + value
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { repeatDays = (1..7).toList() }) {
                        Text("Cada dia")
                    }
                    TextButton(onClick = { repeatDays = listOf(1, 2, 3, 4, 5) }) {
                        Text("Feiners")
                    }
                    TextButton(onClick = { repeatDays = listOf(6, 7) }) {
                        Text("Cap de setmana")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Emissora", style = MaterialTheme.typography.titleMedium, color = RadioText)
            Box {
                OutlinedButton(
                    onClick = { stationMenuOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        selectedStation?.name ?: "Tria una emissora",
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = stationMenuOpen,
                    onDismissRequest = { stationMenuOpen = false }
                ) {
                    stations.forEach { station ->
                        DropdownMenuItem(
                            text = { Text(station.name) },
                            onClick = {
                                stationId = station.id
                                fallbackIds = fallbackIds.filterNot { it == station.id }
                                stationMenuOpen = false
                            }
                        )
                    }
                }
            }

            Text(
                "Emissores de reserva (opcional)",
                style = MaterialTheme.typography.titleSmall,
                color = RadioText
            )
            Text(
                "Si la principal falla, l'app les provarà abans d'activar el to local.",
                style = MaterialTheme.typography.bodySmall,
                color = RadioMuted
            )
            val candidates = stations.filter { it.id != stationId }
            if (candidates.isEmpty()) {
                Text(
                    "No hi ha altres emissores per fer servir de reserva.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioMuted
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    candidates.forEach { station ->
                        FilterChip(
                            selected = fallbackIds.contains(station.id),
                            onClick = {
                                fallbackIds = if (fallbackIds.contains(station.id)) {
                                    fallbackIds - station.id
                                } else {
                                    fallbackIds + station.id
                                }
                            },
                            label = {
                                Text(
                                    station.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Volum",
                    style = MaterialTheme.typography.titleSmall,
                    color = RadioText,
                    modifier = Modifier.weight(1f)
                )
                Text("$volumePercent%", style = MaterialTheme.typography.titleSmall, color = RadioOrange)
            }
            Slider(
                value = volumePercent.toFloat(),
                onValueChange = { volumePercent = it.roundToInt().coerceIn(0, 100) },
                valueRange = 0f..100f,
                steps = 19
            )

            Text("Fade-in", style = MaterialTheme.typography.titleSmall, color = RadioText)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppSettings.FADE_OPTIONS_SECONDS.forEach { seconds ->
                    FilterChip(
                        selected = fadeSeconds == seconds,
                        onClick = { fadeSeconds = seconds },
                        label = { Text(if (seconds == 0) "No" else "$seconds s") }
                    )
                }
            }

            Text("Aturada automàtica", style = MaterialTheme.typography.titleSmall, color = RadioText)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf<Int?>(null, 15, 30, 45, 60, 90).forEach { minutes ->
                    FilterChip(
                        selected = stopAfterMinutes == minutes,
                        onClick = { stopAfterMinutes = minutes },
                        label = { Text(if (minutes == null) "No" else "$minutes min") }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Alarma activada", style = MaterialTheme.typography.titleSmall, color = RadioText)
                    Text(
                        "Si la desactives, es cancel·larà al sistema.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RadioMuted
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RadioOrange,
                        checkedTrackColor = RadioOrange.copy(alpha = 0.45f)
                    )
                )
            }

            validation?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioRed
                )
            }

            if (initial != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OutlinedButton(onClick = onTest, modifier = Modifier.fillMaxWidth()) {
                    Text("Prova-ho ara")
                }
                Text(
                    "La prova sona d'aquí a 5 segons i fa servir la configuració desada de l'alarma.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioMuted
                )
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Elimina l'alarma", color = RadioRed)
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel·la", color = RadioMuted)
            }
        }
    }
}

@Composable
private fun ValueDropdown(
    label: String,
    value: Int,
    values: List<Int>,
    modifier: Modifier = Modifier,
    display: (Int) -> String = { it.toString() },
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        MiniLabel(label)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    display(value),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                values.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(display(option)) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
