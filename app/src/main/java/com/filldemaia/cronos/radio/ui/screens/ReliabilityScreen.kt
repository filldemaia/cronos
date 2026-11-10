package com.filldemaia.cronos.radio.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filldemaia.cronos.radio.data.model.AlarmLog
import com.filldemaia.cronos.radio.data.model.CheckLevel
import com.filldemaia.cronos.radio.data.model.ReliabilityAction
import com.filldemaia.cronos.radio.data.model.ReliabilityCheck
import com.filldemaia.cronos.radio.data.model.ReliabilityLevel
import com.filldemaia.cronos.radio.ui.RadioViewModel
import com.filldemaia.cronos.radio.ui.components.OemGuidanceDialog
import com.filldemaia.cronos.radio.ui.components.OutcomeBadge
import com.filldemaia.cronos.radio.ui.components.RadioCard
import com.filldemaia.cronos.radio.ui.components.SectionHeader
import com.filldemaia.cronos.radio.ui.components.StatusPill
import com.filldemaia.cronos.radio.ui.components.SurfaceRow
import com.filldemaia.cronos.radio.ui.components.formatClock
import com.filldemaia.cronos.radio.ui.components.formatDateTime
import com.filldemaia.cronos.radio.ui.components.reliabilityColor
import com.filldemaia.cronos.radio.ui.theme.RadioBlue
import com.filldemaia.cronos.radio.ui.theme.RadioGreen
import com.filldemaia.cronos.radio.ui.theme.RadioMuted
import com.filldemaia.cronos.radio.ui.theme.RadioOrange
import com.filldemaia.cronos.radio.ui.theme.RadioRed
import com.filldemaia.cronos.radio.ui.theme.RadioText
import com.filldemaia.cronos.radio.ui.theme.RadioYellow
import com.filldemaia.cronos.radio.util.PermissionManager
import kotlin.math.abs

@Composable
fun ReliabilityScreen(
    viewModel: RadioViewModel,
    onOpenSchedules: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val report by viewModel.reliability.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val oemAcknowledged by viewModel.oemAcknowledged.collectAsStateWithLifecycle()
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()

    var showOemDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshReliability()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val handleAction: (ReliabilityAction) -> Unit = { action ->
        when (action) {
            ReliabilityAction.EXACT_ALARM -> PermissionManager.openExactAlarmSettings(context)
            ReliabilityAction.NOTIFICATIONS -> PermissionManager.openNotificationSettings(context)
            ReliabilityAction.BATTERY -> PermissionManager.openBatteryOptimizationSettings(context)
            ReliabilityAction.AUTO_START -> showOemDialog = true
            ReliabilityAction.OPEN_APP_SETTINGS -> PermissionManager.openNotificationSettings(context)
            ReliabilityAction.SOUND_SETTINGS -> PermissionManager.openSoundSettings(context)
            ReliabilityAction.DND_SETTINGS -> PermissionManager.openDndSettings(context)
            ReliabilityAction.ARM_ALARMS -> viewModel.armAllNow()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Fiabilitat",
                    style = MaterialTheme.typography.displaySmall,
                    color = RadioText
                )
                Text(
                    "Podem garantir l'engegada?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RadioMuted
                )
            }
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Tanca la fiabilitat",
                    tint = RadioMuted
                )
            }
        }

        if (report == null) {
            RadioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = RadioOrange,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Comprovant permisos i alarmes…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RadioMuted
                    )
                }
            }
        } else {
            val current = report
            if (current != null) {
                ReliabilitySummaryCard(
                    level = current.level,
                    summary = current.summary,
                    activeCount = current.activeScheduleCount,
                    generatedAt = current.generatedAtMillis,
                    onRefresh = viewModel::refreshReliability
                )
                if (current.activeScheduleCount == 0 && schedules.isEmpty()) {
                    RadioCard(
                        containerColor = RadioBlue.copy(alpha = 0.08f),
                        borderColor = RadioBlue.copy(alpha = 0.35f)
                    ) {
                        Text(
                            "Encara no hi ha cap alarma activa. La fiabilitat es comprova " +
                                "igualment perquè els permisos actuals també afectaran les " +
                                "properes programacions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = RadioText
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onOpenSchedules) {
                            Text("Crea una alarma", color = RadioBlue)
                        }
                    }
                }

                SectionHeader(
                    title = "Comprovacions",
                    subtitle = "Permisos, alarmes armades i connexió"
                )
                current.checks
                    .sortedBy { severityOrder(it.level) }
                    .forEach { check ->
                        CheckCard(check = check, onAction = handleAction)
                    }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Historial recent",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = RadioText
                )
                Text(
                    if (logs.isEmpty()) "Cap execució registrada" else "${logs.size} registres",
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioMuted
                )
            }
            if (logs.isNotEmpty()) {
                TextButton(onClick = { showClearDialog = true }) {
                    Text("Esborra", color = RadioMuted)
                }
            }
        }

        if (logs.isEmpty()) {
            RadioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Alarm, contentDescription = null, tint = RadioMuted)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Quan una alarma soni, aquí veuràs l'hora prevista, l'hora real i " +
                            "si va caldre una emissora de reserva o el to local.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RadioMuted
                    )
                }
            }
        } else {
            logs.take(25).forEach { log ->
                LogCard(log)
            }
        }

        Text(
            "La comparació entre l'hora prevista i la real és la prova més directa que " +
                "l'alarma ha passat pel camí exacte del sistema. Una desviació petita és " +
                "normal; si supera uns quants segons, revisa bateria i permisos.",
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

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Esborrar l'historial?") },
            text = {
                Text(
                    "S'eliminaran tots els registres d'execucions. Les alarmes i els permisos " +
                        "no es modificaran."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearLogs()
                        showClearDialog = false
                    }
                ) {
                    Text("Esborra", color = RadioRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel·la") }
            }
        )
    }
}

@Composable
private fun ReliabilitySummaryCard(
    level: ReliabilityLevel,
    summary: String,
    activeCount: Int,
    generatedAt: Long,
    onRefresh: () -> Unit
) {
    val color = reliabilityColor(level)
    val label = when (level) {
        ReliabilityLevel.GREEN -> "FIABLE"
        ReliabilityLevel.YELLOW -> "A REVISAR"
        ReliabilityLevel.RED -> "NO GARANTIDA"
    }
    RadioCard(
        containerColor = color.copy(alpha = 0.10f),
        borderColor = color.copy(alpha = 0.45f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (level == ReliabilityLevel.GREEN) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleLarge, color = color)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = RadioText)
            }
        }
        Spacer(Modifier.height(10.dp))
        SurfaceRow("Alarmes actives", activeCount.toString())
        SurfaceRow("Darrera comprovació", formatClock(generatedAt))
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = color)
        ) {
            Text("Torna a comprovar", color = Color(0xFF160B00), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CheckCard(
    check: ReliabilityCheck,
    onAction: (ReliabilityAction) -> Unit
) {
    val color = checkColor(check.level)
    RadioCard(
        containerColor = if (check.level == CheckLevel.OK) {
            MaterialTheme.colorScheme.surface
        } else {
            color.copy(alpha = 0.07f)
        },
        borderColor = if (check.level == CheckLevel.OK) {
            MaterialTheme.colorScheme.outlineVariant
        } else {
            color.copy(alpha = 0.35f)
        }
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    check.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = RadioText
                )
                Text(
                    check.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioMuted
                )
                val action = check.action
                if (check.actionLabel != null && action != null) {
                    TextButton(onClick = { onAction(action) }) {
                        Text(check.actionLabel, color = color)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogCard(log: AlarmLog) {
    val deviation = log.actualAtMillis - log.plannedAtMillis
    val deviationColor = when {
        abs(deviation) <= 3_000 -> RadioGreen
        abs(deviation) <= 30_000 -> RadioYellow
        else -> RadioRed
    }
    RadioCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutcomeBadge(log.outcome)
            Spacer(Modifier.width(8.dp))
            Text(
                log.scheduleLabel,
                style = MaterialTheme.typography.titleSmall,
                color = RadioText,
                modifier = Modifier.weight(1f)
            )
            if (log.isTest) StatusPill("PROVA", RadioBlue)
        }
        Spacer(Modifier.height(4.dp))
        Text(log.stationName, style = MaterialTheme.typography.bodySmall, color = RadioMuted)
        Spacer(Modifier.height(8.dp))
        SurfaceRow("Previst", formatDateTime(log.plannedAtMillis))
        SurfaceRow("Real", formatDateTime(log.actualAtMillis))
        SurfaceRow("Desviació", deviationLabel(deviation), deviationColor)
        Spacer(Modifier.height(6.dp))
        Text(
            log.message,
            style = MaterialTheme.typography.bodySmall,
            color = RadioMuted
        )
    }
}

private fun deviationLabel(millis: Long): String {
    val sign = if (millis >= 0) "+" else "−"
    val absMillis = abs(millis)
    return if (absMillis < 1_000) {
        "$sign$absMillis ms"
    } else {
        "$sign%.1f s".format(absMillis / 1000.0)
    }
}

private fun checkColor(level: CheckLevel): Color = when (level) {
    CheckLevel.OK -> RadioGreen
    CheckLevel.WARNING -> RadioYellow
    CheckLevel.ERROR -> RadioRed
    CheckLevel.INFO -> RadioBlue
}

private fun severityOrder(level: CheckLevel): Int = when (level) {
    CheckLevel.ERROR -> 0
    CheckLevel.WARNING -> 1
    CheckLevel.INFO -> 2
    CheckLevel.OK -> 3
}
