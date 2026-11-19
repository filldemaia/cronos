package com.filldemaia.cronos.radio.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filldemaia.cronos.BuildConfig
import com.filldemaia.cronos.SettingsRepository
import com.filldemaia.cronos.radio.data.model.AppSettings
import com.filldemaia.cronos.radio.data.model.Station
import com.filldemaia.cronos.radio.ui.RadioViewModel
import com.filldemaia.cronos.radio.ui.components.CronosSlider
import com.filldemaia.cronos.radio.ui.components.OemGuidanceDialog
import com.filldemaia.cronos.radio.ui.components.RadioCard
import com.filldemaia.cronos.radio.ui.components.ScreenTitle
import com.filldemaia.cronos.radio.ui.components.SectionHeader
import com.filldemaia.cronos.radio.ui.components.StationLogo
import com.filldemaia.cronos.radio.ui.components.StatusDot
import com.filldemaia.cronos.radio.ui.components.WidgetSettingsCard
import com.filldemaia.cronos.radio.ui.components.reliabilityColor
import com.filldemaia.cronos.radio.ui.theme.RadioBlue
import com.filldemaia.cronos.radio.ui.theme.RadioGreen
import com.filldemaia.cronos.radio.ui.theme.RadioMuted
import com.filldemaia.cronos.radio.ui.theme.RadioOrange
import com.filldemaia.cronos.radio.ui.theme.RadioRed
import com.filldemaia.cronos.radio.ui.theme.RadioText
import com.filldemaia.cronos.radio.util.PermissionManager
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: RadioViewModel,
    clockSettings: SettingsRepository,
    onOpenReliability: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val oemAcknowledged by viewModel.oemAcknowledged.collectAsStateWithLifecycle()
    val reliability by viewModel.reliability.collectAsStateWithLifecycle()

    var permissionTick by remember { mutableIntStateOf(0) }
    var showOemDialog by remember { mutableStateOf(false) }
    var showAddStation by remember { mutableStateOf(false) }
    var pendingRemoveStation by remember { mutableStateOf<Station?>(null) }
    var showSecondsWritten by remember { mutableStateOf(clockSettings.showSecondsWritten) }
    var hourSize by remember { mutableFloatStateOf(clockSettings.hourSize) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionTick++
                viewModel.refreshReliability()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val exactAlarms = remember(permissionTick) { PermissionManager.exactAlarmGranted(context) }
    val notifications = remember(permissionTick) { PermissionManager.notificationsGranted(context) }
    val battery = remember(permissionTick) { PermissionManager.batteryOptimizationIgnored(context) }
    val oemReady = oemAcknowledged || !PermissionManager.isKnownOem()
    val customStations = stations.filter { it.isCustom }
    val allPermissionsGranted = exactAlarms && notifications && battery && oemReady

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            ScreenTitle(
                kicker = "Configuració",
                title = "Configuració",
                subtitle = "Configuració de l'aplicació"
            )
        }

        RadioCard {
            SectionHeader(title = "Rellotge")
            Spacer(Modifier.height(10.dp))
            ClockSwitchRow(
                title = "Mostra els segons per escrit",
                checked = showSecondsWritten,
                onCheckedChange = { checked ->
                    showSecondsWritten = checked
                    clockSettings.showSecondsWritten = checked
                }
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Mida de l'hora",
                    style = MaterialTheme.typography.titleSmall,
                    color = RadioText,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${hourSize.roundToInt()}",
                    style = MaterialTheme.typography.titleSmall,
                    color = RadioOrange
                )
            }
            CronosSlider(
                value = hourSize,
                onValueChange = { hourSize = it },
                // La preferència només es desa en acabar l'arrossegada,
                // no a cada fotograma.
                onValueChangeFinished = { clockSettings.hourSize = hourSize },
                valueRange = 12f..50f
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    clockSettings.reset()
                    showSecondsWritten = clockSettings.showSecondsWritten
                    hourSize = clockSettings.hourSize
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restableix el rellotge")
            }
        }

        RadioCard {
            SectionHeader(title = "Reproducció")
            Spacer(Modifier.height(10.dp))
            Text("Retrocedir", style = MaterialTheme.typography.titleSmall, color = RadioText)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppSettings.SKIP_OPTIONS_SECONDS.forEach { seconds ->
                    FilterChip(
                        selected = settings.skipBackSeconds == seconds,
                        onClick = { viewModel.updateSettings { it.copy(skipBackSeconds = seconds) } },
                        label = { Text("$seconds s") }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Avançar", style = MaterialTheme.typography.titleSmall, color = RadioText)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppSettings.SKIP_OPTIONS_SECONDS.forEach { seconds ->
                    FilterChip(
                        selected = settings.skipForwardSeconds == seconds,
                        onClick = { viewModel.updateSettings { it.copy(skipForwardSeconds = seconds) } },
                        label = { Text("$seconds s") }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Volum gradual de l'alarma", style = MaterialTheme.typography.titleSmall, color = RadioText)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppSettings.FADE_OPTIONS_SECONDS.forEach { seconds ->
                    FilterChip(
                        selected = settings.defaultFadeInSeconds == seconds,
                        onClick = {
                            viewModel.updateSettings { it.copy(defaultFadeInSeconds = seconds) }
                        },
                        label = { Text(if (seconds == 0) "No" else "$seconds s") }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Avís de dades mòbils",
                        style = MaterialTheme.typography.titleSmall,
                        color = RadioText
                    )
                }
                Switch(
                    checked = settings.warnOnMobileData,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(warnOnMobileData = checked) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RadioOrange,
                        checkedTrackColor = RadioOrange.copy(alpha = 0.45f)
                    )
                )
            }
        }

        RadioCard {
            SectionHeader(title = "Emissores")
            Spacer(Modifier.height(10.dp))
            Text("Temps per rebobinar", style = MaterialTheme.typography.titleSmall, color = RadioText)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppSettings.BUFFER_OPTIONS_MINUTES.forEach { minutes ->
                    FilterChip(
                        selected = settings.bufferDurationMinutes == minutes,
                        onClick = {
                            viewModel.updateSettings { it.copy(bufferDurationMinutes = minutes) }
                        },
                        label = {
                            Text(if (minutes % 60 == 0) "${minutes / 60} h" else "$minutes min")
                        }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            customStations.forEach { station ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StationLogo(station, 38.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                station.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = RadioText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                station.primaryUrl().orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = RadioMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { pendingRemoveStation = station }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Elimina ${station.name}",
                                tint = RadioRed
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { showAddStation = true },
                colors = ButtonDefaults.buttonColors(containerColor = RadioOrange)
            ) {
                Text("Afegeix una emissora")
            }
        }

        WidgetSettingsCard(clockSettings)

        if (!allPermissionsGranted) {
            RadioCard {
                SectionHeader(
                    title = "Permisos",
                    subtitle = "Algunes restriccions poden impedir que soni l'alarma."
                )
                Spacer(Modifier.height(10.dp))
                PermissionRow(
                    title = "Alarmes exactes",
                    ok = exactAlarms,
                    actionLabel = "Obre",
                    onAction = { PermissionManager.openExactAlarmSettings(context) }
                )
                PermissionRow(
                    title = "Notificacions",
                    ok = notifications,
                    actionLabel = "Obre",
                    onAction = { PermissionManager.openNotificationSettings(context) }
                )
                PermissionRow(
                    title = "Bateria sense restriccions",
                    ok = battery,
                    actionLabel = "Obre",
                    onAction = { PermissionManager.openBatteryOptimizationSettings(context) }
                )
                PermissionRow(
                    title = "Inici automàtic revisat",
                    ok = oemReady,
                    actionLabel = if (oemReady) "Guia" else "Revisa",
                    onAction = { showOemDialog = true }
                )
                Spacer(Modifier.height(6.dp))
                reliability?.let { report ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(reliabilityColor(report.level), 9.dp)
                        Spacer(Modifier.width(9.dp))
                        Text(
                            report.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = RadioMuted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedButton(
                    onClick = onOpenReliability,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Comprova les alarmes")
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showOemDialog = true }) {
                        Text("Guia del fabricant")
                    }
                    TextButton(onClick = { PermissionManager.openDontKillMyApp(context) }) {
                        Text("dontkillmyapp.com", color = RadioBlue)
                    }
                }
            }
        }

        RadioCard {
            SectionHeader(
                title = "Versió",
                subtitle = BuildConfig.VERSION_NAME
            )
        }
    }

    if (showOemDialog) {
        OemGuidanceDialog(
            manufacturer = PermissionManager.manufacturer().ifBlank { "Android" },
            acknowledged = oemAcknowledged || !PermissionManager.isKnownOem(),
            onDismiss = { showOemDialog = false },
            onOpenAutoStart = { PermissionManager.openAutoStartSettings(context) },
            onOpenGuide = { PermissionManager.openDontKillMyApp(context) },
            onAcknowledge = { viewModel.acknowledgeOem(true) }
        )
    }

    if (showAddStation) {
        AddStationDialog(
            onDismiss = { showAddStation = false },
            onAdd = { name, url, onResult ->
                viewModel.addCustomStation(name, url) { error ->
                    onResult(error)
                    if (error == null) showAddStation = false
                }
            }
        )
    }

    pendingRemoveStation?.let { station ->
        AlertDialog(
            onDismissRequest = { pendingRemoveStation = null },
            title = { Text("Eliminar l'emissora?") },
            text = {
                Text(
                    "S'eliminarà \"${station.name}\". Si la fas servir en una alarma, tria una altra emissora."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeCustomStation(station)
                        pendingRemoveStation = null
                    }
                ) {
                    Text("Elimina", color = RadioRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveStation = null }) { Text("Cancel·la") }
            }
        )
    }
}

@Composable
private fun ClockSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = RadioText
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = RadioOrange,
                checkedTrackColor = RadioOrange.copy(alpha = 0.45f)
            )
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    ok: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(if (ok) RadioGreen else RadioRed, 9.dp)
        Spacer(Modifier.width(9.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = RadioText,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAction) {
            Text(actionLabel, color = if (ok) RadioMuted else RadioOrange)
        }
    }
}

@Composable
private fun AddStationDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, (String?) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Afegeix una emissora") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Adreça directa de l'àudio (MP3, AAC o HLS).",
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioMuted
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        error = null
                    },
                    label = { Text("URL del directe") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { message ->
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = RadioRed
                    )
                }
                if (url.isNotBlank() && !url.trim().startsWith("http")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = RadioOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Cal un URL http:// o https://",
                            style = MaterialTheme.typography.labelSmall,
                            color = RadioOrange
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(name, url) { result ->
                        if (result == null) {
                            name = ""
                            url = ""
                        } else {
                            error = result
                        }
                    }
                },
                enabled = url.isNotBlank()
            ) {
                Text("Afegeix", color = RadioOrange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel·la") }
        }
    )
}
