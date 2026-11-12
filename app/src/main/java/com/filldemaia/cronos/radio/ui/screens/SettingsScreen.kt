package com.filldemaia.cronos.radio.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import com.filldemaia.cronos.Cities
import com.filldemaia.cronos.SettingsRepository
import com.filldemaia.cronos.radio.data.model.AppSettings
import com.filldemaia.cronos.radio.data.model.Station
import com.filldemaia.cronos.radio.ui.RadioViewModel
import com.filldemaia.cronos.radio.ui.components.OemGuidanceDialog
import com.filldemaia.cronos.radio.ui.components.RadioCard
import com.filldemaia.cronos.radio.ui.components.ScreenTitle
import com.filldemaia.cronos.radio.ui.components.SectionHeader
import com.filldemaia.cronos.radio.ui.components.StationLogo
import com.filldemaia.cronos.radio.ui.components.StatusDot
import com.filldemaia.cronos.radio.ui.components.SurfaceRow
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
    var showDigital by remember { mutableStateOf(clockSettings.showDigital) }
    var showSeconds by remember { mutableStateOf(clockSettings.showSeconds) }
    var showSecondsWritten by remember { mutableStateOf(clockSettings.showSecondsWritten) }
    var hourSize by remember { mutableFloatStateOf(clockSettings.hourSize) }
    var selectedCity by remember { mutableStateOf(clockSettings.locationCity) }
    var customLat by remember { mutableStateOf(clockSettings.locationLatitude.toString()) }
    var customLon by remember { mutableStateOf(clockSettings.locationLongitude.toString()) }
    var locationMenuOpen by remember { mutableStateOf(false) }
    val isCustomLocation = Cities.all.none { it.name == selectedCity }

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
            SectionHeader(
                title = "Buffer de time-shift",
                subtitle = "Quant directe es conserva al disc"
            )
            Spacer(Modifier.height(10.dp))
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
            Spacer(Modifier.height(6.dp))
            Text(
                "S'aplica la propera vegada que iniciïs el directe. El fitxer circular " +
                    "s'ajusta a la durada i al bitrate de l'emissora.",
                style = MaterialTheme.typography.bodySmall,
                color = RadioMuted
            )
        }

        RadioCard {
            SectionHeader(
                title = "Controls del directe",
                subtitle = "Botons de salt i entrada de so"
            )
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
            Spacer(Modifier.height(6.dp))
            Text(
                "Els botons del reproductor i de la notificació fan servir aquests valors.",
                style = MaterialTheme.typography.bodySmall,
                color = RadioMuted
            )
            Spacer(Modifier.height(10.dp))
            Text("Fade-in per defecte", style = MaterialTheme.typography.titleSmall, color = RadioText)
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
                    Text(
                        "Mostra un avís quan comences a escoltar amb dades mòbils.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RadioMuted
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
            SectionHeader(
                title = "Rellotge",
                subtitle = "Hora tradicional catalana i widgets"
            )
            Spacer(Modifier.height(10.dp))
            ClockSwitchRow(
                title = "Mostra l'hora digital",
                description = "Sota l'hora tradicional (desactivada per defecte)",
                checked = showDigital,
                onCheckedChange = { checked ->
                    showDigital = checked
                    clockSettings.showDigital = checked
                }
            )
            ClockSwitchRow(
                title = "Segons a l'hora digital",
                description = "Inclou els segons (HH:mm:ss)",
                checked = showSeconds,
                enabled = showDigital,
                onCheckedChange = { checked ->
                    showSeconds = checked
                    clockSettings.showSeconds = checked
                }
            )
            ClockSwitchRow(
                title = "Segons per escrit",
                description = "Afegeix els segons per escrit a la frase de l'hora tradicional",
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
            Slider(
                value = hourSize,
                onValueChange = { hourSize = it },
                // La preferència només es desa en acabar l'arrossegada,
                // no a cada fotograma.
                onValueChangeFinished = { clockSettings.hourSize = hourSize },
                valueRange = 18f..48f
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Ubicació",
                style = MaterialTheme.typography.titleSmall,
                color = RadioText
            )
            Spacer(Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { locationMenuOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (isCustomLocation) "Personalitzada" else selectedCity,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = locationMenuOpen,
                    onDismissRequest = { locationMenuOpen = false }
                ) {
                    Cities.all.forEach { city ->
                        DropdownMenuItem(
                            text = { Text(city.name) },
                            onClick = {
                                clockSettings.setLocation(city)
                                selectedCity = city.name
                                customLat = city.latitude.toString()
                                customLon = city.longitude.toString()
                                locationMenuOpen = false
                            }
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DropdownMenuItem(
                        text = { Text("Personalitzada") },
                        onClick = {
                            clockSettings.locationCity = ""
                            selectedCity = ""
                            locationMenuOpen = false
                        }
                    )
                }
            }
            Text(
                "Es fa servir per calcular la sortida i la posta de sol i els colors del widget.",
                style = MaterialTheme.typography.bodySmall,
                color = RadioMuted,
                modifier = Modifier.padding(top = 6.dp)
            )
            if (isCustomLocation) {
                Text(
                    "Coordenades en graus decimals (ex. 41,3874 / 2,1686)",
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioMuted,
                    modifier = Modifier.padding(top = 10.dp)
                )
                OutlinedTextField(
                    value = customLat,
                    onValueChange = {
                        customLat = it
                        it.replace(',', '.').toDoubleOrNull()?.let { lat ->
                            clockSettings.locationLatitude = lat
                        }
                    },
                    label = { Text("Latitud") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = customLon,
                    onValueChange = {
                        customLon = it
                        it.replace(',', '.').toDoubleOrNull()?.let { lon ->
                            clockSettings.locationLongitude = lon
                        }
                    },
                    label = { Text("Longitud") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    clockSettings.reset()
                    showDigital = clockSettings.showDigital
                    showSeconds = clockSettings.showSeconds
                    showSecondsWritten = clockSettings.showSecondsWritten
                    hourSize = clockSettings.hourSize
                    selectedCity = clockSettings.locationCity
                    customLat = clockSettings.locationLatitude.toString()
                    customLon = clockSettings.locationLongitude.toString()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restableix el rellotge")
            }
        }

        RadioCard {
            SectionHeader(
                title = "Permisos i protecció",
                subtitle = "Sense aquests permisos una alarma pot no sonar"
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
                Text("Obre la comprovació de fiabilitat")
            }
        }

        RadioCard {
            SectionHeader(
                title = "Emissores personalitzades",
                subtitle = "Enganxa l'URL directe d'un stream MP3, AAC o HLS"
            )
            Spacer(Modifier.height(10.dp))
            if (customStations.isEmpty()) {
                Text(
                    "Encara no has afegit cap emissora personalitzada. Les emissores " +
                        "oficials ja venen incloses amb URLs verificades.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioMuted
                )
            } else {
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
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { showAddStation = true },
                colors = ButtonDefaults.buttonColors(containerColor = RadioOrange)
            ) {
                Text("Afegeix una emissora")
            }
        }

        RadioCard {
            SectionHeader(
                title = "Sobre Cronos",
                subtitle = "Versió ${BuildConfig.VERSION_NAME}"
            )
            Spacer(Modifier.height(8.dp))
            SurfaceRow("Paquet", BuildConfig.APPLICATION_ID)
            SurfaceRow("Emmagatzematge", "DataStore local")
            Spacer(Modifier.height(8.dp))
            Text(
                "Aquesta aplicació ajuda a preservar la llengua, indicant l'hora tradicional catalana.",
                style = MaterialTheme.typography.bodyMedium,
                color = RadioText
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Les programacions, els ajustos i l'historial es guarden només al " +
                    "dispositiu. L'app no envia estadístiques ni fa servir cap compte.",
                style = MaterialTheme.typography.bodySmall,
                color = RadioMuted
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Les alarmes es programen amb AlarmManager.setAlarmClock() i un " +
                    "BroadcastReceiver que arrenca un servei en primer pla amb un wake lock curt.",
                style = MaterialTheme.typography.bodySmall,
                color = RadioMuted
            )
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

        Text(
            "Consell: després d'actualitzar l'app o de reiniciar el mòbil, obre la " +
                "comprovació de fiabilitat i verifica que les alarmes continuen armades.",
            style = MaterialTheme.typography.bodySmall,
            color = RadioMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )
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
                    "S'eliminarà \"${station.name}\" de les teves emissores. Si alguna " +
                        "programació la feia servir, hauràs de triar-ne una altra."
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
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
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
                color = if (enabled) RadioText else RadioMuted
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = RadioMuted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
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
                    "Enganxa l'URL directe del stream. Ha de començar per http:// o https://.",
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
