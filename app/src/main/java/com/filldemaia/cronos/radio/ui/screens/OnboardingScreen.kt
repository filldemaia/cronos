package com.filldemaia.cronos.radio.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filldemaia.cronos.radio.ui.RadioViewModel
import com.filldemaia.cronos.radio.ui.components.CronosMark
import com.filldemaia.cronos.radio.ui.components.StatusDot
import com.filldemaia.cronos.radio.ui.theme.RadioGreen
import com.filldemaia.cronos.radio.ui.theme.RadioMuted
import com.filldemaia.cronos.radio.ui.theme.RadioNight
import com.filldemaia.cronos.radio.ui.theme.RadioOrange
import com.filldemaia.cronos.radio.ui.theme.RadioRed
import com.filldemaia.cronos.radio.ui.theme.RadioSurface
import com.filldemaia.cronos.radio.ui.theme.RadioText
import com.filldemaia.cronos.radio.ui.theme.RadioYellow
import com.filldemaia.cronos.radio.util.PermissionManager

@Composable
fun OnboardingScreen(
    viewModel: RadioViewModel,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val oemAcknowledged by viewModel.oemAcknowledged.collectAsStateWithLifecycle()
    val report by viewModel.reliability.collectAsStateWithLifecycle()

    var step by rememberSaveable { mutableIntStateOf(0) }
    var permissionTick by remember { mutableIntStateOf(0) }

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
    val manufacturer = remember { PermissionManager.manufacturer().ifBlank { "Android" } }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionTick++
        viewModel.refreshReliability()
    }

    fun askNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            PermissionManager.openNotificationSettings(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RadioNight)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CronosMark(46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Cronos",
                    style = MaterialTheme.typography.titleLarge,
                    color = RadioText
                )
                Text(
                    "La ràdio, a l'hora exacta",
                    style = MaterialTheme.typography.bodySmall,
                    color = RadioMuted
                )
            }
            TextButton(onClick = onFinish) {
                Text("Salta", color = RadioMuted)
            }
        }

        Spacer(Modifier.height(20.dp))
        StepDots(step)
        Spacer(Modifier.height(18.dp))

        when (step) {
            0 -> WelcomeStep()
            1 -> ExactAlarmStep(
                granted = exactAlarms,
                onGrant = { PermissionManager.openExactAlarmSettings(context) }
            )
            2 -> NotificationsBatteryStep(
                notifications = notifications,
                battery = battery,
                onNotifications = ::askNotifications,
                onBattery = { PermissionManager.openBatteryOptimizationSettings(context) }
            )
            3 -> OemStep(
                manufacturer = manufacturer,
                acknowledged = oemAcknowledged || !PermissionManager.isKnownOem(),
                knownOem = PermissionManager.isKnownOem(),
                onOpenAutoStart = { PermissionManager.openAutoStartSettings(context) },
                onOpenGuide = { PermissionManager.openDontKillMyApp(context) },
                onAcknowledge = { viewModel.acknowledgeOem(true) }
            )
            4 -> FinishStep(
                exactAlarms = exactAlarms,
                notifications = notifications,
                battery = battery,
                oemReady = oemAcknowledged || !PermissionManager.isKnownOem(),
                reportLevel = report?.level,
                onFinish = onFinish
            )
        }

        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 0) {
                TextButton(onClick = { step-- }) { Text("Enrere") }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            if (step < 4) {
                Button(
                    onClick = { step++ },
                    colors = ButtonDefaults.buttonColors(containerColor = RadioOrange)
                ) {
                    Text(if (step == 0) "Comença" else "Continua")
                }
            } else {
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = RadioOrange)
                ) {
                    Text("Activa les alarmes")
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StepDots(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(5) { index ->
            Surface(
                shape = CircleShape,
                color = if (index <= step) RadioOrange else RadioSurface,
                modifier = Modifier
                    .width(if (index == step) 26.dp else 9.dp)
                    .height(9.dp)
            ) {}
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column {
        Text(
            "Primer, protegim l'alarma",
            style = MaterialTheme.typography.headlineMedium,
            color = RadioText
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Cronos pot engegar una emissora a l'hora exacta, amb la pantalla " +
                "bloquejada i sense dependre del despertador del mòbil.",
            style = MaterialTheme.typography.bodyMedium,
            color = RadioMuted
        )
        Spacer(Modifier.height(18.dp))
        InfoBullet("Alarma exacta", "Fem servir AlarmManager.setAlarmClock(), el mateix mecanisme que el rellotge del sistema.")
        InfoBullet("Servei en primer pla", "El receptor arrenca immediatament un servei de reproducció amb wake lock curt.")
        InfoBullet("Cadena de seguretat", "Reintents, URLs alternatives, emissores de reserva i, finalment, un to local sense xarxa.")
        InfoBullet("Time-shift al disc", "Pots pausar i recuperar fins a 12 hores del directe, sense perdre la gravació.")
    }
}

@Composable
private fun ExactAlarmStep(granted: Boolean, onGrant: () -> Unit) {
    Column {
        Text(
            "Permís d'alarmes exactes",
            style = MaterialTheme.typography.headlineMedium,
            color = RadioText
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Android 12 i posteriors poden retardar les alarmes fins a diversos minuts si " +
                "no tens aquest permís. És imprescindible per engegar la ràdio a l'hora.",
            style = MaterialTheme.typography.bodyMedium,
            color = RadioMuted
        )
        Spacer(Modifier.height(16.dp))
        SetupStatusCard(
            title = "Alarmes i recordatoris",
            detail = if (granted) {
                "Concedit. L'alarma no dependrà de Doze."
            } else {
                "Cal activar 'Alarmes i recordatoris' per a Cronos."
            },
            ok = granted,
            actionLabel = if (granted) null else "Concedeix el permís",
            onAction = onGrant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Si el fabricant amaga aquesta opció, busca \"Alarmes i recordatoris\" o " +
                "\"Permisos especials\" a la configuració de l'app.",
            style = MaterialTheme.typography.bodySmall,
            color = RadioMuted
        )
    }
}

@Composable
private fun NotificationsBatteryStep(
    notifications: Boolean,
    battery: Boolean,
    onNotifications: () -> Unit,
    onBattery: () -> Unit
) {
    Column {
        Text(
            "Notificacions i bateria",
            style = MaterialTheme.typography.headlineMedium,
            color = RadioText
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "La notificació és el teu botó d'emergència per aturar l'alarma. " +
                "L'exempció de bateria evita que el sistema mati el servei mentre sona.",
            style = MaterialTheme.typography.bodyMedium,
            color = RadioMuted
        )
        Spacer(Modifier.height(16.dp))
        SetupStatusCard(
            title = "Notificacions",
            detail = if (notifications) "Activades." else "Sense notificacions no podràs aturar l'alarma des de la pantalla bloquejada.",
            ok = notifications,
            actionLabel = if (notifications) null else "Activa",
            onAction = onNotifications
        )
        Spacer(Modifier.height(10.dp))
        SetupStatusCard(
            title = "Optimització de bateria",
            detail = if (battery) "Cronos està exempta." else "Exclou l'app de l'estalvi de bateria.",
            ok = battery,
            actionLabel = if (battery) null else "Exclou",
            onAction = onBattery
        )
    }
}

@Composable
private fun OemStep(
    manufacturer: String,
    acknowledged: Boolean,
    knownOem: Boolean,
    onOpenAutoStart: () -> Unit,
    onOpenGuide: () -> Unit,
    onAcknowledge: () -> Unit
) {
    Column {
        Text(
            "Restriccions de $manufacturer",
            style = MaterialTheme.typography.headlineMedium,
            color = RadioText
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (knownOem) {
                "El teu fabricant pot aturar aplicacions en segon pla. Revisa l'inici automàtic " +
                    "i la llista blanca de bateria. No podem verificar-ho automàticament: confirma-ho tu."
            } else {
                "No hem detectat un fabricant especialment restrictiu, però alguns models " +
                    "poden limitar l'activitat en segon pla. Si tens dubtes, consulta la guia."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = RadioMuted
        )
        Spacer(Modifier.height(16.dp))
        SetupStatusCard(
            title = "Inici automàtic revisat",
            detail = if (acknowledged) {
                "Confirmat. La comprovació de fiabilitat quedarà en verd si la resta és correcta."
            } else {
                "Obre la configuració del fabricant, revisa l'inici automàtic i confirma-ho."
            },
            ok = acknowledged,
            actionLabel = if (acknowledged) null else "Obre l'inici automàtic",
            onAction = onOpenAutoStart
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onOpenGuide) {
                Text("Guia dontkillmyapp.com")
            }
            if (!acknowledged) {
                TextButton(onClick = onAcknowledge) { Text("Ho he revisat") }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Xiaomi/Redmi: inici automàtic + \"Sense restriccions\". Samsung: treu l'app de " +
                "\"Aplicacions en repòs profund\". Huawei/Honor: inici automàtic i activitat en segon pla. " +
                "OnePlus/Oppo/Realme/Vivo: inici automàtic i estalvi de bateria desactivat.",
            style = MaterialTheme.typography.bodySmall,
            color = RadioMuted
        )
    }
}

@Composable
private fun FinishStep(
    exactAlarms: Boolean,
    notifications: Boolean,
    battery: Boolean,
    oemReady: Boolean,
    reportLevel: com.filldemaia.cronos.radio.data.model.ReliabilityLevel?,
    onFinish: () -> Unit
) {
    val allReady = exactAlarms && notifications && battery && oemReady
    Column {
        Text(
            "Tot a punt per programar",
            style = MaterialTheme.typography.headlineMedium,
            color = RadioText
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Cada programació serà una alarma real del sistema. Si alguna cosa no es pot " +
                "garantir, ho veuràs a la comprovació de fiabilitat d'Ajustos.",
            style = MaterialTheme.typography.bodyMedium,
            color = RadioMuted
        )
        Spacer(Modifier.height(16.dp))
        SummaryRow("Alarmes exactes", exactAlarms)
        SummaryRow("Notificacions", notifications)
        SummaryRow("Bateria sense restriccions", battery)
        SummaryRow("Inici automàtic revisat", oemReady)
        Spacer(Modifier.height(14.dp))
        if (!allReady || reportLevel == com.filldemaia.cronos.radio.data.model.ReliabilityLevel.RED) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = RadioYellow.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, RadioYellow.copy(alpha = 0.4f))
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = RadioYellow)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Encara hi ha punts pendents. Les alarmes podrien no engegar-se. " +
                            "Pots continuar i arreglar-ho des d'Ajustos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RadioText
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SetupStatusCard(
    title: String,
    detail: String,
    ok: Boolean,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = RadioSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (ok) RadioGreen.copy(alpha = 0.35f) else RadioYellow.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(if (ok) RadioGreen else RadioYellow, 11.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = RadioText)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = RadioMuted)
            }
            if (actionLabel != null) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onAction) { Text(actionLabel, color = RadioOrange) }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, ok: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(if (ok) RadioGreen else RadioRed, 9.dp)
        Spacer(Modifier.width(9.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = RadioText)
        Spacer(Modifier.weight(1f))
        Text(
            if (ok) "Fet" else "Pendent",
            style = MaterialTheme.typography.labelMedium,
            color = if (ok) RadioGreen else RadioRed
        )
    }
}

@Composable
private fun InfoBullet(title: String, text: String) {
    Row(Modifier.padding(bottom = 13.dp)) {
        Icon(Icons.Filled.CheckCircle, null, tint = RadioGreen, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = RadioText)
            Text(text, style = MaterialTheme.typography.bodySmall, color = RadioMuted)
        }
    }
}
